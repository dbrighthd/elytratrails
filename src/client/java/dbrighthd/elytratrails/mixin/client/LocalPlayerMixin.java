package dbrighthd.elytratrails.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Abilities;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

/**
 * Fixes a minecraft bug where you are forced to crouch when fallflying under a ceiling
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Redirect(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;crouching:Z", opcode = Opcodes.PUTFIELD))
    private void injected(LocalPlayer localPlayer, boolean crouching, @Local(name = "abilities") Abilities abilities)
    {
        if(getConfig().fixFallFlyingCrouchBug)
        {
            localPlayer.crouching = !abilities.flying
                    && !localPlayer.isSwimming()
                    && !localPlayer.isPassenger()
                    && (localPlayer.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING))
                    && (localPlayer.isShiftKeyDown() || !localPlayer.isSleeping() && (!(localPlayer.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING))) && !localPlayer.isFallFlying());
        }
        else
        {
            localPlayer.crouching = crouching;
        }
    }
}

