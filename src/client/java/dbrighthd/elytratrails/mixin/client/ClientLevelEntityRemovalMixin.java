package dbrighthd.elytratrails.mixin.client;

import dbrighthd.elytratrails.rendering.TrailSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * when a non-player entity unloads/despawns on the client, immediately break its trail so we never
 * draw a long connecting segment when it comes back (or if the numeric id gets reused).
 */
@Mixin(ClientLevel.class)
public class ClientLevelEntityRemovalMixin {

    @Inject(method = "removeEntity(ILnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"), require = 0)
    private void elytraTrails$onRemoveEntity(int entityId, Entity.RemovalReason reason, CallbackInfo ci) {
        //TrailSystem.getTrailManager().removeTrail(entityId);
        //WingTipSamplerHandler.onEntityRemoved(entityId);
    }
}
