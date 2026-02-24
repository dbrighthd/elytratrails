package dbrighthd.elytratrails.mixin.client;

import dbrighthd.elytratrails.rendering.TrailSystem;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerRemoveEntitiesMixin {

    @Inject(method = "handleRemoveEntities", at = @At("HEAD"))
    private void elytraTrails$onRemoveEntities(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        for (int id : packet.getEntityIds()) {
            TrailSystem.getTrailManager().removeTrail(id);
        }
    }
}