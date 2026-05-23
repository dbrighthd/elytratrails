package dbrighthd.elytratrails.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dbrighthd.elytratrails.accessor.ElytraLayerAccessor;
import dbrighthd.elytratrails.rendering.TrailSystem;
import dbrighthd.elytratrails.util.FrameCounterUtil;
import dbrighthd.elytratrails.util.ShaderChecksUtil;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dbrighthd.elytratrails.config.ConfigManager.getConfig;
import static dbrighthd.elytratrails.util.ModelTransformationUtil.*;

@Mixin(net.minecraft.client.renderer.entity.layers.ElytraLayer.class)
public class ElytraLayerMixin {

    @Unique
    private static final Int2IntOpenHashMap lastCapturedFrameByEntityId = new Int2IntOpenHashMap();

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getArmorFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Z)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private void elytratrails$captureElytraWingTips(PoseStack poseStack, MultiBufferSource buffer, int packedLight, net.minecraft.world.entity.LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        if (ShaderChecksUtil.isShadowPass()) return;

        ElytraLayerAccessor accessor = (ElytraLayerAccessor) this;
        ElytraModel<?> elytraModel = accessor.elytratrails$getModel();
        EquipmentElytraModelAccessor elytraAccessor = (EquipmentElytraModelAccessor) elytraModel;
        ModelPart leftWingPart = elytraAccessor.elytratrails$getLeftWing();
        ModelPart rightWingPart = elytraAccessor.elytratrails$getRightWing();
        int entityId = entity.getId();
        int currentFrameId = FrameCounterUtil.frameId;
        int lastCapturedFrameId = lastCapturedFrameByEntityId.getOrDefault(entityId, Integer.MIN_VALUE);
        if (lastCapturedFrameId == currentFrameId) return;
        lastCapturedFrameByEntityId.put(entityId, currentFrameId);

        float wingOpenness = computeWingOpenness(leftWingPart);

        Vec3 leftWingTipLocal = computeWingTipLocal(leftWingPart, true);
        Vec3 rightWingTipLocal = computeWingTipLocal(rightWingPart, false);

        float localTipXScale = 1.0f;
        if (getConfig().clientPlayerConfig.trailMovesWithElytraAngle)
        {
            localTipXScale = Mth.lerp(wingOpenness, 0.33f, 1.0f);
        }

        leftWingTipLocal = new Vec3(leftWingTipLocal.x * localTipXScale, leftWingTipLocal.y, leftWingTipLocal.z);
        rightWingTipLocal = new Vec3(rightWingTipLocal.x * localTipXScale, rightWingTipLocal.y, rightWingTipLocal.z);
        Vec3 leftWingTipView = transformLocalPointThroughPart(poseStack, leftWingPart, leftWingTipLocal);
        Vec3 rightWingTipView = transformLocalPointThroughPart(poseStack, rightWingPart, rightWingTipLocal);
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();

        Vec3 leftWingTipWorld = leftWingTipView.add(cameraPos); // I dont need the same pose-stack correction in 1.21.1
        Vec3 rightWingTipWorld = rightWingTipView.add(cameraPos);

        TrailSystem.getWingtipSampler().insertWingTips(entityId, leftWingTipWorld, rightWingTipWorld);
    }
}