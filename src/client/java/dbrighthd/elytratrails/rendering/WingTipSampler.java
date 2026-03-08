package dbrighthd.elytratrails.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dbrighthd.elytratrails.ElytraTrailsClient;
import dbrighthd.elytratrails.compat.ModStatuses;
import dbrighthd.elytratrails.compat.emf.EmfAnimationHooks;
import dbrighthd.elytratrails.compat.emf.EmfWingTipHooks;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.mixin.client.ModelFeatureStorageAccessor;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import dbrighthd.elytratrails.util.ModelTransformationUtil;
import dbrighthd.elytratrails.util.ShaderChecksUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;

import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dbrighthd.elytratrails.compat.emf.EmfTrailSpawnerRegistry.getModelVariantFromModel;
import static dbrighthd.elytratrails.util.ModelTransformationUtil.getSignedElytraAoARadiansFast;

public class WingTipSampler {
    private final SubmitNodeStorage submitStorage = new SubmitNodeStorage();
    private final Map<Integer, EmfInfo> emfCache = new HashMap<>();

    private record EmfInfo(String name, int variant, List<SpawnerInfo> spawners){}
    private static final Logger LOGGER = LoggerFactory.getLogger(WingTipSampler.class);

    private record SpawnerInfo(EmfWingTipHooks.SpawnerPath spawner, boolean isLeftWing){}
    public Map<Integer, List<Emitter>> gatheredTrailsThisFrame = new HashMap<>();

    public void clearFrameCache()
    {
        gatheredTrailsThisFrame.clear();
    }
    public @NotNull List<Emitter> getPlayerTrailEmitterPositions(Player player, float partialTick, ModConfig modConfig) {
        ModConfig config = ElytraTrailsClient.getConfig();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ShaderChecksUtil.isShadowPass()) return List.of();

        Camera camera = mc.gameRenderer.getMainCamera();
        CameraRenderState cameraState = buildCameraState(camera);

        SubmitNodeStorage.ModelSubmit<?> elytraSubmit = extractElytraRenderState(player, mc, cameraState, partialTick);
        if (elytraSubmit == null || !(elytraSubmit.model() instanceof ElytraModel elytraModel) || !(elytraSubmit.state() instanceof HumanoidRenderState humanoidState)) return List.of();

        elytraModel.setupAnim(humanoidState);

        ModelPart leftWing = elytraModel.leftWing;
        ModelPart rightWing = elytraModel.rightWing;

        PoseStack basePose = new PoseStack();
        basePose.last().set(elytraSubmit.pose());

