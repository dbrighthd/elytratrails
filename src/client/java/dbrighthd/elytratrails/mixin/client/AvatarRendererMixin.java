package dbrighthd.elytratrails.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dbrighthd.elytratrails.controller.TwirlController;
import dbrighthd.elytratrails.controller.TwirlRoll;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(
            method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("TAIL")
    )
    private void elytratrails$addSpinRoll(AvatarRenderState state, PoseStack poseStack, float f, float g, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (state.id != mc.player.getId()) return;

        if (!state.isFallFlying) return;

        float extra = TwirlRoll.getExtraRollRadians(g);
        if (extra != 0f) {
            poseStack.mulPose(Axis.YP.rotation(extra)); // (you said you fixed the axis)
        }
    }
}
