package dbrighthd.elytratrails.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dbrighthd.elytratrails.api.ElytraTrailsAPI;
import dbrighthd.elytratrails.compat.ModStatuses;
import dbrighthd.elytratrails.compat.cpm.CpmModelStorage;
import dbrighthd.elytratrails.compat.emf.EmfAnimationHooks;
import dbrighthd.elytratrails.compat.emf.EmfWingTipHooks;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.config.pack.ResolvedSampleSettings;
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
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.*;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.compat.ModStatuses.CPM_LOADED;
import static dbrighthd.elytratrails.compat.cpm.CpmModelStorage.findCPMElytraModelSubmit;
import static dbrighthd.elytratrails.compat.emf.EmfTrailSpawnerRegistry.getModelVariantFromModel;
import static dbrighthd.elytratrails.util.ModelTransformationUtil.*;

public class WingTipSampler {
    private final SubmitNodeStorage submitStorage = new SubmitNodeStorage();
    private final Map<Integer, EmfInfo> emfCache = new HashMap<>();
    private final Map<Integer, EmfInfo> emfCacheGeneric = new HashMap<>();
    private final Map<ModelPart, Map<String, ModelPart>> childLookupCache = new IdentityHashMap<>();

    private record EmfInfo(String name, int variant, List<SpawnerInfo> spawners) {
    }

    private record SpawnerInfo(EmfWingTipHooks.SpawnerPath spawner, String[] pathSegments, boolean isLeftWing) {
    }

    private record ResolvedEmitterPoint(Vec3 position, boolean visible) {
    }
    public record EntityEmitters(List<Emitter> emitters, boolean changedModelVariant){}
    public record PlayerEmitters(boolean valid, List<Emitter> emitters){}
    public Map<Integer, List<Emitter>> gatheredTrailsThisFrame = new HashMap<>();

    public void clearFrameCache() {
        gatheredTrailsThisFrame.clear();
        if(CPM_LOADED)
        {
            CpmModelStorage.resetSubmits();
        }
    }

    public @NotNull PlayerEmitters getPlayerTrailEmitterPositions(Avatar player, float partialTick) {
        if(CPM_LOADED)
        {
            CpmModelStorage.resetSubmits();
        }
        ModConfig config = getConfig();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ShaderChecksUtil.isShadowPass()) return new PlayerEmitters(true, List.of());

        Camera camera = mc.gameRenderer.mainCamera();
        CameraRenderState cameraState = buildCameraState(camera);
        ModelFeatureRenderer.Submit<?> elytraSubmit = extractElytraRenderState(player, mc, cameraState, partialTick);
        if(elytraSubmit == null)
        {
            elytraSubmit = extractElytraRenderState(player, mc, cameraState, partialTick);
        }
        if (elytraSubmit == null || !(elytraSubmit.model() instanceof ElytraModel elytraModel) || !(elytraSubmit.state() instanceof HumanoidRenderState humanoidState))
        {
            return new PlayerEmitters(true, List.of());
        }
        if(elytraSubmit.state() instanceof AvatarRenderState avatarRenderState)
        {
            if(avatarRenderState.fallFlyingScale() < 1.0F)
            {
               return new PlayerEmitters(false, List.of());
            }
        }
        elytraModel.setupAnim(humanoidState);

        ModelPart leftWing = elytraModel.leftWing;
        ModelPart rightWing = elytraModel.rightWing;

        PoseStack basePose = new PoseStack();
        basePose.last().set(elytraSubmit.pose());

