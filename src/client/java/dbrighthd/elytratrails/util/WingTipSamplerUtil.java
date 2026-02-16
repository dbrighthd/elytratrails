package dbrighthd.elytratrails.util;

import com.mojang.blaze3d.vertex.PoseStack;
import dbrighthd.elytratrails.compat.emf.EmfAnimationHooks;
import dbrighthd.elytratrails.compat.emf.EmfWingTipHooks;
import dbrighthd.elytratrails.mixin.client.EquipmentElytraModelAccessor;
import dbrighthd.elytratrails.mixin.client.ModelFeatureStorageAccessor;
import dbrighthd.elytratrails.trailrendering.WingTipPos;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
import java.util.List;
import java.util.Map;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.util.ModelTransformationUtil.*;

public final class WingTipSamplerUtil {
    private WingTipSamplerUtil() {}


    private static final Int2ObjectOpenHashMap<net.minecraft.client.renderer.entity.state.EntityRenderState> STATE_CACHE =
            new Int2ObjectOpenHashMap<>();

    private static final SubmitNodeStorage STORAGE = new SubmitNodeStorage();

    private static final boolean EMF_LOADED = FabricLoader.getInstance().isModLoaded("entity_model_features");

    public static void sample(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (ShaderChecksUtil.isShadowPass()) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        Vec3 cameraWorldPos = mc.gameRenderer.getMainCamera().position();
        long now = Util.getNanos();

        CameraRenderState camState = new CameraRenderState();
        camState.pos = cameraWorldPos;
        camState.entityPos = cameraWorldPos;
        camState.blockPos = BlockPos.containing(cameraWorldPos);
        camState.initialized = true;
        camState.orientation = new Quaternionf(mc.gameRenderer.getMainCamera().rotation());

        double camX = -cameraWorldPos.x;
        double camY = -cameraWorldPos.y;
        double camZ = -cameraWorldPos.z;

        for (Player player : mc.level.players()) {
            int entityId = player.getId();

            AvatarRenderState state = new AvatarRenderState();
            @SuppressWarnings("unchecked")
            var renderer = (EntityRenderer<@NotNull Avatar, @NotNull AvatarRenderState>) dispatcher.getRenderer(state);

            renderer.extractRenderState(player, state, partialTick);

            STORAGE.clear();
            dispatcher.submit(state, camState, camX, camY, camZ, new PoseStack(), STORAGE);

            SubmitNodeStorage.ModelSubmit<?> elytraSubmit = findElytraModelSubmit(STORAGE);
            if (elytraSubmit == null) continue;

            Model rawModel = elytraSubmit.model();
            Object submitState = elytraSubmit.state();

            rawModel.setupAnim(submitState);

            if (!(rawModel instanceof ElytraModel elytraModel)) continue;
            if (!(submitState instanceof HumanoidRenderState)) continue;

            EquipmentElytraModelAccessor elytraAccessor = (EquipmentElytraModelAccessor) elytraModel;
            ModelPart leftWingRoot = elytraAccessor.elytratrails$getLeftWing();
            ModelPart rightWingRoot = elytraAccessor.elytratrails$getRightWing();

            ModelPart elytraRoot = null;

            PoseStack base = new PoseStack();
            base.last().set(elytraSubmit.pose());

            if (EMF_LOADED && getConfig().emfSupport) {
                ModelPart emfRoot = EmfAnimationHooks.applyManualAnimationAndGetRoot(rawModel, player);
                if (emfRoot != null) {
                    elytraRoot = emfRoot;
                }
            }

            EmfWingTipHooks.TipPaths tips = EmfWingTipHooks.findTipPaths(leftWingRoot, rightWingRoot);


            Vec3 leftTipCamRelative;
            Vec3 rightTipCamRelative;

            if (tips != null) {
                ModelPart leftRootForTip = (tips.leftRoot() == EmfWingTipHooks.WhichRoot.LEFT_WING) ? leftWingRoot : rightWingRoot;
                ModelPart rightRootForTip = (tips.rightRoot() == EmfWingTipHooks.WhichRoot.LEFT_WING) ? leftWingRoot : rightWingRoot;

                Vec3 l = transformLocalPointThroughElytraRootAndChildPath(base, elytraRoot, leftRootForTip, tips.leftPath(), new Vec3(0, 0, 0));
                Vec3 r = transformLocalPointThroughElytraRootAndChildPath(base, elytraRoot, rightRootForTip, tips.rightPath(), new Vec3(0, 0, 0));


                if (l != null && r != null) {
                    leftTipCamRelative = l;
                    rightTipCamRelative = r;
                } else {
                    leftTipCamRelative = fallbackWingTip(base, elytraRoot, leftWingRoot, true);
                    rightTipCamRelative = fallbackWingTip(base, elytraRoot, rightWingRoot, false);
                }
            } else {
                leftTipCamRelative = fallbackWingTip(base, elytraRoot, leftWingRoot, true);
                rightTipCamRelative = fallbackWingTip(base, elytraRoot, rightWingRoot, false);
            }

            Vec3 entityWorldPos = new Vec3(state.x, state.y, state.z);

            Vec3 leftTipWorld = cameraWorldPos.add(leftTipCamRelative).add(entityWorldPos);
            Vec3 rightTipWorld = cameraWorldPos.add(rightTipCamRelative).add(entityWorldPos);


            WingTipPos.put(entityId, leftTipWorld, rightTipWorld, now);
        }
    }


