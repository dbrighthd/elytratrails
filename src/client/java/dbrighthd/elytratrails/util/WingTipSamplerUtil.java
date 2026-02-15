// WingTipSampler.java
package dbrighthd.elytratrails.util;

import com.mojang.blaze3d.vertex.PoseStack;
import dbrighthd.elytratrails.mixin.client.EquipmentElytraModelAccessor;
import dbrighthd.elytratrails.mixin.client.ModelFeatureStorageAccessor;
import dbrighthd.elytratrails.trailrendering.WingTipPos;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.util.ModelTransformationUtil.*;

public final class WingTipSamplerUtil {
    private WingTipSamplerUtil() {}

    private static final Int2ObjectOpenHashMap<EntityRenderState> STATE_CACHE = new Int2ObjectOpenHashMap<>();

    private static final SubmitNodeStorage STORAGE = new SubmitNodeStorage();

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
            EntityRenderer renderer = dispatcher.getRenderer(player);

            int entityId = player.getId();

            EntityRenderState state = STATE_CACHE.get(entityId);
            if (state == null) {
                state = (EntityRenderState) renderer.createRenderState();
                STATE_CACHE.put(entityId, state);
            }

            // Update state offscreen
            renderer.extractRenderState(player, state, partialTick);
            if (!(state instanceof AvatarRenderState avatar)) continue;

            STORAGE.clear();
            dispatcher.submit(avatar, camState, camX, camY, camZ, new PoseStack(), STORAGE);
            int orders = STORAGE.getSubmitsPerOrder().size();
            int totalModelSubmits = 0;

            for (var entry : STORAGE.getSubmitsPerOrder().int2ObjectEntrySet()) {
                SubmitNodeCollection col = entry.getValue();
                var modelStorage = col.getModelSubmits();
                var acc = (dbrighthd.elytratrails.mixin.client.ModelFeatureStorageAccessor)(Object) modelStorage;

                for (var list : acc.elytratrails$getOpaqueModelSubmits().values()) totalModelSubmits += list.size();
                totalModelSubmits += acc.elytratrails$getTranslucentModelSubmits().size();
            }

            SubmitNodeStorage.ModelSubmit<?> elytraSubmit = findElytraModelSubmit(STORAGE);
            if (elytraSubmit == null) continue;

            Model rawModel = (Model) elytraSubmit.model();
            Object submitState = elytraSubmit.state();
            rawModel.setupAnim(submitState);

            if (!(rawModel instanceof ElytraModel elytraModel)) continue;
            if (!(submitState instanceof HumanoidRenderState humanoidState)) continue;


            EquipmentElytraModelAccessor elytraAccessor = (EquipmentElytraModelAccessor) (Object) elytraModel;
            ModelPart leftWingPart = elytraAccessor.elytratrails$getLeftWing();
            ModelPart rightWingPart = elytraAccessor.elytratrails$getRightWing();


            PoseStack base = new PoseStack();
            base.last().set(elytraSubmit.pose());


            Vec3 leftWingTipLocal = computeWingTipLocal(leftWingPart, true);
            Vec3 rightWingTipLocal = computeWingTipLocal(rightWingPart, false);

            float wingOpenness = computeWingOpenness(leftWingPart);
            float localTipXScale = 1.0f;
            if (getConfig().trailMovesWithElytraAngle) {
                localTipXScale = Mth.lerp(wingOpenness, 0.33f, 1.0f);
            }

            leftWingTipLocal = new Vec3(leftWingTipLocal.x * localTipXScale, leftWingTipLocal.y, leftWingTipLocal.z);
            rightWingTipLocal = new Vec3(rightWingTipLocal.x * localTipXScale, rightWingTipLocal.y, rightWingTipLocal.z);


            Vec3 leftTipCamRelative = transformLocalPointThroughPart(base, leftWingPart, leftWingTipLocal);
            Vec3 rightTipCamRelative = transformLocalPointThroughPart(base, rightWingPart, rightWingTipLocal);

            Vec3 entityWorldPos = new Vec3(avatar.x, avatar.y, avatar.z);

            Vec3 leftTipWorld = cameraWorldPos.add(leftTipCamRelative);
            Vec3 rightTipWorld = cameraWorldPos.add(rightTipCamRelative);

            leftTipWorld = leftTipWorld.add(entityWorldPos);
            rightTipWorld = rightTipWorld.add(entityWorldPos);

            WingTipPos.put(entityId, leftTipWorld, rightTipWorld, now);
        }
    }

    private static SubmitNodeStorage.ModelSubmit<?> findElytraModelSubmit(SubmitNodeStorage storage) {

        for (SubmitNodeCollection collection : storage.getSubmitsPerOrder().values()) {
            ModelFeatureRenderer.Storage modelStorage = collection.getModelSubmits();
            ModelFeatureStorageAccessor access = (ModelFeatureStorageAccessor) (Object) modelStorage;

            Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaque = access.elytratrails$getOpaqueModelSubmits();
            for (List<SubmitNodeStorage.ModelSubmit<?>> submits : opaque.values()) {
                for (SubmitNodeStorage.ModelSubmit<?> s : submits) {
                    if (s.model() instanceof ElytraModel) {
                        return s;
                    }
                }
            }

            List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucent = access.elytratrails$getTranslucentModelSubmits();
            for (SubmitNodeStorage.TranslucentModelSubmit<?> t : translucent) {
                SubmitNodeStorage.ModelSubmit<?> s = t.modelSubmit();
                if (s.model() instanceof ElytraModel) {
                    return s;
                }
            }
        }
        return null;
    }

    public static void removeEntity(int entityId) {
        STATE_CACHE.remove(entityId);
    }
}
