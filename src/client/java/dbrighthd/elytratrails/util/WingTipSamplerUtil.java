package dbrighthd.elytratrails.util;

import com.mojang.blaze3d.vertex.PoseStack;
import dbrighthd.elytratrails.compat.emf.EmfAnimationHooks;
import dbrighthd.elytratrails.compat.emf.EmfGenericTrailSampler;
import dbrighthd.elytratrails.compat.emf.EmfWingTipHooks;
import dbrighthd.elytratrails.mixin.client.EquipmentElytraModelAccessor;
import dbrighthd.elytratrails.mixin.client.ModelFeatureStorageAccessor;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import dbrighthd.elytratrails.trailrendering.WingTipPos;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
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
import net.minecraft.util.Util;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.CLIENT_PLAYER_CONFIGS;
import static dbrighthd.elytratrails.util.ModelTransformationUtil.computeWingOpenness;
import static dbrighthd.elytratrails.util.ModelTransformationUtil.computeWingTipLocal;
import static dbrighthd.elytratrails.util.ModelTransformationUtil.transformPoint;

public final class WingTipSamplerUtil {
    private WingTipSamplerUtil() {}

    private static final SubmitNodeStorage SUBMIT_STORAGE = new SubmitNodeStorage();
    private static final boolean EMF_LOADED = FabricLoader.getInstance().isModLoaded("entity_model_features");

    public static void sample(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (ShaderChecksUtil.isShadowPass()) return;

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        Vec3 cameraWorldPos = minecraft.gameRenderer.getMainCamera().position();
        long nowNanos = Util.getNanos();

        CameraRenderState cameraState = buildCameraState(minecraft, cameraWorldPos);
        double cameraX = -cameraWorldPos.x;
        double cameraY = -cameraWorldPos.y;
        double cameraZ = -cameraWorldPos.z;

        // -------- Players (elytra) --------
        for (Player player : minecraft.level.players()) {
            samplePlayerElytra(
                    player,
                    dispatcher,
                    cameraState,
                    cameraX,
                    cameraY,
                    cameraZ,
                    partialTick,
                    cameraWorldPos,
                    nowNanos
            );
        }

        // -------- Non-player EMF entities --------
        if (EMF_LOADED && getConfig().emfSupport) {
            EmfGenericTrailSampler.sampleNonPlayerEntities(
                    minecraft, dispatcher, cameraState, cameraX, cameraY, cameraZ, partialTick, cameraWorldPos, nowNanos
            );
        }
    }

    private static CameraRenderState buildCameraState(Minecraft minecraft, Vec3 cameraWorldPos) {
        CameraRenderState cameraState = new CameraRenderState();
        cameraState.pos = cameraWorldPos;
        cameraState.entityPos = cameraWorldPos;
        cameraState.blockPos = BlockPos.containing(cameraWorldPos);
        cameraState.initialized = true;
        cameraState.orientation = new Quaternionf(minecraft.gameRenderer.getMainCamera().rotation());
        return cameraState;
    }

