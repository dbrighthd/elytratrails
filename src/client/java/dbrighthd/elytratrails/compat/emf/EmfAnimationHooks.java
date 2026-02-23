package dbrighthd.elytratrails.compat.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import dbrighthd.elytratrails.compat.ModStatuses;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.models.IEMFModel;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.models.animation.state.EMFEntityRenderStateViaReference;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_model_features.utils.EMFEntity;

public final class EmfAnimationHooks {
    private EmfAnimationHooks() {}

    @SuppressWarnings("deprecation")
    public static @Nullable ModelPart applyManualAnimationAndGetRoot(Model<?> model, Entity entity) {
        if (!ModStatuses.EMF_LOADED || model == null || entity == null) return null;
        if (!(model instanceof IEMFModel emfModel)) return null;

        try {
            if (!emfModel.emf$isEMFModel()) return null;

            EMFModelPartRoot emfRoot = emfModel.emf$getEMFRootModel();
            if (emfRoot == null) return null;

            boolean contextSet = false;

            if (entity instanceof EMFEntity emfEntity) {
                EMFEntityRenderStateViaReference state = new EMFEntityRenderStateViaReference(emfEntity);
                EMFAnimationEntityContext.setCurrentEntityNoIteration(state);
                contextSet = true;
            }

            emfRoot.triggerManualAnimation(new PoseStack());

            if (contextSet) {
                EMFAnimationEntityContext.setCurrentEntityNoIteration(null);
            }

            return emfRoot;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
