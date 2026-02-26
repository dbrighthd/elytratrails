package dbrighthd.elytratrails.mixin.client;

import dbrighthd.elytratrails.controller.EntityTwirlManager;
import dbrighthd.elytratrails.controller.TwirlRoll;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

//this sucks
@Mixin(Camera.class)
public class CameraTwirlMixin {

    @Shadow @Final private Quaternionf rotation;

    @Shadow private Entity entity;

    @Inject(method = "setRotation(FF)V", at = @At("TAIL"))
    private void twirl_applyExtraRotation(float yRot, float xRot, CallbackInfo ci) {
        if (!getConfig().enableTwirls) return;
        boolean fishysStupidCameraRoll = getConfig().fishysStupidCameraRoll;
        boolean fishysStupidThirdPersonCameraRoll = getConfig().fishysStupidThirdPersonCameraRoll;
        if(!(fishysStupidCameraRoll || fishysStupidThirdPersonCameraRoll))
        {
            return;
        }

        Entity camEntity = this.entity;
        if (!(camEntity instanceof LivingEntity living) || !living.isFallFlying()) return;
        Minecraft mc = Minecraft.getInstance();
        int localId = (mc.player != null) ? mc.player.getId() : Integer.MIN_VALUE;

        boolean isFirstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();

        boolean frontView = (mc.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
        if(isFirstPerson && !fishysStupidCameraRoll)
        {
            return;
        }
        if(!isFirstPerson && !fishysStupidThirdPersonCameraRoll)
        {
            return;
        }
        float extra;
        if (camEntity.getId() == localId) {
            extra = TwirlRoll.getExtraRollRadians();
        } else {
            extra = -EntityTwirlManager.getExtraRollRadians(camEntity.getId());
        }

        if(frontView)
        {
            extra *= -1;
        }
        if (extra != 0f) {
            this.rotation.rotateZ(-extra);

        }
    }
}