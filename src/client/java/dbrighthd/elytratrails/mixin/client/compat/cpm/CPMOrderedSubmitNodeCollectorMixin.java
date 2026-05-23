//package dbrighthd.elytratrails.mixin.client.compat.cpm;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.tom.cpm.client.CPMOrderedSubmitNodeCollector;
//import dbrighthd.elytratrails.compat.cpm.CpmModelStorage;
//import net.minecraft.client.model.Model;
//import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//import net.minecraft.client.renderer.rendertype.RenderType;
//import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(CPMOrderedSubmitNodeCollector.class)
//public class CPMOrderedSubmitNodeCollectorMixin {
//    @Inject(
//            method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
//            at = @At("TAIL")
//    )
//    private <S> void elytratrails$onSubmitModelWithTextureAtlasSprite(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int tint, TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci)
//    {
//        CpmModelStorage.submitModel(model, state, pose, type, light, overlay, tint, sprite, outline, crumblingOverlay);
//    }
//
//    @Inject(
//            method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
//            at = @At("TAIL")
//    )
//    private <S> void elytratrails$onSubmitModelNoTextureAtlasSprite(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int outline, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci)
//    {
//        CpmModelStorage.submitModel(model, state, pose, type, light, overlay, 0xFFFFFFFF, null, outline, crumblingOverlay);
//    }
//}
