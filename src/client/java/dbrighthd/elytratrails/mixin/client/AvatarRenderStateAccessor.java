package dbrighthd.elytratrails.mixin.client;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AvatarRenderState.class)
public interface AvatarRenderStateAccessor {
    @Accessor("id")
    int elytratrails$getId();
}
