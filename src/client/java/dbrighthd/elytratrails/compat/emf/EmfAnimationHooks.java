
package dbrighthd.elytratrails.compat.emf;

import dbrighthd.elytratrails.compat.ModStatuses;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.IEMFModel;
import traben.entity_model_features.models.animation.state.EMFEntityRenderState;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_texture_features.features.state.ETFState;

import static dbrighthd.elytratrails.ElytraTrails.MOD_ID;
import static dbrighthd.elytratrails.util.ModelTransformationUtil.setupAnyModelAnim;

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
                        ETFState.mount(EMFEntityRenderState.from(renderState));
                        contextSet = true;
                        setupAnyModelAnim(model, renderState);
                        //noinspection UnstableApiUsage
                        emfRoot.animate();
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

    public static void registerAnimationVariables()
    {
        try {
            EMFAnimationApi.registerUniqueAnimationVariableFactory(MOD_ID,"twirl_angle",new TwirlAngleAnimFactory());
            EMFAnimationApi.registerUniqueAnimationVariableFactory(MOD_ID, "twirl_state", new TwirlStateAnimFactory());
        } catch (Exception ignored) {
        }
    }
}
