package dbrighthd.elytratrails.mixin.client;

import dbrighthd.elytratrails.accessor.ElytraLayerAccessor;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ElytraLayer.class)
public class ElytraLayerAccessorMixin implements ElytraLayerAccessor {
    @Shadow
    private ElytraModel<?> elytraModel;

    @Override
    public ElytraModel<?> elytratrails$getModel() {
        return elytraModel;
    }
}
