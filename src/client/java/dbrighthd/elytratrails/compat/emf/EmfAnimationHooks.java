
package dbrighthd.elytratrails.compat.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import dbrighthd.elytratrails.compat.ModStatuses;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.models.IEMFModel;
import traben.entity_model_features.models.animation.state.EMFEntityRenderStateViaReference;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_model_features.utils.EMFEntity;
import traben.entity_texture_features.features.state.ETFState;

public final class EmfAnimationHooks {
    private EmfAnimationHooks() {
    }

    public static @Nullable ModelPart applyManualAnimationAndGetRoot(Model<?> model, Entity entity, EntityRenderState renderState) {
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
                            ETFState.mount(state);
                            contextSet = true;
                        }
                        setupAnimGeneric(model, renderState);
                        emfRoot.triggerManualAnimation(new PoseStack());
                        return emfRoot;
                    } finally {
                        if (contextSet) {
                            ETFState.unMount();
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return root;
    }

    /**
     *  :(
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setupAnimGeneric(
            Model<?> model,
            EntityRenderState state
    ) {
        ((Model) model).setupAnim(state);
    }
}
