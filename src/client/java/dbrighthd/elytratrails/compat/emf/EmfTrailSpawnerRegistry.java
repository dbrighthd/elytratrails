package dbrighthd.elytratrails.compat.emf;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.EMFManager;
import traben.entity_model_features.models.IEMFModel;
import traben.entity_model_features.models.parts.EMFModelPartRoot;

import java.util.*;

public final class EmfTrailSpawnerRegistry {
    private EmfTrailSpawnerRegistry() {}

    private static final boolean EMF_LOADED = FabricLoader.getInstance().isModLoaded("entity_model_features");

    private static volatile boolean initialized = false;

    private static final ObjectOpenHashSet<String> TYPES_WITH_SPAWNERS = new ObjectOpenHashSet<>();
    private static final Object2ObjectOpenHashMap<String, TypeDef> TYPE_DEFS = new Object2ObjectOpenHashMap<>();

    private static final Object2ObjectOpenHashMap<String, ModelPart> REGISTERED_ROOTS_BY_TYPE = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, List<String>> SPAWNER_PATHS_BY_TYPE = new Object2ObjectOpenHashMap<>();

    private static final Comparator<Locator> LOCATOR_ORDER =
            Comparator.comparing((Locator locator) -> System.identityHashCode(locator.poseModel()))
                    .thenComparing(Locator::childPath);

    public record Locator(Model<?> poseModel, @Nullable String typeStringForRoot, String childPath) {
        public static Locator forModelRoot(Model<?> model, String childPath) {
            return new Locator(model, null, childPath);
        }

        public static Locator forRegisteredRoot(Model<?> poseModel, String typeString, String childPath) {
            return new Locator(poseModel, typeString, childPath);
        }
    }

    public record TypeDef(List<Locator> locators) {}

    public static void onResourceReload() {
        if (!EMF_LOADED) return;

        TYPES_WITH_SPAWNERS.clear();
        TYPE_DEFS.clear();
        REGISTERED_ROOTS_BY_TYPE.clear();
        SPAWNER_PATHS_BY_TYPE.clear();
        initialized = false;
    }

    public static void onEmfRootRegistered(String typeString, ModelPart root) {
        if (!EMF_LOADED) return;
        if (typeString == null || root == null) return;

        REGISTERED_ROOTS_BY_TYPE.put(typeString, root);

        List<String> spawnerPaths = findSpawnerPaths(root);
        SPAWNER_PATHS_BY_TYPE.put(typeString, spawnerPaths);

        if (!spawnerPaths.isEmpty()) {
            TYPES_WITH_SPAWNERS.add(typeString);
        }

        TYPE_DEFS.remove(typeString);
    }

    public static @Nullable ModelPart getRoot(String typeString) {
        return typeString == null ? null : REGISTERED_ROOTS_BY_TYPE.get(typeString);
    }

    public static @Nullable List<String> getSpawnerPaths(String typeString) {
        return typeString == null ? null : SPAWNER_PATHS_BY_TYPE.get(typeString);
    }

    public static void rebuildFromEmfManager() {
        if (!EMF_LOADED) return;

        try {
            Map<String, Set<EMFModelPartRoot>> rootsByType =
                    EMFManager.getInstance().rootPartsPerEntityTypeForVariation;

            if (rootsByType == null || rootsByType.isEmpty()) {
                initialized = false;
                return;
            }

            ObjectOpenHashSet<String> typesWithSpawners = new ObjectOpenHashSet<>();

            for (Map.Entry<String, Set<EMFModelPartRoot>> entry : rootsByType.entrySet()) {
                String typeString = entry.getKey();
                if (typeString == null) continue;

                if (anyRootHasSpawners(entry.getValue())) {
                    typesWithSpawners.add(typeString);
                }
            }

            TYPES_WITH_SPAWNERS.clear();
            TYPES_WITH_SPAWNERS.addAll(typesWithSpawners);

            initialized = true;
        } catch (Throwable ignored) {
            initialized = false;
        }
    }

    private static boolean anyRootHasSpawners(Set<EMFModelPartRoot> roots) {
        if (roots == null || roots.isEmpty()) return false;

        for (EMFModelPartRoot root : roots) {
            if (root == null) continue;
            if (!findSpawnerPaths(root).isEmpty()) return true;
        }
        return false;
    }

    private static void ensureInitialized() {
        if (initialized) return;
        rebuildFromEmfManager();
    }

    /**
     * If EMF isn't initialized yet (or failed), this returns true so we don't accidentally
     * disable trail sampling for unknown types.
     */
    public static boolean typeHasSpawners(@Nullable String typeString) {
        ensureInitialized();
        if (!initialized) return true;
        return typeString != null && TYPES_WITH_SPAWNERS.contains(typeString);
    }

