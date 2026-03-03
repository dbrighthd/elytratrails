package dbrighthd.elytratrails.config.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

public final class TrailPackConfigManager {
    private TrailPackConfigManager() {}

    @SuppressWarnings("deprecation")
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final String NAMESPACE = "elytratrails";
    private static final String CONFIG_FOLDER = "trail_configs";
    private static final String PRESETS_FOLDER = "config_presets";
    private static final String HIDDEN_PRESETS_FOLDER = "internal_presets";
    private static final Logger LOGGER = LoggerFactory.getLogger(TrailPackConfigManager.class);
    private static final ConcurrentHashMap<String, ModelTrailConfig> MODEL_TRAIL_CONFIGS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, TrailOverrides> CONFIG_PRESETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, TrailOverrides> HIDDEN_CONFIG_PRESETS = new ConcurrentHashMap<>();

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

    public static void reloadPresets(@Nullable ResourceManager resourceManager) {
        CONFIG_PRESETS.clear();
        HIDDEN_CONFIG_PRESETS.clear();

        if (resourceManager == null) return;

        try {
            Map<Identifier, Resource> discoveredResources = resourceManager.listResources(PRESETS_FOLDER, id -> {
                if (!NAMESPACE.equals(id.getNamespace())) return false;

                String normalizedPath = id.getPath().replace('\\', '/');
                if (!normalizedPath.startsWith(PRESETS_FOLDER + "/")) return false;
                if (!normalizedPath.endsWith(".json")) return false;

                String remainder = normalizedPath.substring((PRESETS_FOLDER + "/").length());
                return !remainder.contains("/");
            });

            for (var entry : discoveredResources.entrySet()) {
                Identifier resourceId = entry.getKey();
                Resource resource = entry.getValue();

                String presetKey = presetKeyFromPresetPath(resourceId.getPath());
                if (presetKey == null) continue;

                loadAndCachePresetOverrides(presetKey, resource, false);
            }
            reloadDiskPresets();
        } catch (Throwable ignored) {
        }
        try {
            Map<Identifier, Resource> discoveredResources = resourceManager.listResources(HIDDEN_PRESETS_FOLDER, id -> {
                if (!NAMESPACE.equals(id.getNamespace())) return false;

                String normalizedPath = id.getPath().replace('\\', '/');
                if (!normalizedPath.startsWith(HIDDEN_PRESETS_FOLDER + "/")) return false;
                if (!normalizedPath.endsWith(".json")) return false;

                String remainder = normalizedPath.substring((HIDDEN_PRESETS_FOLDER + "/").length());
                return !remainder.contains("/");
            });

            for (var entry : discoveredResources.entrySet()) {
                Identifier resourceId = entry.getKey();
                Resource resource = entry.getValue();

                String presetKey = hiddenPresetKeyFromPresetPath(resourceId.getPath());
                if (presetKey == null) continue;

                loadAndCachePresetOverrides(presetKey, resource, true);
            }
            reloadDiskPresets();
        } catch (Throwable ignored) {
        }
    }
    public static void applyPreset(boolean self, String presetName, ModConfig modConfig)
    {
        if(!CONFIG_PRESETS.containsKey(presetName))
        {
            return;
        }
        TrailOverrides overrides = CONFIG_PRESETS.get(presetName);
        if(self)
        {
            applyPresetToSelf(overrides, modConfig);
        }
        else
        {
            applyPresetToOthers(overrides,modConfig);
        }

    }

