package dbrighthd.elytratrails.compat.emf;

import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.animation.AnimSetupContext;
import traben.entity_model_features.models.animation.math.expression_tree.MathValue;
import traben.entity_model_features.models.animation.math.variables.factories.UniqueVariableFactory;
import traben.entity_model_features.models.animation.state.EMFState;

import java.util.Objects;

import static dbrighthd.elytratrails.twirling.TwirlManager.getExtraRollRadiansFromUUID;

public class TwirlAnimFactory extends UniqueVariableFactory {
    @Override
    public MathValue.@Nullable ResultSupplier getSupplierOrNull(String s, AnimSetupContext animSetupContext) {
        return () -> EMFState.state() == null? 0 : getExtraRollRadiansFromUUID(EMFState.state().uuid());
    }

    @Override
    public boolean createsThisVariable(String s) {
        return s.equals("twirl_angle");
    }

    @Override
    public @Nullable String getExplanationTranslationKey() {
        return "text.elytratrails.animation.twirl_angle_explanation";
    }

    @Override
    public @Nullable String getTitleTranslationKey() {
        return "text.elytratrails.animation.twirl_angle";
    }
}
