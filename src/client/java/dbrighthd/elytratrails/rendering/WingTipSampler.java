package dbrighthd.elytratrails.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dbrighthd.elytratrails.ElytraTrailsClient;
import dbrighthd.elytratrails.compat.ModStatuses;
import dbrighthd.elytratrails.compat.emf.EmfAnimationHooks;
import dbrighthd.elytratrails.compat.emf.EmfModelNameUtil;
import dbrighthd.elytratrails.compat.emf.EmfWingTipHooks;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.mixin.client.EquipmentElytraModelAccessor;
import dbrighthd.elytratrails.mixin.client.ModelFeatureStorageAccessor;
import dbrighthd.elytratrails.util.ModelTransformationUtil;
import dbrighthd.elytratrails.util.ShaderChecksUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dbrighthd.elytratrails.compat.emf.EmfModelNameUtil.getModelVariantFromModel;

public class WingTipSampler {
    private final SubmitNodeStorage submitStorage = new SubmitNodeStorage();
    private final Map<Integer, emfInfo> emfCache = new HashMap<>();

    private record emfInfo(int variant, List<spawnerInfo> spawners){}
    private record spawnerInfo(EmfWingTipHooks.SpawnerPath spawner, boolean isLeftWing){}

    public @NotNull List<Emitter> getTrailEmitterPositions(Player player, float partialTick) {
        ModConfig config = ElytraTrailsClient.getConfig();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ShaderChecksUtil.isShadowPass()) return List.of();

        Camera camera = mc.gameRenderer.getMainCamera();
        CameraRenderState cameraState = buildCameraState(camera);

        SubmitNodeStorage.ModelSubmit<?> elytraSubmit = extractElytraRenderState(player, mc, cameraState, partialTick);
        if (elytraSubmit == null || !(elytraSubmit.model() instanceof ElytraModel elytraModel) || !(elytraSubmit.state() instanceof HumanoidRenderState humanoidState)) return List.of();

        elytraModel.setupAnim(humanoidState);

        EquipmentElytraModelAccessor accessor = (EquipmentElytraModelAccessor) elytraModel;
        ModelPart leftWing = accessor.elytratrails$getLeftWing();
        ModelPart rightWing = accessor.elytratrails$getRightWing();

        PoseStack basePose = new PoseStack();
        basePose.last().set(elytraSubmit.pose());

        Vec3 entityWorldOffset = new Vec3(humanoidState.x, humanoidState.y, humanoidState.z);
        ModelPart animatedElytraRoot = tryGetAnimatedElytraRoot(elytraModel, player);

        if (ModStatuses.EMF_LOADED && config.emfSupport) {
            int eid = player.getId();
            int variant = getModelVariantFromModel(animatedElytraRoot);

            if (!emfCache.containsKey(eid) || !(emfCache.get(eid).variant() == variant))
            {
                emfCache.put(eid,new emfInfo(variant, getSpawnersInfo(EmfWingTipHooks.findAllSpawnerPaths(leftWing, rightWing))));
                return List.of();
            }
            List<spawnerInfo> spawners = emfCache.get(eid).spawners();
            if (!spawners.isEmpty()) return getTrailEmittersFromBones(basePose, animatedElytraRoot, elytraModel, spawners, camera.position(), entityWorldOffset);
        }

