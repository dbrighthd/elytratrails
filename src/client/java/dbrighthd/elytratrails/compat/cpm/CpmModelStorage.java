package dbrighthd.elytratrails.compat.cpm;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CpmModelStorage {
    private static final List<ModelFeatureRenderer.Submit<?>> modelSubmits = new ArrayList<>();

    public static void resetSubmits() {
        modelSubmits.clear();
    }

    @SuppressWarnings("unused")
    public static <S> void submitModel(final Model<? super S> model, final S state, final PoseStack poseStack, final RenderType renderType, final int lightCoords, final int overlayCoords, final int tintedColor, final @Nullable TextureAtlasSprite sprite, final int outlineColor, final ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay)
    {
        ModelFeatureRenderer.Submit<S> modelSubmit = new ModelFeatureRenderer.Submit<>(renderType, poseStack.last().copy(), model, state, lightCoords, overlayCoords, tintedColor, sprite,  new PoseStack.Pose());
        modelSubmits.add(modelSubmit);
    }

    public static ModelFeatureRenderer.@Nullable Submit<?> findCPMElytraModelSubmit() {
        for (ModelFeatureRenderer.Submit<?> submit : modelSubmits)
        {
            if (submit.model() instanceof ElytraModel)
            {
                return submit;
            }
        }

        return null;
    }
}