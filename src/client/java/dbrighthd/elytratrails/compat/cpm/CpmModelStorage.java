//package dbrighthd.elytratrails.compat.cpm;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import dbrighthd.elytratrails.accessor.ModelFeatureStorageAccess;
//import net.minecraft.client.model.Model;
//import net.minecraft.client.model.object.equipment.ElytraModel;
//import net.minecraft.client.renderer.SubmitNodeStorage;
//import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//import net.minecraft.client.renderer.rendertype.RenderType;
//import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//import org.jspecify.annotations.Nullable;
//
//import java.util.List;
//import java.util.Map;
//
//public class CpmModelStorage {
//    public static final ModelFeatureRenderer.Storage modelSubmits = new ModelFeatureRenderer.Storage();
//
//    public static void resetSubmits()
//    {
//        modelSubmits.clear();
//    }
//    public static <S> void submitModel(final Model<? super S> model, final S state, final PoseStack poseStack, final RenderType renderType, final int lightCoords, final int overlayCoords, final int tintedColor, final @Nullable TextureAtlasSprite sprite, final int outlineColor, final ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
//        SubmitNodeStorage.ModelSubmit<S> modelSubmit = new SubmitNodeStorage.ModelSubmit<>(poseStack.last().copy(), model, state, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
//        modelSubmits.add(renderType, modelSubmit);
//    }
//
//    public static SubmitNodeStorage.@Nullable ModelSubmit<?> findCPMElytraModelSubmit() {
//        ModelFeatureStorageAccess accessor = (ModelFeatureStorageAccess) modelSubmits;
//        Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaqueByType =
//                accessor.elytratrails$getSolidModelSubmits();
//        for (List<SubmitNodeStorage.ModelSubmit<?>> submits : opaqueByType.values()) {
//            for (SubmitNodeStorage.ModelSubmit<?> submit : submits) {
//                if (submit.model() instanceof ElytraModel) return submit;
//            }
//        }
//        return null;
//    }
//}
