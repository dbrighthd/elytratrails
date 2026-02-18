package dbrighthd.elytratrails.compat.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import dbrighthd.elytratrails.mixin.client.ModelFeatureStorageAccessor;
import dbrighthd.elytratrails.trailrendering.WingTipPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.models.IEMFModel;
import traben.entity_model_features.utils.EMFEntity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dbrighthd.elytratrails.util.ModelTransformationUtil.transformPoint;

public final class EmfGenericTrailSampler {
    private EmfGenericTrailSampler() {}

    private static final SubmitNodeStorage SUBMIT_STORAGE = new SubmitNodeStorage();

    public static void sampleNonPlayerEntities(
            Minecraft minecraft,
            EntityRenderDispatcher renderDispatcher,
            CameraRenderState cameraState,
            double cameraX,
            double cameraY,
            double cameraZ,
            float partialTick,
            Vec3 cameraWorldPos,
            long nowNanos
    ) {
        if (minecraft.level == null) return;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof Player) continue;
            if (!(entity instanceof EMFEntity emfEntity)) continue;

            String emfTypeString = emfEntity.emf$getTypeString();
            if (!EmfTrailSpawnerRegistry.typeHasSpawners(emfTypeString)) continue;

            @SuppressWarnings("unchecked")
            EntityRenderer<@NotNull Entity, @NotNull EntityRenderState> renderer =
                    (EntityRenderer<@NotNull Entity, @NotNull EntityRenderState>) renderDispatcher.getRenderer(entity);

            EntityRenderState renderState = renderer.createRenderState();
            renderer.extractRenderState(entity, renderState, partialTick);

            SUBMIT_STORAGE.clear();
            renderDispatcher.submit(renderState, cameraState, cameraX, cameraY, cameraZ, new PoseStack(), SUBMIT_STORAGE);

            List<SubmitNodeStorage.ModelSubmit<?>> modelSubmits = collectAllModelSubmits();
            if (modelSubmits.isEmpty()) continue;

            EmfTrailSpawnerRegistry.TypeDef typeDef = EmfTrailSpawnerRegistry.getOrBuildTypeDef(emfTypeString, modelSubmits);
            if (typeDef == null || typeDef.locators().isEmpty()) continue;

            Map<Model<?>, SubmitNodeStorage.ModelSubmit<?>> submitByModel = indexSubmitsByModel(modelSubmits);

            Vec3 entityOffset = new Vec3(renderState.x, renderState.y, renderState.z);
            int locatorCount = typeDef.locators().size();

            Vec3[] locatorWorldPoints = new Vec3[locatorCount];
            String[] locatorBoneNames = new String[locatorCount];
            @Nullable String packModelName = null;

            for (int locatorIndex = 0; locatorIndex < locatorCount; locatorIndex++) {
                EmfTrailSpawnerRegistry.Locator locator = typeDef.locators().get(locatorIndex);

                SubmitNodeStorage.ModelSubmit<?> modelSubmit = submitByModel.get(locator.poseModel());
                if (modelSubmit == null) continue;

                Model<?> model = modelSubmit.model();
                Object submitState = modelSubmit.state();

                try {
                    @SuppressWarnings("unchecked")
                    Model<Object> typed = (Model<Object>) model;
                    typed.setupAnim(submitState);
                } catch (Throwable ignored) {
                }

                ModelPart emfRoot = resolveEmfRoot(locator, model, entity);
                if (emfRoot == null) continue;

                //ensure EMFs per-entity variant state is up to date before we get the pack model name
                EmfModelNameUtil.ensureVariantStateForEntity(emfRoot, entity);

                if (packModelName == null) {
                    packModelName = EmfModelNameUtil.tryGetPackModelName(emfRoot, emfTypeString);
                }

                locatorBoneNames[locatorIndex] = EmfModelNameUtil.getLastPathSegment(locator.childPath());

                PoseStack poseStack = new PoseStack();
                poseStack.last().set(modelSubmit.pose());

                Vec3 cameraRelativePoint = transformPivotAtChildPath(poseStack, emfRoot, locator.childPath());
                if (cameraRelativePoint == null) continue;

                Vec3 worldPointA = cameraWorldPos.add(cameraRelativePoint);
                Vec3 worldPointB = worldPointA.add(entityOffset);

                Vec3 entityWorldOrigin = cameraWorldPos.add(entityOffset);
                double distA = worldPointA.distanceToSqr(entityWorldOrigin);
                double distB = worldPointB.distanceToSqr(entityWorldOrigin);

                locatorWorldPoints[locatorIndex] = (distA <= distB) ? worldPointA : worldPointB;
            }