        Vec3 entityWorldOffset = new Vec3(humanoidState.x, humanoidState.y, humanoidState.z);
        ModelPart animatedElytraRoot = tryGetAnimatedElytraRoot(elytraModel, player);
        int eid = player.getId();
        if (ModStatuses.EMF_LOADED && config.emfSupport) {

            int variant = getModelVariantFromModel(animatedElytraRoot);

            if (!emfCache.containsKey(eid) || !(emfCache.get(eid).variant() == variant))
            {
                if(config.logTrails)
                {
                    LOGGER.info("Entity {}, New elytra equipped with model variant {}",eid, variant);
                }
                emfCache.put(eid,new EmfInfo("elytra", variant, getSpawnersInfo(EmfWingTipHooks.findAllSpawnerPaths(leftWing, rightWing, modConfig.hardCodedFreshAnimationsPlayerWingtips))));
                return List.of();
            }
            EmfInfo emfInfo = emfCache.get(eid);
            if (!emfInfo.spawners.isEmpty()) {
                List<Emitter> gatheredTrails = getTrailEmittersFromBones(basePose, animatedElytraRoot, elytraModel, camera.position(), entityWorldOffset, emfInfo);
                if(config.alwaysSnapTrail)
                {
                    gatheredTrailsThisFrame.put(eid,gatheredTrails);
                }
                return gatheredTrails;
            }
        }
        List<Emitter> gatheredTrails = getVanillaTrailEmitters(basePose, animatedElytraRoot, elytraModel, camera.position(), entityWorldOffset,player,modConfig);
        if(config.alwaysSnapTrail)
        {
            gatheredTrailsThisFrame.put(eid,gatheredTrails);
        }
        return gatheredTrails;
    }


    public @NotNull List<Emitter> getEntityTrailEmitterPositions(Entity entity, float partialTick) {
        ModConfig config = ElytraTrailsClient.getConfig();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ShaderChecksUtil.isShadowPass() || entity instanceof Player) return List.of();

        Camera camera = mc.gameRenderer.getMainCamera();
        CameraRenderState cameraState = buildCameraState(camera);
        SubmitNodeStorage.ModelSubmit<?> entitySubmit = extractEntityRenderState(entity, mc, cameraState, partialTick);
        if (entitySubmit == null || !(entitySubmit.model() instanceof EntityModel<?> entityModel) || !(entitySubmit.state() instanceof EntityRenderState entityRenderState)) return List.of();
        setupAnyModelAnim(entityModel,entityRenderState);
        PoseStack basePose = new PoseStack();
        basePose.last().set(entitySubmit.pose());

        Vec3 entityWorldOffset = new Vec3(entityRenderState.x, entityRenderState.y, entityRenderState.z);
        ModelPart animatedRoot = tryGetAnimatedEntityRoot(entityModel, entity);
        int eid = entity.getId();
        if (ModStatuses.EMF_LOADED && config.emfSupport) {

            int variant = getModelVariantFromModel(animatedRoot);
            if (!emfCache.containsKey(eid) || !(emfCache.get(eid).variant() == variant))
            {
                if(config.logTrails)
                {
                    LOGGER.info("Entity {}, New variant with model variant {}",eid, variant);
                }
                emfCache.put(eid,new EmfInfo(entity.getType().toString(), variant, getSpawnersInfo(EmfWingTipHooks.findAllSpawnerPathsGeneric(animatedRoot, false))));
                return List.of();

            }
            EmfInfo emfInfo = emfCache.get(eid);
            if (!emfInfo.spawners.isEmpty()) {
                List<Emitter> gatheredTrails = getTrailEmittersFromBonesGeneric(basePose, animatedRoot, camera.position(), entityWorldOffset, emfInfo);
                if(config.alwaysSnapTrail)
                {
                    gatheredTrailsThisFrame.put(eid,gatheredTrails);
                }
                return gatheredTrails;
            }
        }
        List<Emitter> gatheredTrails = getVanillaTrailEmittersGeneric(basePose, animatedRoot, entityModel, camera.position(), entityWorldOffset, entity);
        if(config.alwaysSnapTrail)
        {
            gatheredTrailsThisFrame.put(eid,gatheredTrails);
        }
        return gatheredTrails;
    }
    public void removeFromEmfCache(int eid)
    {
        emfCache.remove(eid);
    }
    public void removeAllEmfCache()
    {
        emfCache.clear();
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setupAnimUnchecked(EntityModel<?> model, EntityRenderState state) {
        ((EntityModel) model).setupAnim(state);
    }
    private static void setupAnyModelAnim(Object modelObj, Object stateObj) {
        if (!(modelObj instanceof EntityModel<?> model)) return;
        if (!(stateObj instanceof EntityRenderState state)) return;

        setupAnimUnchecked(model, state);
    }
    private List<SpawnerInfo> getSpawnersInfo(List<EmfWingTipHooks.SpawnerPath> spawners)
    {
        ArrayList<SpawnerInfo> spawnerInfos = new ArrayList<>();
        for(EmfWingTipHooks.SpawnerPath spawner : spawners)
        {
            spawnerInfos.add(new SpawnerInfo(spawner, inferLeftWing(spawner.where(), spawner.path())));
        }
        return spawnerInfos;
    }

    private @NotNull List<Emitter> getTrailEmittersFromBones(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ElytraModel model, @NotNull Vec3 cameraPos, @NotNull Vec3 entityWorldOffset, EmfInfo emfInfo) {
        ModelPart leftWing = model.leftWing;
        ModelPart rightWing = model.rightWing;
        List<SpawnerInfo> spawners = emfInfo.spawners;

        List<Emitter> emitters = new ArrayList<>();
        for (SpawnerInfo spawner : spawners) {
            ModelPart wingRoot = (spawner.spawner.where() == EmfWingTipHooks.WhichRoot.LEFT_WING) ? leftWing : rightWing;

            Vec3 cameraRelative = transformLocalPointThroughPath(
                    stack, elytraRoot, wingRoot, spawner.spawner.path(), getEmitterLocalOffset(spawner)
            );
            if (cameraRelative == null) continue;

            emitters.add(new Emitter(cameraPos.add(cameraRelative).add(entityWorldOffset), spawner.isLeftWing, "elytra" +(emfInfo.variant > 1 ?  emfInfo.variant : ""),spawner.spawner.path()));
        }
        return emitters;
    }
    private @NotNull List<Emitter> getTrailEmittersFromBonesGeneric(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull Vec3 cameraPos, @NotNull Vec3 entityWorldOffset, EmfInfo emfInfo) {
        List<SpawnerInfo> spawners = emfInfo.spawners;

        List<Emitter> emitters = new ArrayList<>();
        for (SpawnerInfo spawner : spawners) {

            Vec3 cameraRelative = transformLocalPointThroughPathGeneric(
                    stack, elytraRoot, spawner.spawner.path()
            );
            if (cameraRelative == null) continue;

            emitters.add(new Emitter(cameraPos.add(cameraRelative).add(entityWorldOffset), spawner.isLeftWing, emfInfo.name.replace("entity.minecraft.","") + (emfInfo.variant > 1 ?  emfInfo.variant : ""),spawner.spawner.path()));
        }
        return emitters;
    }
    private static boolean inferLeftWing(EmfWingTipHooks.WhichRoot modelRoot, String spawnerOrBoneName) {

        if (spawnerOrBoneName != null) {
            String bone = spawnerOrBoneName.substring(spawnerOrBoneName.lastIndexOf('/')).toLowerCase();
            return bone.contains("left");
        }

        return (modelRoot == EmfWingTipHooks.WhichRoot.LEFT_WING);
    }

    private @NotNull List<Emitter> getVanillaTrailEmitters(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ElytraModel model, @NotNull Vec3 cameraPos, @NotNull Vec3 entityWorldOffset, Player player, ModConfig modConfig) {
        ModelPart leftWing = model.leftWing;
        ModelPart rightWing = model.rightWing;

        Vec3 leftTip = computeTransformedWingTip(stack, elytraRoot, leftWing, ModelTransformationUtil.VANILLA_LEFT_WING_TIP,player, modConfig);
        Vec3 rightTip = computeTransformedWingTip(stack, elytraRoot, rightWing, ModelTransformationUtil.VANILLA_RIGHT_WING_TIP,player, modConfig);

        return List.of(
                new Emitter(cameraPos.add(leftTip).add(entityWorldOffset), true, "elytra","/leftWingTip"),
                new Emitter (cameraPos.add(rightTip).add(entityWorldOffset), false, "elytra","/rightWingTip")
        );
    }
    private @NotNull List<Emitter> getVanillaTrailEmittersGeneric(
            @NotNull PoseStack stack,
            @Nullable ModelPart animatedRoot,
            @NotNull EntityModel<?> model,
            @NotNull Vec3 cameraPos,
            @NotNull Vec3 entityWorldOffset,
            Entity entity
    ) {
        Vec3 tip;

        ModelPart modelPart = animatedRoot != null ? animatedRoot : model.root();
        if (entity.getType() == EntityType.ARROW || entity.getType() == EntityType.SPECTRAL_ARROW) {
            tip = computeTransformedPoint(stack, modelPart, modelPart, ModelTransformationUtil.VANILLA_ARROW_WINGTIP);
        } else {
            tip = computeTransformedPoint(stack, modelPart, modelPart, ModelTransformationUtil.ZERO_WINGTIP);
        }

        return List.of(
                new Emitter(cameraPos.add(tip).add(entityWorldOffset), true, entity.getType().toShortString(), "/trailspawner")
        );
    }
    private static final Vec3 FRESH_ANIMATIONS_LEFT_WINGTIP_OFFSET =
            new Vec3(-11.0 / 16.0, 21.0 / 16.0, 3.0 / 16.0);

    private static final Vec3 FRESH_ANIMATIONS_RIGHT_WINGTIP_OFFSET =
            new Vec3(11.0 / 16.0, 21.0 / 16.0, 3.0 / 16.0);
    private static Vec3 getEmitterLocalOffset(SpawnerInfo spawner) {
        String key = spawner.spawner.key();
        if (key == null) return Vec3.ZERO;

        return switch (key.toLowerCase()) {
            case "emf_left_wing2" -> FRESH_ANIMATIONS_LEFT_WINGTIP_OFFSET;
            case "emf_right_wing2" -> FRESH_ANIMATIONS_RIGHT_WINGTIP_OFFSET;
            default -> Vec3.ZERO;
        };
    }

    private @NotNull Vec3 computeTransformedWingTip(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ModelPart wingRoot, @NotNull Vec3 localPos, Player player, ModConfig modConfig) {
        float wingspread = ModelTransformationUtil.computeWingOpenness(wingRoot);
        float xScale = 1.0f;
        float zScale = 0.666666f;
        PlayerConfig config = ClientPlayerConfigStore.getOrDefault(player.getId());
        if(config.trailMovesWithElytraAngle())
        {
            xScale = Mth.lerp(wingspread, 0.33333f, 1.0f);
        }
        if(config.trailMovesWithAngleOfAttack())
        {
            zScale = Mth.lerp(getSignedElytraAoARadiansFast(player),0.33333f,1.0f);
        }
        Vec3 scaledLocalTip = new Vec3(localPos.x * xScale, localPos.y, localPos.z * zScale);
        return transformLocalPoint(stack, elytraRoot, wingRoot, scaledLocalTip);
    }

    private @NotNull Vec3 computeTransformedPoint(@NotNull PoseStack stack, @Nullable ModelPart modelRoot, @NotNull ModelPart partRoot, @NotNull Vec3 localPos ) {
        stack.pushPose();
        if (modelRoot != null && modelRoot != partRoot) {
            modelRoot.translateAndRotate(stack);
        }
        partRoot.translateAndRotate(stack);
        Vec3 point = ModelTransformationUtil.transformPoint(stack.last().pose(), localPos);
        stack.popPose();
        return point;
    }
    private @Nullable Vec3 transformLocalPointThroughPath(
            @NotNull PoseStack stack,
            @Nullable ModelPart elytraRoot,
            @NotNull ModelPart wingRoot,
            @Nullable String childOnlyPath,
            @NotNull Vec3 localOffset
    ) {
        if (childOnlyPath == null || childOnlyPath.isEmpty()) return null;

        stack.pushPose();
        if (elytraRoot != null && elytraRoot != wingRoot) {
            elytraRoot.translateAndRotate(stack);
        }
        wingRoot.translateAndRotate(stack);

        ModelPart current = wingRoot;
        int segmentStartIndex = 0;

        while (segmentStartIndex < childOnlyPath.length()) {
            int nextSlashIndex = childOnlyPath.indexOf('/', segmentStartIndex);
            String segmentName = (nextSlashIndex == -1)
                    ? childOnlyPath.substring(segmentStartIndex)
                    : childOnlyPath.substring(segmentStartIndex, nextSlashIndex);

            ModelPart child = findChildIgnoreCase(current, segmentName);
            if (child == null) {
                stack.popPose();
                return null;
            }

            child.translateAndRotate(stack);
            current = child;

            if (nextSlashIndex == -1) break;
            segmentStartIndex = nextSlashIndex + 1;
        }

        Vec3 point = ModelTransformationUtil.transformPoint(stack.last().pose(), localOffset);
        stack.popPose();
        return point;
    }
    private @NotNull Vec3 transformLocalPoint(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ModelPart wingRoot, @NotNull Vec3 localPos) {
        stack.pushPose();
        if (elytraRoot != null && elytraRoot != wingRoot) {
            elytraRoot.translateAndRotate(stack);
        }
        wingRoot.translateAndRotate(stack);
        Vec3 point = ModelTransformationUtil.transformPoint(stack.last().pose(), localPos);
        stack.popPose();
        return point;
    }

    private @Nullable Vec3 transformLocalPointThroughPath(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ModelPart wingRoot, @Nullable String childOnlyPath) {
        if (childOnlyPath == null || childOnlyPath.isEmpty()) return null;

        stack.pushPose();
        if (elytraRoot != null && elytraRoot != wingRoot) {
            elytraRoot.translateAndRotate(stack);
        }
        wingRoot.translateAndRotate(stack);

        ModelPart current = wingRoot;
        int segmentStartIndex = 0;

        while (segmentStartIndex < childOnlyPath.length()) {
            int nextSlashIndex = childOnlyPath.indexOf('/', segmentStartIndex);
            String segmentName = (nextSlashIndex == -1)
                    ? childOnlyPath.substring(segmentStartIndex)
                    : childOnlyPath.substring(segmentStartIndex, nextSlashIndex);

            ModelPart child = findChildIgnoreCase(current, segmentName);
            if (child == null) return null;

            child.translateAndRotate(stack);
            current = child;

            if (nextSlashIndex == -1) break;
            segmentStartIndex = nextSlashIndex + 1;
        }

        Vec3 point = ModelTransformationUtil.transformPoint(stack.last().pose(), Vec3.ZERO);

        stack.popPose();
        return point;
    }
    private @Nullable Vec3 transformLocalPointThroughPathGeneric(@NotNull PoseStack stack, @Nullable ModelPart modelRoot, @Nullable String childOnlyPath) {
        if (childOnlyPath == null || childOnlyPath.isEmpty()) return null;

        stack.pushPose();
        if (modelRoot != null) {
            modelRoot.translateAndRotate(stack);
        }

        ModelPart current = modelRoot;
        int segmentStartIndex = 0;

        while (segmentStartIndex < childOnlyPath.length()) {
            int nextSlashIndex = childOnlyPath.indexOf('/', segmentStartIndex);
            String segmentName = (nextSlashIndex == -1)
                    ? childOnlyPath.substring(segmentStartIndex)
                    : childOnlyPath.substring(segmentStartIndex, nextSlashIndex);

            if(current==null)
            {
                return null;
            }
            ModelPart child = findChildIgnoreCase(current, segmentName);
            if (child == null) return null;

            child.translateAndRotate(stack);
            current = child;

            if (nextSlashIndex == -1) break;
            segmentStartIndex = nextSlashIndex + 1;
        }

        Vec3 point = ModelTransformationUtil.transformPoint(stack.last().pose(), Vec3.ZERO);

        stack.popPose();
        return point;
    }

    private @Nullable ModelPart findChildIgnoreCase(@NotNull ModelPart parent, @NotNull String name) {
        ModelPart direct = parent.children.get(name);
        if (direct != null) return direct;

        for (var entry : parent.children.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private @Nullable ModelPart tryGetAnimatedElytraRoot(ElytraModel model, Player player) {
        if (!ModStatuses.EMF_LOADED || !ElytraTrailsClient.getConfig().emfSupport) return null;
        try {
            return EmfAnimationHooks.applyManualAnimationAndGetRoot(model, player);
        } catch (Throwable ignored) { return null; }
    }
    private @Nullable ModelPart tryGetAnimatedEntityRoot(EntityModel<?> model, Entity entity) {
        if (!ModStatuses.EMF_LOADED || !ElytraTrailsClient.getConfig().emfSupport) return null;
        try {
            return EmfAnimationHooks.applyManualAnimationAndGetRoot(model, entity);
        } catch (Throwable e) {
            return null; }
    }

    private @NotNull CameraRenderState buildCameraState(@NotNull Camera camera) {
        CameraRenderState state = new CameraRenderState();
        state.pos = camera.position();
        state.entityPos = camera.position();
        state.blockPos = BlockPos.containing(camera.position());
        state.initialized = true;
        state.orientation = new Quaternionf(camera.rotation());
        return state;
    }

    @SuppressWarnings("unchecked")
    private SubmitNodeStorage.ModelSubmit<?> extractElytraRenderState(Player player, Minecraft mc, CameraRenderState cameraRenderState, float partialTick)
    {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        AvatarRenderState state = new AvatarRenderState();

        EntityRenderer<@NotNull Avatar, @NotNull AvatarRenderState> renderer = (EntityRenderer<@NotNull Avatar, @NotNull AvatarRenderState>) dispatcher.getRenderer(state);
        renderer.extractRenderState(player, state, partialTick);

        submitStorage.clear();
        dispatcher.submit(state, cameraRenderState, -cameraRenderState.pos.x, -cameraRenderState.pos.y, -cameraRenderState.pos.z, new PoseStack(), submitStorage);

        return findElytraModelSubmit();
    }
    private SubmitNodeStorage.ModelSubmit<?> extractEntityRenderState(Entity entity, Minecraft mc, CameraRenderState cameraRenderState, float partialTick)
    {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        EntityRenderState state = dispatcher.extractEntity(entity, partialTick);
        submitStorage.clear();
        dispatcher.submit(state, cameraRenderState, -cameraRenderState.pos.x, -cameraRenderState.pos.y, -cameraRenderState.pos.z, new PoseStack(), submitStorage);
        return getModelSubmit();
    }
    private SubmitNodeStorage.@Nullable ModelSubmit<?> findElytraModelSubmit() {
        for (SubmitNodeCollection collection : submitStorage.getSubmitsPerOrder().values()) {
            ModelFeatureRenderer.Storage modelStorage = collection.getModelSubmits();
            ModelFeatureStorageAccessor accessor = (ModelFeatureStorageAccessor) modelStorage;

            Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaqueByType =
                    accessor.elytratrails$getOpaqueModelSubmits();
            for (List<SubmitNodeStorage.ModelSubmit<?>> submits : opaqueByType.values()) {
                for (SubmitNodeStorage.ModelSubmit<?> submit : submits) {
                    if (submit.model() instanceof ElytraModel) return submit;
                }
            }

            List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucentSubmits =
                    accessor.elytratrails$getTranslucentModelSubmits();
            for (SubmitNodeStorage.TranslucentModelSubmit<?> translucent : translucentSubmits) {
                SubmitNodeStorage.ModelSubmit<?> submit = translucent.modelSubmit();
                if (submit.model() instanceof ElytraModel) return submit;
            }
        }
        return null;
    }
    private SubmitNodeStorage.@Nullable ModelSubmit<?> getModelSubmit() {
        for (SubmitNodeCollection collection : submitStorage.getSubmitsPerOrder().values()) {
            ModelFeatureRenderer.Storage modelStorage = collection.getModelSubmits();
            ModelFeatureStorageAccessor accessor = (ModelFeatureStorageAccessor) modelStorage;

            Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaqueByType =
                    accessor.elytratrails$getOpaqueModelSubmits();
            for (List<SubmitNodeStorage.ModelSubmit<?>> submits : opaqueByType.values()) {
                for (SubmitNodeStorage.ModelSubmit<?> submit : submits) {
                    return submit;
                }
            }
            List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucentSubmits =
                    accessor.elytratrails$getTranslucentModelSubmits();
            for (SubmitNodeStorage.TranslucentModelSubmit<?> translucent : translucentSubmits) {
                return translucent.modelSubmit();
            }
        }
        return null;
    }
}
