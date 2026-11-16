package dbrighthd.elytratrails.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @ModifyReturnValue(method = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;isCritArrow()Z", at = @At("RETURN"))
    private boolean stopCrit(boolean original){

        return false;
    }

}