    private static void samplePlayerElytra(
            Player player,
            EntityRenderDispatcher dispatcher,
            CameraRenderState cameraState,
            double cameraX,
            double cameraY,
            double cameraZ,
            float partialTick,
            Vec3 cameraWorldPos,
            long nowNanos
    ) {
        int entityId = player.getId();

        AvatarRenderState renderState = new AvatarRenderState();

        @SuppressWarnings("unchecked")
        EntityRenderer<@NotNull Avatar, @NotNull AvatarRenderState> renderer =
                (EntityRenderer<@NotNull Avatar, @NotNull AvatarRenderState>) dispatcher.getRenderer(renderState);

        renderer.extractRenderState(player, renderState, partialTick);

        SUBMIT_STORAGE.clear();
        dispatcher.submit(renderState, cameraState, cameraX, cameraY, cameraZ, new PoseStack(), SUBMIT_STORAGE);

        SubmitNodeStorage.ModelSubmit<?> elytraSubmit = findElytraModelSubmit();
        if (elytraSubmit == null) return;

        Model<?> model = elytraSubmit.model();
        Object submitState = elytraSubmit.state();

        @SuppressWarnings("unchecked")
        Model<Object> typed = (Model<Object>) model;
        typed.setupAnim(submitState);

        if (!(model instanceof ElytraModel elytraModel)) return;
        if (!(submitState instanceof HumanoidRenderState)) return;

        EquipmentElytraModelAccessor elytraAccessor = (EquipmentElytraModelAccessor) elytraModel;
        ModelPart leftWingRoot = elytraAccessor.elytratrails$getLeftWing();
        ModelPart rightWingRoot = elytraAccessor.elytratrails$getRightWing();

        PoseStack basePose = new PoseStack();
        basePose.last().set(elytraSubmit.pose());

        @Nullable ModelPart elytraRoot = resolveAnimatedElytraRootIfAvailable(model, player);

        Vec3 entityWorldOffset = new Vec3(renderState.x, renderState.y, renderState.z);

        List<EmfWingTipHooks.SpawnerPath> spawners = List.of();
        if (EMF_LOADED && getConfig().emfSupport) {
            spawners = EmfWingTipHooks.findAllSpawnerPaths(leftWingRoot, rightWingRoot);
        }

        if (!spawners.isEmpty()) {
            sampleSpawnerBones(
                    entityId,
                    spawners,
                    basePose,
                    elytraRoot,
                    leftWingRoot,
                    rightWingRoot,
                    cameraWorldPos,
                    entityWorldOffset,
                    nowNanos
            );
        } else {
            sampleVanillaWingTips(
                    entityId,
                    basePose,
                    elytraRoot,
                    leftWingRoot,
                    rightWingRoot,
                    cameraWorldPos,
                    entityWorldOffset,
                    nowNanos
            );
        }
    }