    public static void applyPresetToSelf(TrailOverrides preset, ModConfig config)
    {
        applyIfPresent(preset.glowingTrails, v -> config.glowingTrails = v);
        applyIfPresent(preset.translucentTrails, v -> config.translucentTrails = v);
        applyIfPresent(preset.wireframeTrails, v -> config.wireframeTrails = v);
        applyIfPresent(preset.enableTrail, v -> config.enableTrail = v);
        applyIfPresent(preset.enableRandomWidth, v -> config.enableRandomWidth = v);
        applyIfPresent(preset.speedDependentTrail, v -> config.speedDependentTrail = v);
        applyIfPresent(preset.maxWidth, v -> config.width = v);
        applyIfPresent(preset.trailLifetime, v -> config.trailLifetime = v);
        applyIfPresent(preset.trailMinSpeed, v -> config.trailMinSpeed = v);
        applyIfPresent(preset.startRampDistance, v -> config.startRampDistance = v);
        applyIfPresent(preset.endRampDistance, v -> config.endRampDistance = v);
        applyIfPresent(preset.randomWidthVariation, v -> config.randomWidthVariation = v);
        applyIfPresent(preset.color, v -> config.color = toHexColorString(v));
        applyIfPresent(preset.prideTrail, v -> config.prideTrail = v);
        applyIfPresent(preset.fadeStart, v -> config.fadeStart = v);
        applyIfPresent(preset.fadeStartDistance, v -> config.fadeStartDistance = v);
        applyIfPresent(preset.fadeEnd, v -> config.fadeEnd = v);
    }

    public static void applyPresetToOthers(TrailOverrides preset, ModConfig config)
    {

        applyIfPresent(preset.glowingTrails, v -> config.glowingTrailsOthersDefault = v);
        applyIfPresent(preset.translucentTrails, v -> config.translucentTrailsOthersDefault = v);
        applyIfPresent(preset.wireframeTrails, v -> config.wireframeTrailsOthersDefault = v);
        applyIfPresent(preset.enableTrail, v -> config.enableTrailOthersDefault = v);
        applyIfPresent(preset.enableRandomWidth, v -> config.enableRandomWidthOthersDefault = v);
        applyIfPresent(preset.speedDependentTrail, v -> config.speedDependentTrailOthersDefault = v);
        applyIfPresent(preset.maxWidth, v -> config.widthOthersDefault = v);
        applyIfPresent(preset.trailLifetime, v -> config.trailLifetimeOthersDefault = v);
        applyIfPresent(preset.trailMinSpeed, v -> config.trailMinSpeedOthersDefault = v);
        applyIfPresent(preset.startRampDistance, v -> config.startRampDistanceOthersDefault = v);
        applyIfPresent(preset.endRampDistance, v -> config.endRampDistanceOthersDefault = v);
        applyIfPresent(preset.randomWidthVariation, v -> config.randomWidthVariationOthersDefault = v);
        applyIfPresent(preset.color, v -> config.colorOthersDefault = toHexColorString(v));
        applyIfPresent(preset.prideTrail, v -> config.prideTrailOthersDefault = v);
        applyIfPresent(preset.fadeStart, v -> config.fadeStartOthersDefault = v);
        applyIfPresent(preset.fadeStartDistance, v -> config.fadeStartDistanceOthersDefault = v);
        applyIfPresent(preset.fadeEnd, v -> config.fadeEndOthersDefault = v);
    }

    private static <T> void applyIfPresent(T value, java.util.function.Consumer<T> setter) {
        if (value != null) setter.accept(value);
    }
    private static @Nullable String presetKeyFromPresetPath(String rawPath) {
        String normalizedPath = rawPath.replace('\\', '/');
        if (!normalizedPath.startsWith(PRESETS_FOLDER + "/")) return null;
        if (!normalizedPath.endsWith(".json")) return null;

        String fileName = normalizedPath.substring((PRESETS_FOLDER + "/").length());
        if (fileName.contains("/")) return null;


        String withoutExt = fileName.substring(0, fileName.length() - ".json".length());
        String trimmed = withoutExt.trim();
        if (trimmed.isEmpty()) return null;

        return trimmed.toLowerCase(Locale.ROOT);
    }


