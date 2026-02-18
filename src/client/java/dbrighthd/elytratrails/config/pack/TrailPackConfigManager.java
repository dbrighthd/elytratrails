package dbrighthd.elytratrails.config.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dbrighthd.elytratrails.config.ModConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;


public final class TrailPackConfigManager {
    private TrailPackConfigManager() {}

    @SuppressWarnings("deprecation")
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final String NAMESPACE = "elytratrails";
    private static final String CONFIG_FOLDER = "trail_configs";

    private static final ConcurrentHashMap<String, ModelTrailConfig> MODEL_TRAIL_CONFIGS = new ConcurrentHashMap<>();
    private static double maxLifetimeOverrideSeconds = -1.0;

    public static void clear() {
        MODEL_TRAIL_CONFIGS.clear();
        maxLifetimeOverrideSeconds = -1.0;
    }

    public static void reload(@Nullable ResourceManager resourceManager) {
        clear();
        if (resourceManager == null) return;

        try {
            Map<Identifier, Resource> discoveredResources = resourceManager.listResources(CONFIG_FOLDER, id -> {
                if (!NAMESPACE.equals(id.getNamespace())) return false;

                String normalizedPath = id.getPath().replace('\\', '/');
                if (!normalizedPath.startsWith(CONFIG_FOLDER + "/")) return false;
                if (!normalizedPath.endsWith(".json")) return false;

                String remainder = normalizedPath.substring((CONFIG_FOLDER + "/").length());
                return !remainder.contains("/");
            });

            for (var entry : discoveredResources.entrySet()) {
                Identifier resourceId = entry.getKey();
                Resource resource = entry.getValue();

                String modelKey = modelKeyFromTrailConfigsPath(resourceId.getPath());
                if (modelKey == null) continue;

                loadAndCacheModelConfig(modelKey, resource);
            }
        } catch (Throwable ignored) {
        }
    }

    public static double getMaxLifetimeSeconds(double configDefaultSeconds) {
        if (maxLifetimeOverrideSeconds > 0.0) {
            return Math.max(configDefaultSeconds, maxLifetimeOverrideSeconds);
        }
        return configDefaultSeconds;
    }

    public static ResolvedTrailSettings resolve(@Nullable String modelName, @Nullable String boneName, @Nullable ModConfig baseConfig) {
        if (baseConfig == null) return ResolvedTrailSettings.defaults();

        String normalizedModelKey = normalizeModelKey(modelName);
        ModelTrailConfig modelConfig = (normalizedModelKey == null) ? null : MODEL_TRAIL_CONFIGS.get(normalizedModelKey);

        TrailOverrides mergedOverrides = TrailOverrides.fromBase(baseConfig);

        if (modelConfig != null && modelConfig.defaultOverrides != null) {
            mergedOverrides = mergedOverrides.with(modelConfig.defaultOverrides);
        }

        if (modelConfig != null && boneName != null) {
            String normalizedBoneKey = normalizeBoneKey(boneName);
            if (normalizedBoneKey != null) {
                TrailOverrides boneOverrides = modelConfig.boneOverrides.get(normalizedBoneKey);
                if (boneOverrides != null) {
                    mergedOverrides = mergedOverrides.with(boneOverrides);
                }
            }
        }

        return mergedOverrides.resolve();
    }

    private static @Nullable String modelKeyFromTrailConfigsPath(String rawPath) {
        String normalizedPath = rawPath.replace('\\', '/');
        if (!normalizedPath.startsWith(CONFIG_FOLDER + "/")) return null;
        if (!normalizedPath.endsWith(".json")) return null;

        String fileName = normalizedPath.substring((CONFIG_FOLDER + "/").length());
        if (fileName.contains("/")) return null;

        String withoutExt = fileName.substring(0, fileName.length() - ".json".length());
        return normalizeModelKey(withoutExt);
    }

