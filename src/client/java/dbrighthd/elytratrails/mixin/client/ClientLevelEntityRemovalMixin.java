package dbrighthd.elytratrails.mixin.client;

import dbrighthd.elytratrails.rendering.TrailSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tries to remove entities when the unload?? the client packet listener does this better
 */
@Mixin(ClientLevel.class)
public class ClientLevelEntityRemovalMixin {

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void elytraTrails$onRemoveEntity(int entityId, Entity.RemovalReason reason, CallbackInfo ci) {
        TrailSystem.getTrailManager().removeTrail(entityId);
    }
}
