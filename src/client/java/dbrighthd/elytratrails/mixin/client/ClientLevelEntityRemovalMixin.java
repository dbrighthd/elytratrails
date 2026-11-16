package dbrighthd.elytratrails.mixin.client;

import dbrighthd.elytratrails.handler.ArrowHandler;
import dbrighthd.elytratrails.rendering.TrailSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dbrighthd.elytratrails.config.ConfigManager.getConfig;

/**
 * Tries to remove entities when the unload?? the client packet listener does this better
 */
@Mixin(ClientLevel.class)
public class ClientLevelEntityRemovalMixin {

    @Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
    private void elytraTrails$onRemoveEntity(int entityId, Entity.RemovalReason reason, CallbackInfo ci) {
        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if(entity instanceof Arrow arrow && getConfig().arrowOvershoot)
        {
            ArrowHandler.addAndCalcFakeArrow(arrow, Minecraft.getInstance().level);
        }
        else
        {
            TrailSystem.getTrailManager().stopTrail(entityId);
        }
    }
}