            if (containsAny(locatorWorldPoints)) {
                WingTipPos.put(entity.getId(), locatorWorldPoints, nowNanos, packModelName, locatorBoneNames);
            }
        }
    }

    private static Map<Model<?>, SubmitNodeStorage.ModelSubmit<?>> indexSubmitsByModel(List<SubmitNodeStorage.ModelSubmit<?>> submits) {
        Map<Model<?>, SubmitNodeStorage.ModelSubmit<?>> submitByModel = new IdentityHashMap<>();
        for (SubmitNodeStorage.ModelSubmit<?> submit : submits) {
            submitByModel.put(submit.model(), submit);
        }
        return submitByModel;
    }

    private static @Nullable ModelPart resolveEmfRoot(EmfTrailSpawnerRegistry.Locator locator, Model<?> model, Entity entity) {
        if (locator.typeStringForRoot() != null) {
            return EmfTrailSpawnerRegistry.getRoot(locator.typeStringForRoot());
        }

        ModelPart animatedRoot = EmfAnimationHooks.applyManualAnimationAndGetRoot(model, entity);
        if (animatedRoot != null) return animatedRoot;

        if (model instanceof IEMFModel emfModel) {
            return emfModel.emf$getEMFRootModel();
        }

        return null;
    }

    private static boolean containsAny(Vec3[] points) {
        for (Vec3 point : points) {
            if (point != null) return true;
        }
        return false;
    }

    private static List<SubmitNodeStorage.ModelSubmit<?>> collectAllModelSubmits() {
        ArrayList<SubmitNodeStorage.ModelSubmit<?>> collected = new ArrayList<>();

        for (SubmitNodeCollection collection : EmfGenericTrailSampler.SUBMIT_STORAGE.getSubmitsPerOrder().values()) {
            ModelFeatureRenderer.Storage modelStorage = collection.getModelSubmits();
            ModelFeatureStorageAccessor accessor = (ModelFeatureStorageAccessor) modelStorage;

            Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaqueSubmitsByType =
                    accessor.elytratrails$getOpaqueModelSubmits();
            for (List<SubmitNodeStorage.ModelSubmit<?>> opaqueSubmits : opaqueSubmitsByType.values()) {
                collected.addAll(opaqueSubmits);
            }

            List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucentSubmits =
                    accessor.elytratrails$getTranslucentModelSubmits();
            for (SubmitNodeStorage.TranslucentModelSubmit<?> translucent : translucentSubmits) {
                collected.add(translucent.modelSubmit());
            }
        }

        return collected;
    }

    private static @Nullable Vec3 transformPivotAtChildPath(PoseStack poseStack, ModelPart root, String childOnlyPath) {
        if (childOnlyPath == null || childOnlyPath.isEmpty()) return null;

        poseStack.pushPose();
        try {
            root.translateAndRotate(poseStack);

            ModelPart current = root;
            int segmentStartIndex = 0;

            while (segmentStartIndex < childOnlyPath.length()) {
                int nextSlashIndex = childOnlyPath.indexOf('/', segmentStartIndex);
                String segmentName = (nextSlashIndex == -1)
                        ? childOnlyPath.substring(segmentStartIndex)
                        : childOnlyPath.substring(segmentStartIndex, nextSlashIndex);

                ModelPart child = findChildIgnoreCase(current, segmentName);
                if (child == null) return null;

                child.translateAndRotate(poseStack);
                current = child;

                if (nextSlashIndex == -1) break;
                segmentStartIndex = nextSlashIndex + 1;
            }

            return transformPoint(poseStack.last().pose(), new Vec3(0, 0, 0));
        } finally {
            poseStack.popPose();
        }
    }

    private static @Nullable ModelPart findChildIgnoreCase(ModelPart parent, String name) {
        ModelPart direct = parent.children.get(name);
        if (direct != null) return direct;

        for (var entry : parent.children.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