        return getVanillaTrailEmitters(basePose, animatedElytraRoot, elytraModel, camera.position(), entityWorldOffset);
    }

    public void removeFromEmfCache(int eid)
    {
        emfCache.remove(eid);
    }
    public void removeAllEmfCache()
    {
        emfCache.clear();
    }
    private List<spawnerInfo> getSpawnersInfo(List<EmfWingTipHooks.SpawnerPath> spawners)
    {
        ArrayList<spawnerInfo> spawnerInfos = new ArrayList<>();
        for(EmfWingTipHooks.SpawnerPath spawner : spawners)
        {
            spawnerInfos.add(new spawnerInfo(spawner, inferLeftWing(spawner.where(), spawner.path())));
        }
        return spawnerInfos;
    }

    private @NotNull List<Emitter> getTrailEmittersFromBones(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ElytraModel model, @NotNull List<spawnerInfo> spawners, @NotNull Vec3 cameraPos, @NotNull Vec3 entityWorldOffset) {
        EquipmentElytraModelAccessor accessor = (EquipmentElytraModelAccessor) model;
        ModelPart leftWing = accessor.elytratrails$getLeftWing();
        ModelPart rightWing = accessor.elytratrails$getRightWing();


        List<Emitter> emitters = new ArrayList<>();
        for (spawnerInfo spawner : spawners) {
            ModelPart wingRoot = (spawner.spawner.where() == EmfWingTipHooks.WhichRoot.LEFT_WING) ? leftWing : rightWing;

            Vec3 cameraRelative = transformLocalPointThroughPath(
                    stack, elytraRoot, wingRoot, spawner.spawner.path()
            );
            if (cameraRelative == null) continue;

            emitters.add(new Emitter(cameraPos.add(cameraRelative).add(entityWorldOffset), spawner.isLeftWing));
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

    private @NotNull List<Emitter> getVanillaTrailEmitters(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ElytraModel model, @NotNull Vec3 cameraPos, @NotNull Vec3 entityWorldOffset) {
        EquipmentElytraModelAccessor accessor = (EquipmentElytraModelAccessor) model;
        ModelPart leftWing = accessor.elytratrails$getLeftWing();
        ModelPart rightWing = accessor.elytratrails$getRightWing();

        Vec3 leftTip = computeTransformedWingTip(stack, elytraRoot, leftWing, ModelTransformationUtil.VANILLA_LEFT_WING_TIP);
        Vec3 rightTip = computeTransformedWingTip(stack, elytraRoot, rightWing, ModelTransformationUtil.VANILLA_RIGHT_WING_TIP);

        return List.of(
                new Emitter(cameraPos.add(leftTip).add(entityWorldOffset), true),
                new Emitter (cameraPos.add(rightTip).add(entityWorldOffset), false)
        );
    }

    private @NotNull Vec3 computeTransformedWingTip(@NotNull PoseStack stack, @Nullable ModelPart elytraRoot, @NotNull ModelPart wingRoot, @NotNull Vec3 localPos) {
        float wingspread = ModelTransformationUtil.computeWingOpenness(wingRoot);

        float xScale = Mth.lerp(wingspread, 0.33f, 1.0f); // TODO: configurable (why?)
        Vec3 scaledLocalTip = new Vec3(localPos.x * xScale, localPos.y, localPos.z);
        return transformLocalPoint(stack, elytraRoot, wingRoot, scaledLocalTip);
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
    private SubmitNodeStorage.ModelSubmit<?> extractElytraRenderState(Player player, Minecraft mc, CameraRenderState cameraRenderState, float partialTick) {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        AvatarRenderState state = new AvatarRenderState();

        EntityRenderer<Avatar, AvatarRenderState> renderer = (EntityRenderer<Avatar, AvatarRenderState>) dispatcher.getRenderer(state);
        renderer.extractRenderState(player, state, partialTick);

        submitStorage.clear();
        dispatcher.submit(state, cameraRenderState, -cameraRenderState.pos.x, -cameraRenderState.pos.y, -cameraRenderState.pos.z, new PoseStack(), submitStorage);

        return findElytraModelSubmit();
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
    private static boolean containsWord(String haystackLower, String needleLower) {
        int n = haystackLower.length();
        int m = needleLower.length();
        if (m == 0 || n < m) return false;

        int from = 0;
        while (true) {
            int idx = haystackLower.indexOf(needleLower, from);
            if (idx < 0) return false;

            int before = idx - 1;
            int after = idx + m;

            boolean beforeOk = before < 0 || !Character.isLetterOrDigit(haystackLower.charAt(before));
            boolean afterOk = after >= n || !Character.isLetterOrDigit(haystackLower.charAt(after));

            if (beforeOk && afterOk) return true;
            from = idx + 1;
        }
    }
}
