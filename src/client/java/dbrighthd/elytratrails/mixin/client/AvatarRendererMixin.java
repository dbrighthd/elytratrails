package dbrighthd.elytratrails.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dbrighthd.elytratrails.controller.EntityTwirlManager;
import dbrighthd.elytratrails.controller.TwirlRoll;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(
            method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("TAIL")
    )
    private void elytratrails$addSpinRoll(AvatarRenderState state, PoseStack poseStack, float f, float g, CallbackInfo ci) {
        if (!getConfig().enableTwirls)
        {
            return;
        }
        if (!state.isFallFlying) return;

        var mc = Minecraft.getInstance();
        int localId = (mc.player != null) ? mc.player.getId() : Integer.MIN_VALUE;

        float extra;
        if (state.id == localId) {
            // local: old feel
            extra = TwirlRoll.getExtraRollRadians(g);
        } else {
            // remote: packet-driven per-entity timeline
            extra = -EntityTwirlManager.getExtraRollRadians(state.id, g);
        }

        if (extra != 0f) {
            poseStack.mulPose(Axis.YP.rotation(extra));
        }
    }
}