    private static @Nullable ModelPart resolveAnimatedElytraRootIfAvailable(Model<?> model, Player player) {
        if (!EMF_LOADED || !getConfig().emfSupport) return null;
        try {
            return EmfAnimationHooks.applyManualAnimationAndGetRoot(model, player);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void sampleSpawnerBones(
            int entityId,
            List<EmfWingTipHooks.SpawnerPath> spawners,
            PoseStack basePose,
            @Nullable ModelPart elytraRoot,
            ModelPart leftWingRoot,
            ModelPart rightWingRoot,
            Vec3 cameraWorldPos,
            Vec3 entityWorldOffset,
            long nowNanos
    ) {
        Vec3[] pointsWorld = new Vec3[spawners.size()];
        String[] boneNames = new String[spawners.size()];

        for (int i = 0; i < spawners.size(); i++) {
            EmfWingTipHooks.SpawnerPath spawner = spawners.get(i);
            ModelPart wingRoot = (spawner.where() == EmfWingTipHooks.WhichRoot.LEFT_WING) ? leftWingRoot : rightWingRoot;

            boneNames[i] = lastPathSegment(spawner.path());

            Vec3 cameraRelative = transformLocalPointThroughElytraRootAndChildPath(
                    basePose, elytraRoot, wingRoot, spawner.path()
            );
            if (cameraRelative == null) continue;

            pointsWorld[i] = cameraWorldPos.add(cameraRelative).add(entityWorldOffset);
        }

        // Player elytra "model name" isn't stable across skins/mods; keep null here.
        WingTipPos.put(entityId, pointsWorld, nowNanos, null, boneNames);
    }

    private static void sampleVanillaWingTips(
            int entityId,
            PoseStack basePose,
            @Nullable ModelPart elytraRoot,
            ModelPart leftWingRoot,
            ModelPart rightWingRoot,
            Vec3 cameraWorldPos,
            Vec3 entityWorldOffset,
            long nowNanos
    ) {
        Vec3 leftTipCameraRelative = fallbackWingTip(basePose, elytraRoot, leftWingRoot, true, entityId);
        Vec3 rightTipCameraRelative = fallbackWingTip(basePose, elytraRoot, rightWingRoot, false, entityId);

        Vec3[] pointsWorld = new Vec3[]{
                cameraWorldPos.add(leftTipCameraRelative).add(entityWorldOffset),
                cameraWorldPos.add(rightTipCameraRelative).add(entityWorldOffset)
        };

        WingTipPos.put(entityId, pointsWorld, nowNanos, "elytra", new String[]{"left", "right"});
    }

    private static Vec3 fallbackWingTip(PoseStack basePose, @Nullable ModelPart elytraRoot, ModelPart wingRoot, boolean left, int entityId) {
        Vec3 wingTipLocal = computeWingTipLocal(left);

        float wingOpenness = computeWingOpenness(wingRoot);
        float localTipXScale = 1.0f;

        PlayerConfig cfg = ClientPlayerConfigStore.getOrDefault(entityId);
        if (cfg.trailMovesWithElytraAngle()) {
            localTipXScale = Mth.lerp(wingOpenness, 0.33f, 1.0f);
        }

        Vec3 scaledLocalTip = new Vec3(wingTipLocal.x * localTipXScale, wingTipLocal.y, wingTipLocal.z);
        return transformLocalPointThroughElytraRootAndWing(basePose, elytraRoot, wingRoot, scaledLocalTip);
    }

    private static Vec3 transformLocalPointThroughElytraRootAndWing(
            PoseStack basePose,
            @Nullable ModelPart elytraRoot,
            ModelPart wingRoot,
            Vec3 localPoint
    ) {
        basePose.pushPose();
        try {
            if (elytraRoot != null && elytraRoot != wingRoot) {
                elytraRoot.translateAndRotate(basePose);
            }
            wingRoot.translateAndRotate(basePose);
            return transformPoint(basePose.last().pose(), localPoint);
        } finally {
            basePose.popPose();
        }
    }

    private static @Nullable Vec3 transformLocalPointThroughElytraRootAndChildPath(
            PoseStack basePose,
            @Nullable ModelPart elytraRoot,
            ModelPart wingRoot,
            String childOnlyPath
    ) {
        if (childOnlyPath == null || childOnlyPath.isEmpty()) return null;

        basePose.pushPose();
        try {
            if (elytraRoot != null && elytraRoot != wingRoot) {
                elytraRoot.translateAndRotate(basePose);
            }

            wingRoot.translateAndRotate(basePose);

            ModelPart current = wingRoot;
            int segmentStartIndex = 0;

            while (segmentStartIndex < childOnlyPath.length()) {
                int nextSlashIndex = childOnlyPath.indexOf('/', segmentStartIndex);
                String segmentName = (nextSlashIndex == -1)
                        ? childOnlyPath.substring(segmentStartIndex)
                        : childOnlyPath.substring(segmentStartIndex, nextSlashIndex);

                ModelPart child = findChildIgnoreCase(current, segmentName);
                if (child == null) return null;

                child.translateAndRotate(basePose);
                current = child;

                if (nextSlashIndex == -1) break;
                segmentStartIndex = nextSlashIndex + 1;
            }

            return transformPoint(basePose.last().pose(), Vec3.ZERO);
        } finally {
            basePose.popPose();
        }
    }

    private static @Nullable ModelPart findChildIgnoreCase(ModelPart parent, String name) {
        ModelPart direct = parent.children.get(name);
        if (direct != null) return direct;

        for (var entry : parent.children.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static @Nullable String lastPathSegment(@Nullable String path) {
        if (path == null) return null;
        int idx = path.lastIndexOf('/');
        return (idx == -1) ? path : path.substring(idx + 1);
    }

    private static SubmitNodeStorage.@Nullable ModelSubmit<?> findElytraModelSubmit() {
        for (SubmitNodeCollection collection : WingTipSamplerUtil.SUBMIT_STORAGE.getSubmitsPerOrder().values()) {
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
}
