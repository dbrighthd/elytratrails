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
        if (model == null || entity == null) return null;

        ModelPart root = model.root();

        if (ModStatuses.EMF_LOADED) {
            try {
                EMFModelPartRoot emfRoot = null;

                if (model instanceof IEMFModel emfModel && emfModel.emf$isEMFModel()) {
                    emfRoot = emfModel.emf$getEMFRootModel();
                } else if (root instanceof EMFModelPartRoot castRoot) {
                    emfRoot = castRoot;
                }

                if (emfRoot != null) {
                    boolean contextSet = false;
                    try {
                        if (entity instanceof EMFEntity emfEntity) {
                            EMFEntityRenderStateViaReference state = new EMFEntityRenderStateViaReference(emfEntity);
                            EMFAnimationEntityContext.setCurrentEntityNoIteration(state);
                            contextSet = true;
                        }

                        emfRoot.triggerManualAnimation(new PoseStack());
                        return emfRoot;
                    } finally {
                        if (contextSet) {
                            EMFAnimationEntityContext.setCurrentEntityNoIteration(null);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return root;
    }
}
