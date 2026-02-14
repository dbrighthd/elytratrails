package dbrighthd.elytratrails.mixin.client;

import dbrighthd.elytratrails.util.FrameCounterUtil;
import dbrighthd.elytratrails.util.ShaderChecksUtil;
import dbrighthd.elytratrails.trailrendering.WingTipPos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.util.ModelTransformationUtil.*;

@Mixin(net.minecraft.client.renderer.feature.ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    @Unique
    private static final Int2IntOpenHashMap lastCapturedFrameByEntityId = new Int2IntOpenHashMap();

    @Inject(method = "renderModel", at = @At("TAIL"))
    private static <S> void elytratrails$captureElytraWingTips(
            SubmitNodeStorage.ModelSubmit<S> modelSubmit,
            RenderType renderType,
            VertexConsumer vertexConsumer,
            OutlineBufferSource outlineBufferSource,
            MultiBufferSource.BufferSource bufferSource,
            CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        //skip if iris is doing shadowpass (or else stuff gets weird)
        if (ShaderChecksUtil.isShadowPass()) return;

        Model<? super S> submittedModel = modelSubmit.model();
        if (!(submittedModel instanceof ElytraModel elytraModel)) return;

        Object renderState = modelSubmit.state();
        if (!(renderState instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState)) return;

        int entityId = ((AvatarRenderStateAccessor) renderState).elytratrails$getId();

        //only get one entity per frame, or else stuff like the UI player model will override the wingtip positions
        int currentFrameId = FrameCounterUtil.frameId;
        int lastCapturedFrameId = lastCapturedFrameByEntityId.getOrDefault(entityId, Integer.MIN_VALUE);
        if (lastCapturedFrameId == currentFrameId) return;
        lastCapturedFrameByEntityId.put(entityId, currentFrameId);

        // pose matrix provided by the submit node
        Matrix4f submitPoseMatrix = modelSubmit.pose().pose();

        // camera world position
        Vec3 cameraWorldPos = minecraft.gameRenderer.getMainCamera().position();

        // gets left/right wing parts (these are always symmetrical, but I will eventually try to add EMF support so then we will not know if it will be symmetrical)
        EquipmentElytraModelAccessor elytraAccessor = (EquipmentElytraModelAccessor) (Object) elytraModel;
        ModelPart leftWingPart = elytraAccessor.elytratrails$getLeftWing();
        ModelPart rightWingPart = elytraAccessor.elytratrails$getRightWing();

        float wingOpenness = computeWingOpenness(leftWingPart);

        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(submitPoseMatrix);

        //tip point for each wing (in model space units)
        Vec3 leftWingTipLocal = computeWingTipLocal(leftWingPart, true);
        Vec3 rightWingTipLocal = computeWingTipLocal(rightWingPart, false);

        float localTipXScale = 1.0f;
        if(getConfig().trailMovesWithElytraAngle)
        {
            localTipXScale = Mth.lerp(wingOpenness, 0.33f, 1.0f);
        }

        leftWingTipLocal = new Vec3(leftWingTipLocal.x * localTipXScale, leftWingTipLocal.y, leftWingTipLocal.z);
        rightWingTipLocal = new Vec3(rightWingTipLocal.x * localTipXScale, rightWingTipLocal.y, rightWingTipLocal.z);

        Vec3 leftWingTipCamRelative = transformLocalPointThroughPart(poseStack, leftWingPart, leftWingTipLocal);
        Vec3 rightWingTipCamRelative = transformLocalPointThroughPart(poseStack, rightWingPart, rightWingTipLocal);

        Vec3 leftWingTipWorld = cameraWorldPos.add(leftWingTipCamRelative);
        Vec3 rightWingTipWorld = cameraWorldPos.add(rightWingTipCamRelative);

        long nowNanos = Util.getNanos();

        WingTipPos.put(entityId, leftWingTipWorld, rightWingTipWorld, nowNanos);
    }

}