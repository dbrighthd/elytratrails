package dbrighthd.elytratrails.mixin.client.compat;

import dbrighthd.elytratrails.compat.emf.EmfTrailSpawnerRegistry;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.models.animation.state.EMFState;
import traben.entity_model_features.models.parts.EMFModelPartRoot;

@Mixin(EMFModelPartRoot.class)
public abstract class EmfModelPartRootRegisterMixin {
    @Inject(method = "registerModelRunnableWithEntityTypeContext", at = @At("TAIL"))
    private void elytratrails$onEmfRootRegistered(CallbackInfo ci) {

        var state = EMFState.state();
        if (state == null) return;

        String type = state.typeString();
        EmfTrailSpawnerRegistry.onEmfRootRegistered(type, (ModelPart) (Object) this);
    }
}
