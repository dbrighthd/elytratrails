package dbrighthd.elytratrails.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dbrighthd.elytratrails.controller.EntityTwirlManager;
import dbrighthd.elytratrails.controller.TwirlRoll;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

/**
 * This is what handles the rotations during twirls.
 */
@Mixin(PlayerRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(
            method = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
            at = @At("TAIL")
    )
    private void elytratrails$addSpinRoll(AbstractClientPlayer entityLiving, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        if (!getConfig().enableTwirls) {
            return;
        }
        if (!entityLiving.isFallFlying()) return;

        var mc = Minecraft.getInstance();
        int localId = (mc.player != null) ? mc.player.getId() : Integer.MIN_VALUE;

        float extra;
        if (entityLiving.getId() == localId) {
            //Local player twirling uses its own system
            extra = TwirlRoll.getExtraRollRadians();
        } else {
            //twirling for other entities
            extra = -EntityTwirlManager.getExtraRollRadians(entityLiving.getId());
        }

        if (extra != 0f) {
            poseStack.mulPose(Axis.YP.rotation(extra));
        }
    }
}