    public static @Nullable TypeDef getOrBuildTypeDef(
            @Nullable String typeString,
            @Nullable List<SubmitNodeStorage.ModelSubmit<?>> submits
    ) {
        ensureInitialized();
        if (typeString == null) return null;

        boolean shouldHaveSpawners = TYPES_WITH_SPAWNERS.contains(typeString);

        TypeDef cached = TYPE_DEFS.get(typeString);
        if (cached != null && isStillValid(cached, shouldHaveSpawners, submits)) {
            return cached;
        }
        TYPE_DEFS.remove(typeString);

        if (submits == null || submits.isEmpty()) {
            if (shouldHaveSpawners) return null;

            TypeDef empty = new TypeDef(List.of());
            TYPE_DEFS.put(typeString, empty);
            return empty;
        }

        // Prefer: derive from the models we actually saw in submit list.
        TypeDef fromSubmits = tryBuildTypeDefFromSubmits(typeString, submits);
        if (fromSubmits != null) {
            TYPE_DEFS.put(typeString, fromSubmits);
            return fromSubmits;
        }

        // Fallback: if EMF says this type should have spawners, use its registered root + paths
        // with a best-effort pose model from the submit list.
        if (shouldHaveSpawners) {
            TypeDef fromRegistry = tryBuildTypeDefFromRegistry(typeString, submits);
            if (fromRegistry != null) {
                TYPE_DEFS.put(typeString, fromRegistry);
                return fromRegistry;
            }
            return null;
        }

        TypeDef empty = new TypeDef(List.of());
        TYPE_DEFS.put(typeString, empty);
        return empty;
    }

    private static boolean isStillValid(
            TypeDef cached,
            boolean shouldHaveSpawners,
            @Nullable List<SubmitNodeStorage.ModelSubmit<?>> submits
    ) {
        if (cached.locators().isEmpty()) {
            return !shouldHaveSpawners;
        }
        return containsAnyPoseModel(submits, cached.locators());
    }

    private static boolean containsAnyPoseModel(
            @Nullable List<SubmitNodeStorage.ModelSubmit<?>> submits,
            List<Locator> locators
    ) {
        if (submits == null || submits.isEmpty()) return false;

        for (Locator locator : locators) {
            for (SubmitNodeStorage.ModelSubmit<?> submit : submits) {
                if (submit != null && submit.model() == locator.poseModel()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @Nullable TypeDef tryBuildTypeDefFromSubmits(
            String typeString,
            List<SubmitNodeStorage.ModelSubmit<?>> submits
    ) {
        ArrayList<Locator> locators = new ArrayList<>();

        for (SubmitNodeStorage.ModelSubmit<?> submit : submits) {
            if (submit == null) continue;

            Model<?> model = submit.model();
            if (isElytraModel(model)) continue;
            if (!(model instanceof IEMFModel emfModel)) continue;

            ModelPart root;
            try {
                root = emfModel.emf$getEMFRootModel();
            } catch (Throwable ignored) {
                continue;
            }
            if (root == null) continue;

            List<String> spawnerPaths = findSpawnerPaths(root);
            if (spawnerPaths.isEmpty()) continue;

            for (String path : spawnerPaths) {
                locators.add(Locator.forModelRoot(model, path));
            }
        }

        if (locators.isEmpty()) return null;

        locators.sort(LOCATOR_ORDER);
        return new TypeDef(Collections.unmodifiableList(locators));
    }

    private static @Nullable TypeDef tryBuildTypeDefFromRegistry(
            String typeString,
            List<SubmitNodeStorage.ModelSubmit<?>> submits
    ) {
        ModelPart registeredRoot = getRoot(typeString);
        List<String> registeredPaths = getSpawnerPaths(typeString);
        if (registeredRoot == null || registeredPaths == null || registeredPaths.isEmpty()) return null;

        Model<?> poseModel = chooseNonElytraModel(submits);
        if (poseModel == null) return null;

        ArrayList<Locator> locators = new ArrayList<>();
        for (String path : registeredPaths) {
            locators.add(Locator.forRegisteredRoot(poseModel, typeString, path));
        }

        locators.sort(LOCATOR_ORDER);
        return new TypeDef(Collections.unmodifiableList(locators));
    }

    private static @Nullable Model<?> chooseNonElytraModel(List<SubmitNodeStorage.ModelSubmit<?>> submits) {
        for (SubmitNodeStorage.ModelSubmit<?> submit : submits) {
            if (submit == null) continue;
            Model<?> model = submit.model();
            if (!isElytraModel(model)) return model;
        }
        return null;
    }

    private static boolean isElytraModel(Model<?> model) {
        return model instanceof net.minecraft.client.model.object.equipment.ElytraModel;
    }

    private static List<String> findSpawnerPaths(ModelPart root) {
        if (root == null) return List.of();

        ArrayList<String> foundPaths = new ArrayList<>();
        Deque<PathNode> stack = new ArrayDeque<>();
        stack.push(new PathNode(root, ""));

        while (!stack.isEmpty()) {
            PathNode node = stack.pop();

            for (var entry : node.part.children.entrySet()) {
                String childName = entry.getKey();
                ModelPart childPart = entry.getValue();

                String childPath = node.path.isEmpty() ? childName : (node.path + "/" + childName);

                if (isSpawnerBoneName(childName)) {
                    foundPaths.add(childPath);
                }

                stack.push(new PathNode(childPart, childPath));
            }
        }

        foundPaths.sort(String::compareTo);
        return foundPaths;
    }

    private static boolean isSpawnerBoneName(@Nullable String boneName) {
        if (boneName == null) return false;
        String lower = boneName.toLowerCase(Locale.ROOT);
        return lower.contains("wingtip") || lower.contains("trailspawner");
    }

    private record PathNode(ModelPart part, String path) {}
}