    private static @Nullable String hiddenPresetKeyFromPresetPath(String rawPath) {
        String normalizedPath = rawPath.replace('\\', '/');
        if (!normalizedPath.startsWith(HIDDEN_PRESETS_FOLDER + "/")) return null;
        if (!normalizedPath.endsWith(".json")) return null;

        String fileName = normalizedPath.substring((HIDDEN_PRESETS_FOLDER + "/").length());
        if (fileName.contains("/")) return null;


        String withoutExt = fileName.substring(0, fileName.length() - ".json".length());
        String trimmed = withoutExt.trim();
        if (trimmed.isEmpty()) return null;

        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static void loadAndCachePresetOverrides(String presetKey, Resource resource, boolean hidden) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            JsonElement rootElement = GSON.fromJson(reader, JsonElement.class);
            if (!(rootElement instanceof JsonObject rootObject)) return;

            TrailOverrides overrides = TrailOverrides.fromJson(rootObject);
            if (overrides == null || overrides.isEmpty()) return;

            if(!hidden)
            {
                CONFIG_PRESETS.put(presetKey, overrides);
            }
            else
            {
                HIDDEN_CONFIG_PRESETS.put(presetKey, overrides);

            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean exportTrailPresetToDisk(@Nullable String presetName, @Nullable ModConfig config) {
        if (config == null || presetName == null) return false;

        String trimmed = presetName.trim();
        if (trimmed.isEmpty()) return false;

        String safeFileName = sanitizePresetFileName(trimmed);
        if (safeFileName.isEmpty()) return false;

        Path presetDir = FabricLoader.getInstance()
                .getGameDir()
                .resolve("config")
                .resolve("elytratrails_presets");

        Path outFile = presetDir.resolve(safeFileName + ".json");

        try {
            Files.createDirectories(presetDir);

            JsonObject root = new JsonObject();

            root.addProperty("enableTrail", config.enableTrail);
            root.addProperty("glowingTrails", config.glowingTrails);
            root.addProperty("translucentTrails", config.translucentTrails);
            root.addProperty("wireframeTrails", config.wireframeTrails);
            root.addProperty("enableRandomWidth", config.enableRandomWidth);
            root.addProperty("speedDependentTrail", config.speedDependentTrail);
            root.addProperty("fadeFirstPersonTrail", config.fadeFirstPersonTrail);
            root.addProperty("trailMovesWithElytraAngle", config.trailMovesWithElytraAngle);
            root.addProperty("width", config.width);
            root.addProperty("firstPersonFadeTime", config.firstPersonFadeTime);
            root.addProperty("trailLifetime", config.trailLifetime);
            root.addProperty("maxSamplePerSecond", config.maxSamplePerSecond);
            root.addProperty("trailMinSpeed", config.trailMinSpeed);
            root.addProperty("startRampDistance", config.startRampDistance);
            root.addProperty("endRampDistance", config.endRampDistance);

            root.addProperty("color", config.color);

            root.addProperty("prideTrail", config.prideTrail == null ? "" : config.prideTrail);
            root.addProperty("randomWidthVariation", config.randomWidthVariation);
            root.addProperty("fadeStart", config.fadeStart);
            root.addProperty("fadeStartDistance", config.fadeStartDistance);
            root.addProperty("fadeEnd", config.fadeEnd);

            // Overwrite if the file already exists, so people can actually update their presets instead of living with the endless suffering if they accidentally made their trail slightly too thin
            try (Writer writer = Files.newBufferedWriter(
                    outFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }

            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String sanitizePresetFileName(String raw) {
        String s = raw.replaceAll("[\\\\/:*?\"<>|]", "_").trim();

        s = s.replaceAll("[.\\s]+$", "");

        if (s.length() > 100) s = s.substring(0, 100).trim();

        return s;
    }

    public static void reloadDiskPresets() {
        Path presetDir = FabricLoader.getInstance()
                .getGameDir()
                .resolve("config")
                .resolve("elytratrails_presets");

        try {
            Files.createDirectories(presetDir); // okay if already exists
        } catch (IOException ignored) {
            return;
        }

        try (Stream<Path> paths = Files.list(presetDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .forEach(TrailPackConfigManager::loadAndCacheDiskPresetFile);
        } catch (Throwable ignored) {
        }
    }
    private static void loadAndCacheDiskPresetFile(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement rootElement = GSON.fromJson(reader, JsonElement.class);
            if (!(rootElement instanceof JsonObject rootObject)) return;

            TrailOverrides overrides = TrailOverrides.fromJson(rootObject);
            if (overrides == null || overrides.isEmpty()) return;

            String fileName = file.getFileName().toString();
            String fallbackName = fileName.substring(0, fileName.length() - ".json".length()).trim();
            if (fallbackName.isEmpty()) return;

            CONFIG_PRESETS.put(fallbackName, overrides);
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unused")
    public static double getMaxLifetimeSeconds(double configDefaultSeconds) {
        if (maxLifetimeOverrideSeconds > 0.0) {
            return Math.max(configDefaultSeconds, maxLifetimeOverrideSeconds);
        }
        return configDefaultSeconds;
    }

    public static ResolvedTrailSettings resolve(@Nullable String modelName, @Nullable String boneName, @Nullable PlayerConfig baseConfig) {

        if (baseConfig == null) return ResolvedTrailSettings.defaults();
        if (boneName != null)
        {
            boneName = boneName.substring(boneName.lastIndexOf('/')+1).replace("EMF_","");
        }
        ModConfig mainConfig = getConfig();
        String normalizedModelKey = normalizeModelKey(modelName);
        ModelTrailConfig modelConfig = (normalizedModelKey == null) ? null : MODEL_TRAIL_CONFIGS.get(normalizedModelKey);

        TrailOverrides mergedOverrides = TrailOverrides.fromBase(baseConfig);

        if (modelConfig != null && modelConfig.defaultOverrides != null) {
            if(modelConfig.defaultOverrides.parentPreset != null)
            {
                if(mainConfig.logTrails)
                {
                    LOGGER.info("Parent default preset found in model {}, preset name: {}", modelName, modelConfig.defaultOverrides.parentPreset);
                }
                TrailOverrides preset = getPresetOverrides(modelConfig.defaultOverrides.parentPreset);
                mergedOverrides = mergedOverrides.with(preset);
            }
            mergedOverrides = mergedOverrides.with(modelConfig.defaultOverrides);
        }

        if (modelConfig != null && boneName != null) {
            String normalizedBoneKey = normalizeBoneKey(boneName);
            if (normalizedBoneKey != null) {
                TrailOverrides boneOverrides = modelConfig.boneOverrides.get(normalizedBoneKey);
                if (boneOverrides != null) {
                    if(boneOverrides.parentPreset != null)
                    {
                        if(mainConfig.logTrails)
                        {
                            LOGGER.info("Parent preset found for bone {} in model {}, preset name: {}", boneName, modelName, boneOverrides.parentPreset);
                        }
                        TrailOverrides preset = getPresetOverrides(boneOverrides.parentPreset);
                        mergedOverrides = mergedOverrides.with(preset);
                    }
                    mergedOverrides = mergedOverrides.with(boneOverrides);
                }
                else if(mainConfig.logTrails)
                {
                    LOGGER.info("No bone override found for bone {} in model {}, using defaults.", boneName, modelName);
                }
            }
        }


        ResolvedTrailSettings out = mergedOverrides.resolve();
        if(mainConfig.logTrails)
        {
            LOGGER.info("starting trail from model {} on bone {} with configs: {}", modelName, boneName, out);

        }
        return out;
    }

    @SuppressWarnings("unused")
    public static ResolvedTrailSettings resolveOnTop(@Nullable String modelName,
                                                     @Nullable String boneName,
                                                     @Nullable ResolvedTrailSettings base) {
        if (base == null) return ResolvedTrailSettings.defaults();

        String normalizedModelKey = normalizeModelKey(modelName);
        ModelTrailConfig modelConfig = (normalizedModelKey == null) ? null : MODEL_TRAIL_CONFIGS.get(normalizedModelKey);
        if (modelConfig == null) return base;

        // Merge model defaults + bone overrides if present.
        TrailOverrides merged = TrailOverrides.fromResolved(base);

        if (modelConfig.defaultOverrides != null) {
            merged = merged.with(modelConfig.defaultOverrides);
        }

        if (boneName != null) {
            String normalizedBoneKey = normalizeBoneKey(boneName);
            if (normalizedBoneKey != null) {
                TrailOverrides boneOverrides = modelConfig.boneOverrides.get(normalizedBoneKey);
                if (boneOverrides != null) {
                    merged = merged.with(boneOverrides);
                }
            }
        }

        if (merged.isSameAs(base)) return base;

        return merged.resolve();
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
    @SuppressWarnings("unused")
    private static void loadAndCachePresets(String modelKey, Resource resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            JsonElement rootElement = GSON.fromJson(reader, JsonElement.class);
            if (!(rootElement instanceof JsonObject rootObject)) return;

            ModelTrailConfig parsed = ModelTrailConfig.fromJson(rootObject);
            if (parsed == null) return;

            MODEL_TRAIL_CONFIGS.put(modelKey, parsed);
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

    public static Map<String, TrailOverrides> getPresets() {
        return Map.copyOf(CONFIG_PRESETS);
    }

    @SuppressWarnings("unused")

    private static @Nullable TrailOverrides getPresetOverrides(@Nullable String name) {
        if (name == null) return null;
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) return null;
        if(name.startsWith("internal:"))
        {
            return HIDDEN_CONFIG_PRESETS.get(key.substring("internal:".length()));
        }
        return CONFIG_PRESETS.get(key);
    }
    public static final class TrailOverrides {
        @Nullable String parentPreset;
        @Nullable Boolean enableTrail;
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

        @Nullable Integer color;

        @Nullable String prideTrail;

        @Nullable Boolean fadeStart;
        @Nullable Double fadeStartDistance;
        @Nullable Boolean fadeEnd;
        @Nullable Boolean glowingTrails;
        @Nullable Boolean translucentTrails;
        @Nullable Boolean wireframeTrails;

        public static TrailOverrides fromBase(PlayerConfig baseConfig) {
            TrailOverrides overrides = new TrailOverrides();
            overrides.enableTrail = baseConfig.enableTrail();
            overrides.enableRandomWidth = baseConfig.enableRandomWidth();
            overrides.speedDependentTrail = baseConfig.speedDependentTrail();
            overrides.maxWidth = baseConfig.maxWidth();
            overrides.trailLifetime = baseConfig.trailLifetime();
            overrides.trailMinSpeed = baseConfig.trailMinSpeed();
            overrides.startRampDistance = baseConfig.startRampDistance();
            overrides.endRampDistance = baseConfig.endRampDistance();
            overrides.randomWidthVariation = baseConfig.randomWidthVariation();

            overrides.color = baseConfig.color();
            System.out.println("color parsed: " + baseConfig.color());
            overrides.prideTrail = baseConfig.prideTrail();
            ClientPlayerConfigStore.TrailRenderSettings baseTrailRenderSettings = ClientPlayerConfigStore.decodeTrailType(baseConfig.trailType());
            overrides.glowingTrails = baseTrailRenderSettings.glowing();
            overrides.translucentTrails = baseTrailRenderSettings.translucent();
            overrides.wireframeTrails = baseTrailRenderSettings.wireframe();
            overrides.fadeStart = baseConfig.fadeStart();
            overrides.fadeStartDistance = baseConfig.fadeStartDistance();
            overrides.fadeEnd = baseConfig.fadeEnd();
            return overrides;
        }
        public static TrailOverrides fromResolved(ResolvedTrailSettings s) {
            TrailOverrides o = new TrailOverrides();
            // base is "fully resolved", so treat everything as non-null base values
            o.enableTrail = s.enableTrail();
            o.enableRandomWidth = s.enableRandomWidth();
            o.speedDependentTrail = s.speedDependentTrail();
            o.cameraDistanceFade = s.cameraDistanceFade();

            o.maxWidth = s.maxWidth();
            o.trailLifetime = s.trailLifetime();
            o.trailMinSpeed = s.trailMinSpeed();
            o.startRampDistance = s.startRampDistance();
            o.endRampDistance = s.endRampDistance();
            o.randomWidthVariation = s.randomWidthVariation();

            o.color = s.color();
            o.prideTrail = s.prideTrail();
            o.glowingTrails = s.glowingTrails();
            o.translucentTrails = s.translucentTrails();
            o.wireframeTrails = s.wireframeTrails();
            return o;
        }

        /**
         * Used so resolveOnTop can cheaply return base when pack contributes nothing.
         */
        public boolean isSameAs(ResolvedTrailSettings s) {
            return asTrue(enableTrail) == s.enableTrail()
                    && asTrue(enableRandomWidth) == s.enableRandomWidth()
                    && asTrue(speedDependentTrail) == s.speedDependentTrail()
                    && asTrue(cameraDistanceFade) == s.cameraDistanceFade()
                    && asNumber(maxWidth) == s.maxWidth()
                    && asNumber(trailLifetime) == s.trailLifetime()
                    && asNumber(trailMinSpeed) == s.trailMinSpeed()
                    && asNumber(startRampDistance) == s.startRampDistance()
                    && asNumber(endRampDistance) == s.endRampDistance()
                    && asNumber(randomWidthVariation) == s.randomWidthVariation()
                    && (color != null && (color == s.color())
                    && ((prideTrail == null && s.prideTrail() == null) || (prideTrail != null && prideTrail.equals(s.prideTrail()))));
        }

        @SuppressWarnings("unused")
        public static TrailOverrides fromOthersDefaults(ModConfig cfg) {
            TrailOverrides overrides = new TrailOverrides();
            overrides.enableTrail = cfg.enableTrailOthersDefault;
            overrides.enableRandomWidth = cfg.enableRandomWidthOthersDefault;
            overrides.speedDependentTrail = cfg.speedDependentTrailOthersDefault;
            overrides.cameraDistanceFade = cfg.cameraDistanceFadeOthersDefault;

            overrides.maxWidth = cfg.widthOthersDefault;
            overrides.trailLifetime = cfg.trailLifetimeOthersDefault;
            overrides.trailMinSpeed = cfg.trailMinSpeedOthersDefault;
            overrides.startRampDistance = cfg.startRampDistanceOthersDefault;
            overrides.endRampDistance = cfg.endRampDistanceOthersDefault;
            overrides.randomWidthVariation = cfg.randomWidthVariationOthersDefault;

            overrides.color = parseHexColor(cfg.colorOthersDefault);
            overrides.prideTrail = cfg.prideTrailOthersDefault;
            overrides.fadeStart = cfg.fadeStartOthersDefault;
            overrides.fadeStartDistance = cfg.fadeStartDistanceOthersDefault;
            overrides.fadeEnd = cfg.fadeEndOthersDefault;
            return overrides;
        }

        public static @Nullable TrailOverrides fromJson(@Nullable JsonObject json) {
            if (json == null) return null;

            TrailOverrides overrides = new TrailOverrides();
            overrides.parentPreset = readString(json, "parentPreset");
            overrides.maxWidth = readDouble(json, "maxWidth", "maxwidth", "width");
            overrides.trailLifetime = readDouble(json, "trailLifetime", "traillifetime");
            overrides.trailMinSpeed = readDouble(json, "trailMinSpeed", "minspeed");
            overrides.startRampDistance = readDouble(json, "startRampDistance", "startramp");
            overrides.endRampDistance = readDouble(json, "endRampDistance", "endramp");
            overrides.randomWidthVariation = readDouble(json, "randomWidthVariation");

            overrides.enableTrail = readBoolean(json, "enableTrail");
            overrides.enableRandomWidth = readBoolean(json, "enableRandomWidth");
            overrides.useSplineTrail = readBoolean(json, "useSplineTrail");
            overrides.speedDependentTrail = readBoolean(json, "speedDependentTrail", "speeddependant", "speedDependent");
            overrides.cameraDistanceFade = readBoolean(json, "cameraDistanceFade");

            overrides.color = json.has("color") ? parseHexColor(readString(json, "color")) : null;
            overrides.prideTrail = readString(json, "prideTrail");
            overrides.fadeStart = readBoolean(json, "fadeStart");
            overrides.fadeStartDistance = readDouble(json,"fadeStartDistance");
            overrides.fadeEnd = readBoolean(json,"fadeEnd");
            overrides.glowingTrails = readBoolean(json,"glowingTrails");
            overrides.translucentTrails = readBoolean(json,"translucentTrails");
            overrides.wireframeTrails = readBoolean(json,"wireframeTrails");

            return overrides;
        }
        public boolean isEmpty() {
            return enableTrail == null
                    && enableRandomWidth == null
                    && useSplineTrail == null
                    && speedDependentTrail == null
                    && cameraDistanceFade == null
                    && maxWidth == null
                    && trailLifetime == null
                    && trailMinSpeed == null
                    && startRampDistance == null
                    && endRampDistance == null
                    && randomWidthVariation == null
                    && color == null
                    && prideTrail == null
                    && fadeStart == null
                    && fadeEnd == null
                    && fadeStartDistance == null
                    && glowingTrails == null
                    && translucentTrails == null
                    && wireframeTrails == null
                    && parentPreset == null;
        }

        public TrailOverrides with(@Nullable TrailOverrides other) {
            if (other == null) return this;

            TrailOverrides merged = new TrailOverrides();
            merged.enableTrail = (other.enableTrail != null) ? other.enableTrail : this.enableTrail;
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
            merged.prideTrail = (other.prideTrail != null) ? other.prideTrail : this.prideTrail;
            merged.fadeEnd = (other.fadeEnd != null) ? other.fadeEnd : this.fadeEnd;
            merged.fadeStart = (other.fadeStart != null) ? other.fadeStart : this.fadeStart;
            merged.fadeStartDistance = (other.fadeStartDistance != null) ? other.fadeStartDistance : this.fadeStartDistance;
            merged.glowingTrails = (other.glowingTrails != null) ? other.glowingTrails : this.glowingTrails;
            merged.translucentTrails = (other.translucentTrails != null) ? other.translucentTrails : this.translucentTrails;
            merged.wireframeTrails = (other.wireframeTrails != null) ? other.wireframeTrails : this.wireframeTrails;
            return merged;
        }
        @SuppressWarnings("unused")
        public TrailOverrides getPreset() {
            if(parentPreset == null || (!HIDDEN_CONFIG_PRESETS.containsKey(parentPreset) && !CONFIG_PRESETS.containsKey(parentPreset)))
            {
                return null;
            }
            if(HIDDEN_CONFIG_PRESETS.containsKey(parentPreset))
            {
                return  HIDDEN_CONFIG_PRESETS.get(parentPreset);
            }
            return CONFIG_PRESETS.get(parentPreset);
        }
        public ResolvedTrailSettings resolve() {
            return new ResolvedTrailSettings(
                    asTrue(enableTrail),
                    asTrue(enableRandomWidth),
                    asTrue(speedDependentTrail),
                    asTrue(cameraDistanceFade),
                    asNumber(maxWidth),
                    asNumber(trailLifetime),
                    asNumber(trailMinSpeed),
                    asNumber(startRampDistance),
                    asNumber(endRampDistance),
                    asNumber(randomWidthVariation),
                    color != null ? color : 0xFFFFFFFF,
                    prideTrail,
                    asTrue(fadeStart),
                    asNumber(fadeStartDistance),
                    asTrue(fadeEnd),
                    asTrue(glowingTrails),
                    asTrue(translucentTrails),
                    asTrue(wireframeTrails)
            );
        }

        public  static boolean asTrue(@Nullable Boolean value) {
            return value != null && value;
        }

        public  static double asNumber(@Nullable Double value) {
            return value != null ? value : 0.0;
        }

        public  static @Nullable Double readDouble(JsonObject json, String... keys) {
            for (String key : keys) {
                if (json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isNumber()) {
                    return json.get(key).getAsDouble();
                }
            }
            return null;
        }

        public  static @Nullable Boolean readBoolean(JsonObject json, String... keys) {
            for (String key : keys) {
                if (json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isBoolean()) {
                    return json.get(key).getAsBoolean();
                }
            }
            return null;
        }

        public  static @Nullable String readString(JsonObject json, String key) {
            if (json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isString()) {
                return json.get(key).getAsString();
            }
            return null;
        }
    }

    public record ResolvedTrailSettings(
            boolean enableTrail,
            boolean enableRandomWidth,
            boolean speedDependentTrail,
            boolean cameraDistanceFade,
            double maxWidth,
            double trailLifetime,
            double trailMinSpeed,
            double startRampDistance,
            double endRampDistance,
            double randomWidthVariation,
            int color,
            @Nullable String prideTrail,
            boolean fadeStart,
            double fadeStartDistance,
            boolean fadeEnd,
            boolean glowingTrails,
            boolean translucentTrails,
            boolean wireframeTrails
    ) {
        public static ResolvedTrailSettings defaults() {
            return resolveFromPlayerConfig(ClientPlayerConfigStore.getLocalPlayerConfig());
        }
    }
    public static int parseHexColor(String s) {
        if (s == null) return 0xFFFFFFFF;
        String t = s.trim();
        if (t.isEmpty()) return 0xFFFFFFFF;
        if (t.startsWith("#")) t = t.substring(1);
        if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);

        try {
            if (t.length() == 6) {
                int rgb = Integer.parseUnsignedInt(t, 16);
                return 0xFF000000 | rgb;
            }
            if (t.length() == 8) {
                return (int) Long.parseLong(t, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return 0xFFFFFFFF;
    }
    public static String toHexColorString(int color) {
        return String.format("#%06X", color);
    }
    public static int getAlpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    public static int getColorRgb(int argb) {
        return argb & 0xFFFFFF;
    }

    @SuppressWarnings("unused")
    public static int withAlphaAndColor(int alpha, int rgb) {
        int a = alpha & 0xFF;
        int c = rgb & 0xFFFFFF;
        return (a << 24) | c;
    }
    public static String withAlphaAndColorToHexString(int alpha, int rgb) {
        int a = alpha & 0xFF;
        int c = rgb & 0xFFFFFF;
        int argb = (a << 24) | c;
        return String.format("#%08X", argb); // #AARRGGBB
    }
    public static ResolvedTrailSettings resolveFromPlayerConfig(@Nullable PlayerConfig playerConfig) {
        assert playerConfig != null;
        ClientPlayerConfigStore.TrailRenderSettings trailRenderSettings = ClientPlayerConfigStore.decodeTrailType(playerConfig.trailType());
        return new ResolvedTrailSettings(
                playerConfig.enableTrail(),
                playerConfig.enableRandomWidth(),
                playerConfig.speedDependentTrail(),
                false,       // not in PlayerConfig
                playerConfig.maxWidth(),
                playerConfig.trailLifetime(),
                playerConfig.trailMinSpeed(),
                playerConfig.startRampDistance(),
                playerConfig.endRampDistance(),
                playerConfig.randomWidthVariation(),
                playerConfig.color(),
                safePride(playerConfig),
                playerConfig.fadeStart(),
                playerConfig.fadeStartDistance(),
                playerConfig.fadeEnd(),
                trailRenderSettings.glowing(),
                trailRenderSettings.translucent(),
                trailRenderSettings.wireframe()
        );
    }

    private static String safePride(PlayerConfig playerConfig) {
        try {
            String v = playerConfig.prideTrail();
            if (v == null) return "trail";
            v = v.trim();
            return v.isEmpty() ? "" : v;
        } catch (Throwable ignored) {
            return "trail";
        }
    }
}
