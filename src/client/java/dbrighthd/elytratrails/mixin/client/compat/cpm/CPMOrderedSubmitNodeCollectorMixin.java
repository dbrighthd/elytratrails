package dbrighthd.elytratrails.mixin.client.compat.cpm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tom.cpm.client.CPMOrderedSubmitNodeCollector;
import dbrighthd.elytratrails.compat.cpm.CpmModelStorage;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CPMOrderedSubmitNodeCollector.class)
public class CPMOrderedSubmitNodeCollectorMixin {
    @Inject(
            method = "submitModel",
            at = @At("TAIL")
    )
    private <S> void elytratrails$onSubmitModelWithTextureAtlasSprite(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable UvMapping uvMapping, int outlineColor, CallbackInfo ci)
    {
        CpmModelStorage.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, uvMapping, outlineColor);
    }
}
