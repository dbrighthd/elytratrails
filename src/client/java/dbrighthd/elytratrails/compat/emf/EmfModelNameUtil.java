package dbrighthd.elytratrails.compat.emf;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.models.animation.state.EMFEntityRenderStateViaReference;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_model_features.models.parts.EMFModelPartWithState;
import traben.entity_model_features.utils.EMFEntity;

public final class EmfModelNameUtil {
    private EmfModelNameUtil() {}

    @SuppressWarnings("deprecation")
    public static void ensureVariantStateForEntity(ModelPart emfRoot, @Nullable Entity entity) {
        if (emfRoot instanceof EMFModelPartRoot root) {
            if (!(entity instanceof EMFEntity emfEntity)) return;

            try {
                EMFAnimationEntityContext.setCurrentEntityNoIteration(new EMFEntityRenderStateViaReference(emfEntity));
                root.doVariantCheck();
            } catch (Throwable ignored) {
            } finally {
                try {
                    EMFAnimationEntityContext.setCurrentEntityNoIteration(null);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public static @Nullable String tryGetPackModelName(ModelPart emfRoot, @Nullable String typeStringFallback) {
        @Nullable String modelNameFromRoot = tryGetModelNameFromEmfRoot(emfRoot);
        if (modelNameFromRoot != null) return modelNameFromRoot;

        return sanitizeModelName(typeStringFallback);
    }

    private static @Nullable String tryGetModelNameFromEmfRoot(ModelPart emfRoot) {
        if (emfRoot instanceof EMFModelPartRoot root) {
            try {
                String fileName = root.modelName.getfileName();
                if (fileName == null || fileName.isBlank()) return null;

                String base = stripExtension(getLastPathSegment(fileName));
                if (base == null || base.isBlank()) return null;

                int variant = 1;
                if (emfRoot instanceof EMFModelPartWithState withState) {
                    variant = Math.max(1, withState.currentModelVariant);
                }

                return (variant > 1) ? (base + variant) : base;
            } catch (Throwable ignored) {
                return null;
            }
        } else {
            return null;
        }

    }

    private static @Nullable String sanitizeModelName(@Nullable String raw) {
        if (raw == null) return null;

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        String baseName = stripExtension(getLastPathSegment(trimmed));
        if (baseName == null || baseName.isBlank()) return null;

        return baseName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    static String getLastPathSegment(String path) {
        int lastSlashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return (lastSlashIndex >= 0 && lastSlashIndex + 1 < path.length())
                ? path.substring(lastSlashIndex + 1)
                : path;
    }

    private static @Nullable String stripExtension(@Nullable String name) {
        if (name == null) return null;

        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex <= 0) return name;

        return name.substring(0, lastDotIndex);
    }
}