    private static void loadAndCacheModelConfig(String modelKey, Resource resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            JsonElement rootElement = GSON.fromJson(reader, JsonElement.class);
            if (!(rootElement instanceof JsonObject rootObject)) return;

            ModelTrailConfig parsed = ModelTrailConfig.fromJson(rootObject);
            if (parsed == null) return;

            MODEL_TRAIL_CONFIGS.put(modelKey, parsed);
            maxLifetimeOverrideSeconds = Math.max(maxLifetimeOverrideSeconds, parsed.maxLifetimeSeconds());
        } catch (Throwable ignored) {
        }
    }

    private static @Nullable String normalizeModelKey(@Nullable String rawName) {
        if (rawName == null) return null;

        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) return null;

        String normalized = trimmed.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 1 < normalized.length()) {
            normalized = normalized.substring(lastSlash + 1);
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jem")) normalized = normalized.substring(0, normalized.length() - 4);
        else if (lower.endsWith(".json")) normalized = normalized.substring(0, normalized.length() - 5);

        return normalized.toLowerCase(Locale.ROOT);
    }

    private static @Nullable String normalizeBoneKey(@Nullable String rawBoneName) {
        if (rawBoneName == null) return null;

        String normalized = rawBoneName.replace('\\', '/');

        if (normalized.startsWith("EMF_")) normalized = normalized.substring(4);

        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 1 < normalized.length()) {
            normalized = normalized.substring(lastSlash + 1);
        }

        normalized = normalized.trim();
        if (normalized.isEmpty()) return null;

        return normalized.toLowerCase(Locale.ROOT);
    }

    private record ModelTrailConfig(@Nullable TrailOverrides defaultOverrides,
                                    Map<String, TrailOverrides> boneOverrides) {

        static @Nullable ModelTrailConfig fromJson(JsonObject root) {
                TrailOverrides defaultsFromDefaultsObject = null;
                if (root.has("defaults") && root.get("defaults") instanceof JsonObject defaultsObject) {
                    defaultsFromDefaultsObject = TrailOverrides.fromJson(defaultsObject);
                }

                TrailOverrides defaultsFromTopLevel = TrailOverrides.fromJson(root);

                TrailOverrides mergedDefaults = null;
                if (defaultsFromDefaultsObject != null && !defaultsFromDefaultsObject.isEmpty()) {
                    mergedDefaults = defaultsFromDefaultsObject;
                }
                if (defaultsFromTopLevel != null && !defaultsFromTopLevel.isEmpty()) {
                    mergedDefaults = (mergedDefaults == null) ? defaultsFromTopLevel : mergedDefaults.with(defaultsFromTopLevel);
                }

                Map<String, TrailOverrides> boneOverrides = new ConcurrentHashMap<>();
                if (root.has("bones") && root.get("bones") instanceof JsonObject bonesObject) {
                    for (var entry : bonesObject.entrySet()) {
                        if (!(entry.getValue() instanceof JsonObject boneObject)) continue;

                        TrailOverrides overrides = TrailOverrides.fromJson(boneObject);
                        if (overrides == null || overrides.isEmpty()) continue;

                        String normalizedBoneKey = normalizeBoneKey(entry.getKey());
                        if (normalizedBoneKey != null) {
                            boneOverrides.put(normalizedBoneKey, overrides);
                        }
                    }
                }

                if ((mergedDefaults == null || mergedDefaults.isEmpty()) && boneOverrides.isEmpty()) return null;
                return new ModelTrailConfig(mergedDefaults, boneOverrides);
            }

            double maxLifetimeSeconds() {
                double max = -1.0;

                if (defaultOverrides != null && defaultOverrides.trailLifetime != null) {
                    max = Math.max(max, defaultOverrides.trailLifetime);
                }
                for (TrailOverrides overrides : boneOverrides.values()) {
                    if (overrides != null && overrides.trailLifetime != null) {
                        max = Math.max(max, overrides.trailLifetime);
                    }
                }

                return max;
            }
        }

    private static final class TrailOverrides {
        @Nullable Boolean enableRandomWidth;
        @Nullable Boolean useSplineTrail;
        @Nullable Boolean speedDependentTrail;
        @Nullable Boolean cameraDistanceFade;

        @Nullable Double maxWidth;
        @Nullable Double trailLifetime;
        @Nullable Double trailMinSpeed;
        @Nullable Double startRampDistance;
        @Nullable Double endRampDistance;
        @Nullable Double randomWidthVariation;

        @Nullable String color;

        static TrailOverrides fromBase(ModConfig baseConfig) {
            TrailOverrides overrides = new TrailOverrides();
            overrides.enableRandomWidth = baseConfig.enableRandomWidth;
            overrides.useSplineTrail = baseConfig.useSplineTrail;
            overrides.speedDependentTrail = baseConfig.speedDependentTrail;
            overrides.cameraDistanceFade = baseConfig.cameraDistanceFade;

            overrides.maxWidth = baseConfig.maxWidth;
            overrides.trailLifetime = baseConfig.trailLifetime;
            overrides.trailMinSpeed = baseConfig.trailMinSpeed;
            overrides.startRampDistance = baseConfig.startRampDistance;
            overrides.endRampDistance = baseConfig.endRampDistance;
            overrides.randomWidthVariation = baseConfig.randomWidthVariation;

            overrides.color = baseConfig.color;
            return overrides;
        }

        static @Nullable TrailOverrides fromJson(@Nullable JsonObject json) {
            if (json == null) return null;

            TrailOverrides overrides = new TrailOverrides();

            overrides.maxWidth = readDouble(json, "maxWidth", "maxwidth");
            overrides.trailLifetime = readDouble(json, "trailLifetime", "traillifetime");
            overrides.trailMinSpeed = readDouble(json, "trailMinSpeed", "minspeed");
            overrides.startRampDistance = readDouble(json, "startRampDistance", "startramp");
            overrides.endRampDistance = readDouble(json, "endRampDistance", "endramp");
            overrides.randomWidthVariation = readDouble(json, "randomWidthVariation");

            overrides.enableRandomWidth = readBoolean(json, "enableRandomWidth");
            overrides.useSplineTrail = readBoolean(json, "useSplineTrail");
            overrides.speedDependentTrail = readBoolean(json, "speedDependentTrail", "speeddependant", "speedDependent");
            overrides.cameraDistanceFade = readBoolean(json, "cameraDistanceFade");

            overrides.color = readString(json, "color");

            return overrides;
        }

        boolean isEmpty() {
            return enableRandomWidth == null
                    && useSplineTrail == null
                    && speedDependentTrail == null
                    && cameraDistanceFade == null
                    && maxWidth == null
                    && trailLifetime == null
                    && trailMinSpeed == null
                    && startRampDistance == null
                    && endRampDistance == null
                    && randomWidthVariation == null
                    && color == null;
        }

        TrailOverrides with(@Nullable TrailOverrides other) {
            if (other == null) return this;

            TrailOverrides merged = new TrailOverrides();

            merged.enableRandomWidth = (other.enableRandomWidth != null) ? other.enableRandomWidth : this.enableRandomWidth;
            merged.useSplineTrail = (other.useSplineTrail != null) ? other.useSplineTrail : this.useSplineTrail;
            merged.speedDependentTrail = (other.speedDependentTrail != null) ? other.speedDependentTrail : this.speedDependentTrail;
            merged.cameraDistanceFade = (other.cameraDistanceFade != null) ? other.cameraDistanceFade : this.cameraDistanceFade;

            merged.maxWidth = (other.maxWidth != null) ? other.maxWidth : this.maxWidth;
            merged.trailLifetime = (other.trailLifetime != null) ? other.trailLifetime : this.trailLifetime;
            merged.trailMinSpeed = (other.trailMinSpeed != null) ? other.trailMinSpeed : this.trailMinSpeed;
            merged.startRampDistance = (other.startRampDistance != null) ? other.startRampDistance : this.startRampDistance;
            merged.endRampDistance = (other.endRampDistance != null) ? other.endRampDistance : this.endRampDistance;
            merged.randomWidthVariation = (other.randomWidthVariation != null) ? other.randomWidthVariation : this.randomWidthVariation;

            merged.color = (other.color != null) ? other.color : this.color;

            return merged;
        }

        ResolvedTrailSettings resolve() {
            // This is always merged on top of a base config via fromBase(), so these should be non-null.
            return new ResolvedTrailSettings(
                    asTrue(enableRandomWidth),
                    asTrue(useSplineTrail),
                    asTrue(speedDependentTrail),
                    asTrue(cameraDistanceFade),
                    asNumber(maxWidth),
                    asNumber(trailLifetime),
                    asNumber(trailMinSpeed),
                    asNumber(startRampDistance),
                    asNumber(endRampDistance),
                    asNumber(randomWidthVariation),
                    color
            );
        }

        private static boolean asTrue(@Nullable Boolean value) {
            return value != null && value;
        }

        private static double asNumber(@Nullable Double value) {
            return value != null ? value : 0.0;
        }

        private static @Nullable Double readDouble(JsonObject json, String... keys) {
            for (String key : keys) {
                if (json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isNumber()) {
                    return json.get(key).getAsDouble();
                }
            }
            return null;
        }

        private static @Nullable Boolean readBoolean(JsonObject json, String... keys) {
            for (String key : keys) {
                if (json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isBoolean()) {
                    return json.get(key).getAsBoolean();
                }
            }
            return null;
        }

        private static @Nullable String readString(JsonObject json, String key) {
            if (json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isString()) {
                return json.get(key).getAsString();
            }
            return null;
        }
    }

    public record ResolvedTrailSettings(
            boolean enableRandomWidth,
            boolean useSplineTrail,
            boolean speedDependentTrail,
            boolean cameraDistanceFade,
            double maxWidth,
            double trailLifetime,
            double trailMinSpeed,
            double startRampDistance,
            double endRampDistance,
            double randomWidthVariation,
            @Nullable String color
    ) {
        public static ResolvedTrailSettings defaults() {
            return new ResolvedTrailSettings(
                    getConfig().enableRandomWidth,
                    getConfig().useSplineTrail,
                    getConfig().speedDependentTrail,
                    getConfig().cameraDistanceFade,
                    getConfig().maxWidth,
                    getConfig().trailLifetime,
                    getConfig().trailMinSpeed,
                    getConfig().startRampDistance,
                    getConfig().endRampDistance,
                    getConfig().randomWidthVariation,
                    getConfig().color
            );
        }
    }
}