        Vec3 entityWorldOffset = new Vec3(humanoidState.x, humanoidState.y, humanoidState.z);
        ModelPart animatedElytraRoot = tryGetAnimatedElytraRoot(elytraModel, player, humanoidState);
        int eid = player.getId();
        if (ModStatuses.EMF_LOADED && config.emfSupport) {

            int variant = getModelVariantFromModel(elytraModel);

            if (!emfCache.containsKey(eid) || !(emfCache.get(eid).variant() == variant)) {
                emfCache.put(eid, new EmfInfo("elytra", variant, getSpawnersInfo(EmfWingTipHooks.findAllSpawnerPaths(leftWing, rightWing))));
                return new PlayerEmitters(true, List.of());
            }
            EmfInfo emfInfo = emfCache.get(eid);
            if (!emfInfo.spawners.isEmpty()) {
                List<Emitter> gatheredTrails = getTrailEmittersFromBones(basePose, animatedElytraRoot, elytraModel, entityWorldOffset, emfInfo);
                if (config.alwaysSnapTrail) {
                    putOrAppendGatheredThisFrame(eid, gatheredTrails);
                }
                return new PlayerEmitters(true, gatheredTrails);
            }
        }
        List<Emitter> gatheredTrails = getVanillaTrailEmitters(basePose, animatedElytraRoot, elytraModel, entityWorldOffset, player);
        if (config.alwaysSnapTrail) {
            gatheredTrailsThisFrame.put(eid, gatheredTrails);
        }
        return new PlayerEmitters(true, gatheredTrails);
    }

    public void putOrAppendGatheredThisFrame(int eid, List<Emitter> emitters) {
        if (gatheredTrailsThisFrame.containsKey(eid)) {
            List<Emitter> newEmitters = new ArrayList<>(gatheredTrailsThisFrame.get(eid));
            newEmitters.addAll(emitters);
            gatheredTrailsThisFrame.put(eid, newEmitters);
            return;
        }
        gatheredTrailsThisFrame.put(eid, emitters);
    }

    public @NotNull EntityEmitters getEntityTrailEmitterPositions(Entity entity, float partialTick, ResolvedSampleSettings sampleSettings) {
        ModConfig config = getConfig();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ShaderChecksUtil.isShadowPass())  return new EntityEmitters(List.of(),false);
        if(entity instanceof ThrowableItemProjectile || entity instanceof FireworkRocketEntity || entity instanceof ExperienceOrb)
        {
            return new EntityEmitters(List.of(new Emitter(entity.getPosition(partialTick).add(offsetsFromSampleSettings(sampleSettings,1)),false, entity.getType().toShortString(), "trailSpawner", true)),false);
        }
        Camera camera = mc.gameRenderer.mainCamera();
        CameraRenderState cameraState = buildCameraState(camera);
        ModelFeatureRenderer.Submit<?> entitySubmit = extractEntityRenderState(entity, mc, cameraState, partialTick);

        if (entitySubmit == null || !(entitySubmit.model() instanceof EntityModel<?> entityModel) || !(entitySubmit.state() instanceof EntityRenderState entityRenderState)) {
            return new EntityEmitters(List.of(), false);
        }
        setupAnyModelAnim(entityModel, entityRenderState);
        PoseStack basePose = new PoseStack();
        basePose.last().set(entitySubmit.pose());

        Vec3 entityWorldOffset = new Vec3(entityRenderState.x, entityRenderState.y, entityRenderState.z);
        ModelPart animatedRoot = tryGetAnimatedEntityRoot(entityModel, entity, entityRenderState);
        int eid = entity.getId();

        if (!ElytraTrailsAPI.entityHasAnyTrailOverrides(entity) && ModStatuses.EMF_LOADED && config.emfSupport) {
            int variant = getModelVariantFromModel(entityModel);

            if (!emfCacheGeneric.containsKey(eid) || !(emfCacheGeneric.get(eid).variant() == variant)) {
                List<EmfWingTipHooks.SpawnerPath> found = EmfWingTipHooks.findAllSpawnerPathsGeneric(animatedRoot);
                emfCacheGeneric.put(eid, new EmfInfo(entity.getType().toShortString(), variant, getSpawnersInfo(found)));
                return new EntityEmitters(List.of(), true);
            }

            EmfInfo emfInfo = emfCacheGeneric.get(eid);

            if (!emfInfo.spawners.isEmpty()) {
                List<Emitter> gatheredTrails = getTrailEmittersFromBonesGeneric(basePose, animatedRoot, entityWorldOffset, emfInfo);
                if (config.alwaysSnapTrail) {
                    putOrAppendGatheredThisFrame(eid, gatheredTrails);
                }
                return new EntityEmitters(gatheredTrails,false);
            }
        }
        if(!sampleSettings.useWithoutEmf() || entity instanceof Avatar) {
            return new EntityEmitters(List.of(), false);
        }
        List<Emitter> gatheredTrails = getVanillaTrailEmittersGeneric(basePose, animatedRoot, entityModel, entityWorldOffset, entity, sampleSettings);
        if (config.alwaysSnapTrail) {
            putOrAppendGatheredThisFrame(eid, gatheredTrails);
        }
        return new EntityEmitters(gatheredTrails,false);
    }

    public void removeFromEmfCache(int eid) {
        emfCache.remove(eid);
        emfCacheGeneric.remove(eid);
    }

    public void removeAllEmfCache() {
        emfCache.clear();
        emfCacheGeneric.clear();
        childLookupCache.clear();
    }


    private List<SpawnerInfo> getSpawnersInfo(List<EmfWingTipHooks.SpawnerPath> spawners) {
        ArrayList<SpawnerInfo> spawnerInfos = new ArrayList<>();
        for (EmfWingTipHooks.SpawnerPath spawner : spawners) {
            spawnerInfos.add(new SpawnerInfo(spawner, splitPath(spawner.path()), inferLeftWing(spawner.where(), spawner.path())));
        }
        return spawnerInfos;
    }

    private String[] splitPath(String path) {
        if (path == null || path.isEmpty()) return new String[0];
        return path.split("/");
    }

    private @NotNull List<Emitter> getTrailEmittersFromBones(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ElytraModel model, @NotNull Vec3 entityWorldOffset, EmfInfo emfInfo) {
        ModelPart leftWing = model.leftWing;
        ModelPart rightWing = model.rightWing;
        List<SpawnerInfo> spawners = emfInfo.spawners;

        List<Emitter> emitters = new ArrayList<>();
        for (SpawnerInfo spawner : spawners) {
            ModelPart wingRoot = (spawner.spawner.where() == EmfWingTipHooks.WhichRoot.LEFT_WING) ? leftWing : rightWing;

            ResolvedEmitterPoint cameraRelative = transformLocalPointThroughPath(stack, elytraRoot, wingRoot, spawner.pathSegments());
            if (cameraRelative == null) continue;

            emitters.add(new Emitter(cameraRelative.position().add(entityWorldOffset), spawner.isLeftWing, "elytra" + (emfInfo.variant > 1 ? emfInfo.variant : ""), spawner.spawner.path(), cameraRelative.visible));
        }
        return emitters;
    }
    private @NotNull List<Emitter> getTrailEmittersFromBonesGeneric(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull Vec3 entityWorldOffset, EmfInfo emfInfo) {
        List<SpawnerInfo> spawners = emfInfo.spawners;

        List<Emitter> emitters = new ArrayList<>();
        for (SpawnerInfo spawner : spawners) {

            ResolvedEmitterPoint cameraRelative = transformLocalPointThroughPathGeneric(
                    stack, elytraRoot, spawner.pathSegments()
            );
            if (cameraRelative == null) continue;

            emitters.add(new Emitter(cameraRelative.position().add(entityWorldOffset), spawner.isLeftWing, emfInfo.name + (emfInfo.variant > 1 ? emfInfo.variant : ""), spawner.spawner.path(), cameraRelative.visible));
        }
        return emitters;
    }

    private static Vec3 offsetsFromSampleSettings(ResolvedSampleSettings sampleSettings, double divideBy)
    {
        Vector3d offsets = new Vector3d(-sampleSettings.xOffset() / divideBy, -sampleSettings.yOffset() / divideBy, -sampleSettings.zOffset() / divideBy);
        if(sampleSettings.billBoarded())
        {
            Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
            offsets = offsets.rotate(new Quaterniond(camera.rotation()));
        }
        return new Vec3(offsets.x, offsets.y, offsets.z);
    }
    private static boolean inferLeftWing(EmfWingTipHooks.WhichRoot modelRoot, String spawnerOrBoneName) {

        if (spawnerOrBoneName != null) {
            String bone = spawnerOrBoneName.substring(spawnerOrBoneName.lastIndexOf('/')).toLowerCase();
            return bone.contains("left");
        }

        return (modelRoot == EmfWingTipHooks.WhichRoot.LEFT_WING);
    }

    private @NotNull List<Emitter> getVanillaTrailEmitters(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ElytraModel model, @NotNull Vec3 entityWorldOffset, Avatar player) {
        ModelPart leftWing = model.leftWing;
        ModelPart rightWing = model.rightWing;

        Vec3 leftTip = computeTransformedWingTip(stack, elytraRoot, leftWing, ModelTransformationUtil.VANILLA_LEFT_WING_TIP, player);
        Vec3 rightTip = computeTransformedWingTip(stack, elytraRoot, rightWing, ModelTransformationUtil.VANILLA_RIGHT_WING_TIP, player);

        return List.of(
                new Emitter(leftTip.add(entityWorldOffset), true, "elytra", "/leftWingTip", true),
                new Emitter(rightTip.add(entityWorldOffset), false, "elytra", "/rightWingTip", true)
        );
    }

    private @NotNull List<Emitter> getVanillaTrailEmittersGeneric(@NotNull PoseStack stack, @Nullable ModelPart animatedRoot, @NotNull EntityModel<?> model, @NotNull Vec3 entityWorldOffset, Entity entity, ResolvedSampleSettings sampleSettings) {
        Vec3 offsets = offsetsFromSampleSettings(sampleSettings,16);
        ModelPart modelPart = animatedRoot != null ? animatedRoot : model.root();
        Vec3 tip = computeTransformedPoint(stack, modelPart, modelPart, offsets);
        return List.of(
                new Emitter(tip.add(entityWorldOffset), true, entity.getType().toShortString(), "/trailspawner", true)
        );
    }


    private @NotNull Vec3 computeTransformedWingTip(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ModelPart wingRoot, @NotNull Vec3 localPos, Avatar player) {
        float wingspread = ModelTransformationUtil.computeWingOpenness(wingRoot);

        PlayerConfig config = ClientPlayerConfigStore.getOrDefault(player.getId());
        float xScale = (float) config.wingtipDepthPosition();
        float zScale = (float) ((1.0 + (config.wingtipVerticalPosition() * 2.0)) / 3.0);
        float yScale = (float) config.wingtipHorizontalPosition();
        if (config.trailMovesWithElytraAngle()) {
            xScale = Mth.lerp(wingspread, 0.33333f, 1.0f);
        }
        if (config.trailMovesWithAngleOfAttack()) {
            zScale = Mth.lerp(getSignedElytraAoACalculation(player), 0.33333f, 1.0f);
        }
        Vec3 scaledLocalTip = new Vec3(localPos.x * xScale, localPos.y * yScale, localPos.z * zScale);
        return transformLocalPoint(stack, elytraRoot, wingRoot, scaledLocalTip);
    }

    private @NotNull Vec3 computeTransformedPoint(@NotNull PoseStack stack, @Nullable ModelPart modelRoot, @NotNull ModelPart partRoot, @NotNull Vec3 localPos) {
        stack.pushPose();
        if (modelRoot != null && modelRoot != partRoot) {
            modelRoot.translateAndRotate(stack);
        }
        partRoot.translateAndRotate(stack);
        Vec3 point = ModelTransformationUtil.transformPoint(stack.last().pose(), localPos);
        stack.popPose();
        return point;
    }

    private @Nullable ResolvedEmitterPoint transformLocalPointThroughPath(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ModelPart wingRoot, String[] pathSegments) {
        if (pathSegments.length == 0) return null;

        stack.pushPose();
        if (elytraRoot != null && elytraRoot != wingRoot) {
            elytraRoot.translateAndRotate(stack);
        }
        wingRoot.translateAndRotate(stack);

        ModelPart current = wingRoot;
        for (String segmentName : pathSegments) {
            if (segmentName.isEmpty()) continue;

            ModelPart child = findChildIgnoreCase(current, segmentName);
            if (child == null) {
                stack.popPose();
                return null;
            }

            child.translateAndRotate(stack);
            current = child;
        }

        Vec3 point = ModelTransformationUtil.transformPoint(stack.last().pose(), Vec3.ZERO);
        boolean visible = current.visible;
        stack.popPose();
        return new ResolvedEmitterPoint(point, visible);
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

    private @Nullable ResolvedEmitterPoint transformLocalPointThroughPathGeneric(@NotNull PoseStack stack, @Nullable ModelPart modelRoot, String[] pathSegments) {
        if (pathSegments.length == 0 || modelRoot == null) return null;

        stack.pushPose();
        try {
            modelRoot.translateAndRotate(stack);

            ModelPart current = modelRoot;
            for (String segmentName : pathSegments) {
                if (segmentName.isEmpty()) continue;

                ModelPart child = findChildIgnoreCase(current, segmentName);
                if (child == null) return null;

                child.translateAndRotate(stack);
                current = child;
            }

            return new ResolvedEmitterPoint(ModelTransformationUtil.transformPoint(stack.last().pose(), Vec3.ZERO), current.visible);
        } finally {
            stack.popPose();
        }
    }

    private @Nullable ModelPart findChildIgnoreCase(@NotNull ModelPart parent, @NotNull String name) {
        ModelPart direct = parent.children.get(name);
        if (direct != null) return direct;

        Map<String, ModelPart> cachedChildren = childLookupCache.computeIfAbsent(parent, p -> {
            Map<String, ModelPart> map = new HashMap<>();
            for (var entry : p.children.entrySet()) {
                map.put(entry.getKey().toLowerCase(), entry.getValue());
            }
            return map;
        });

        return cachedChildren.get(name.toLowerCase());
    }

    private @Nullable ModelPart tryGetAnimatedElytraRoot(ElytraModel model, Avatar player, EntityRenderState state) {
        if (!ModStatuses.EMF_LOADED || !getConfig().emfSupport) return null;
        try {
            return EmfAnimationHooks.applyManualAnimationAndGetRoot(model, player, state);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private @Nullable ModelPart tryGetAnimatedEntityRoot(EntityModel<?> model, Entity entity, EntityRenderState state) {
        if (!ModStatuses.EMF_LOADED || !getConfig().emfSupport) return null;
        try {
            return EmfAnimationHooks.applyManualAnimationAndGetRoot(model, entity, state);
        } catch (Throwable e) {
            return null;
        }
    }

    private @NotNull CameraRenderState buildCameraState(@NotNull Camera camera) {
        CameraRenderState state = new CameraRenderState();
        state.pos = camera.position();
        state.blockPos = BlockPos.containing(camera.position());
        state.initialized = true;
        state.orientation = new Quaternionf(camera.rotation());
        return state;
    }

    @SuppressWarnings("unchecked")
    private ModelFeatureRenderer.Submit<?> extractElytraRenderState(Avatar player, Minecraft mc, CameraRenderState cameraRenderState, float partialTick) {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        AvatarRenderState state = new AvatarRenderState();

        EntityRenderer<@NotNull Avatar, @NotNull AvatarRenderState> renderer = (EntityRenderer<@NotNull Avatar, @NotNull AvatarRenderState>) dispatcher.getRenderer(state);
        renderer.extractRenderState(player, state, partialTick);

        submitStorage.getSubmitsPerOrder().clear();
        dispatcher.submit(state, cameraRenderState, 0, 0, 0, new PoseStack(), submitStorage);
        return findElytraModelSubmit();
    }

    private ModelFeatureRenderer.Submit<?> extractEntityRenderState(Entity entity, Minecraft mc, CameraRenderState cameraRenderState, float partialTick) {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        EntityRenderState state = dispatcher.extractEntity(entity, partialTick);
        submitStorage.getSubmitsPerOrder().clear();
        dispatcher.submit(state, cameraRenderState, 0, 0, 0, new PoseStack(), submitStorage);

        List<SubmitNode> submits = getAllModelSubmits();
        ModelFeatureRenderer.Submit<?> fallback = null;
        for (SubmitNode submit : submits) {
            if(submit instanceof ModelFeatureRenderer.Submit<?> modelSubmit)
            {
                if(modelSubmit.model() instanceof ElytraModel)
                {
                    continue;
                }
                if (!(modelSubmit.model() instanceof EntityModel<?> entityModel)) continue;
                if (!(modelSubmit.state() instanceof EntityRenderState entityRenderState)) continue;
                setupAnyModelAnim(entityModel, entityRenderState);
                ModelPart animatedRoot = tryGetAnimatedEntityRoot(entityModel, entity, entityRenderState);
                if (fallback == null) {
                    fallback = modelSubmit;
                }
                if (animatedRoot == null) continue;
                List<EmfWingTipHooks.SpawnerPath> found = EmfWingTipHooks.findAllSpawnerPathsGeneric(animatedRoot);
                if (!found.isEmpty()) {
                    return modelSubmit;

                }
            }

        }
        return fallback;
    }

    /**
     * I know this is super cursed but im not sure how else to get the accurate positions without requiring an entity to be rendered (so that I can get the wingtips in first person)n
     */
    private ModelFeatureRenderer.Submit<?> findElytraModelSubmit() {

            List<SubmitNode> submits = getAllModelSubmits();
            for (SubmitNode submit : submits) {
                if(submit instanceof ModelFeatureRenderer.Submit<?> modelSubmit && modelSubmit.model() instanceof ElytraModel)
                {
                    return modelSubmit;
                }
            }
        if(CPM_LOADED)
        {
            return findCPMElytraModelSubmit();
        }
        return null;
    }

    private List<SubmitNode> getAllModelSubmits() {
        List<SubmitNode> out = new ArrayList<>();

        for (SubmitNodeCollection collection : submitStorage.getSubmitsPerOrder().values()) {
            SimpleFeatureRenderPhase solids = collection.solid;

            for(SimpleFeatureRenderPhase.FeatureSubmits<?> submits: Arrays.stream(solids.submitsByFeature).toList())
            {
                if(submits == null)
                {
                    continue;
                }
                out.addAll(submits.unbatched);
                for(List<?  > submitList : submits.batches.values())
                {
                    for(var item : submitList)
                    {
                        if(item instanceof SubmitNode submitNode)
                        {
                            out.add(submitNode);
                        }
                    }
                }
            }
            out.addAll((collection.translucentModels.submits));
        }
        return out;
    }

}