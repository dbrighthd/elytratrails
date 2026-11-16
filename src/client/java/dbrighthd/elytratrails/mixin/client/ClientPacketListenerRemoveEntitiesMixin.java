package dbrighthd.elytratrails.mixin.client;

import dbrighthd.elytratrails.handler.ArrowHandler;
import dbrighthd.elytratrails.rendering.TrailSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dbrighthd.elytratrails.config.ConfigManager.getConfig;

/**
 * Removes entities when they unload
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerRemoveEntitiesMixin {

    @Inject(method = "handleRemoveEntities", at = @At("HEAD"))
    private void elytraTrails$onRemoveEntities(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {

        for (int id : packet.entityIds()) {
            Entity entity = Minecraft.getInstance().level.getEntity(id);
            if(!(entity instanceof Arrow && getConfig().arrowOvershoot))
            {
                TrailSystem.getTrailManager().stopTrail(id);
            }
        }
    }
}