    private static Vec3 fallbackWingTip(PoseStack base, ModelPart elytraRoot, ModelPart wingRoot, boolean left) {
        Vec3 wingTipLocal = computeWingTipLocal(wingRoot, left);

        float wingOpenness = computeWingOpenness(wingRoot);
        float localTipXScale = 1.0f;
        if (getConfig().trailMovesWithElytraAngle) {
            localTipXScale = Mth.lerp(wingOpenness, 0.33f, 1.0f);
        }

        wingTipLocal = new Vec3(wingTipLocal.x * localTipXScale, wingTipLocal.y, wingTipLocal.z);

        return transformLocalPointThroughElytraRootAndWing(base, elytraRoot, wingRoot, wingTipLocal);
    }

    private static Vec3 transformLocalPointThroughElytraRootAndWing(PoseStack basePoseStack, ModelPart elytraRoot, ModelPart wingRoot, Vec3 localPoint) {
        basePoseStack.pushPose();
        try {
            if (elytraRoot != null && elytraRoot != wingRoot) {
                elytraRoot.translateAndRotate(basePoseStack);
            }
            wingRoot.translateAndRotate(basePoseStack);
            return transformPoint(basePoseStack.last().pose(), localPoint);
        } finally {
            basePoseStack.popPose();
        }
    }


    private static Vec3 transformLocalPointThroughElytraRootAndChildPath(
            PoseStack basePoseStack,
            ModelPart elytraRoot,
            ModelPart wingRoot,
            String childOnlyPath,
            Vec3 localPoint
    ) {
        if (wingRoot == null || childOnlyPath == null || childOnlyPath.isEmpty()) return null;

        basePoseStack.pushPose();
        try {
            if (elytraRoot != null && elytraRoot != wingRoot) {
                elytraRoot.translateAndRotate(basePoseStack);
            }

            wingRoot.translateAndRotate(basePoseStack);

            ModelPart cur = wingRoot;
            int start = 0;
            while (start < childOnlyPath.length()) {
                int slash = childOnlyPath.indexOf('/', start);
                String key = (slash == -1) ? childOnlyPath.substring(start) : childOnlyPath.substring(start, slash);

                Map<String, ModelPart> kids = cur.children;
                ModelPart next = kids.get(key);

                if (next == null) {
                    for (var e : kids.entrySet()) {
                        if (e.getKey().equalsIgnoreCase(key)) {
                            next = e.getValue();
                            break;
                        }
                    }
                }
                if (next == null) return null;

                next.translateAndRotate(basePoseStack);
                cur = next;

                if (slash == -1) break;
                start = slash + 1;
            }

            return transformPoint(basePoseStack.last().pose(), localPoint);
        } finally {
            basePoseStack.popPose();
        }
    }

    private static SubmitNodeStorage.ModelSubmit<?> findElytraModelSubmit(SubmitNodeStorage storage) {
        for (SubmitNodeCollection collection : storage.getSubmitsPerOrder().values()) {
            ModelFeatureRenderer.Storage modelStorage = collection.getModelSubmits();
            ModelFeatureStorageAccessor access = (ModelFeatureStorageAccessor) (Object) modelStorage;

            Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaque = access.elytratrails$getOpaqueModelSubmits();
            for (List<SubmitNodeStorage.ModelSubmit<?>> submits : opaque.values()) {
                for (SubmitNodeStorage.ModelSubmit<?> s : submits) {
                    if (s.model() instanceof ElytraModel) return s;
                }
            }

            List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucent = access.elytratrails$getTranslucentModelSubmits();
            for (SubmitNodeStorage.TranslucentModelSubmit<?> t : translucent) {
                SubmitNodeStorage.ModelSubmit<?> s = t.modelSubmit();
                if (s.model() instanceof ElytraModel) return s;
            }
        }
        return null;
    }

    public static void removeEntity(int entityId) {
        STATE_CACHE.remove(entityId);
    }
}
