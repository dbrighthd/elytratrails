package dbrighthd.elytratrails.config.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.terraformersmc.modmenu.util.mod.Mod;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final ConcurrentHashMap<String, ModelTrailConfig> MODEL_TRAIL_CONFIGS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, TrailOverrides> CONFIG_PRESETS = new ConcurrentHashMap<>();

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

                loadAndCachePresetOverrides(presetKey, resource);
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
        if(preset.trailType != null)
        {
            ClientPlayerConfigStore.TrailRenderSettings trailRenderSettings = ClientPlayerConfigStore.decodeTrailType(preset.trailType);
            config.translucentTrails = trailRenderSettings.translucent();
            config.glowingTrails = trailRenderSettings.glowing();
            config.wireframeTrails = trailRenderSettings.wireframe();
        }
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
        if(preset.trailType != null)
        {
            ClientPlayerConfigStore.TrailRenderSettings trailRenderSettings = ClientPlayerConfigStore.decodeTrailType(preset.trailType);
            config.translucentTrailsOthersDefault = trailRenderSettings.translucent();
            config.glowingTrailsOthersDefault = trailRenderSettings.glowing();
            config.wireframeTrailsOthersDefault = trailRenderSettings.wireframe();
        }
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

    private static void loadAndCachePresetOverrides(String presetKey, Resource resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            JsonElement rootElement = GSON.fromJson(reader, JsonElement.class);
            if (!(rootElement instanceof JsonObject rootObject)) return;

            TrailOverrides overrides = TrailOverrides.fromJson(rootObject);
            if (overrides == null || overrides.isEmpty()) return;

            CONFIG_PRESETS.put(presetKey, overrides);
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

            try (Writer writer = Files.newBufferedWriter(
                    outFile,
                    StandardCharsets.UTF_8
            )) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }

            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String sanitizePresetFileName(String raw) {
        // Replace invalid filename chars on Windows/macOS/Linux-ish cases
        String s = raw.replaceAll("[\\\\/:*?\"<>|]", "_").trim();

        // Avoid weird trailing dots/spaces
        s = s.replaceAll("[\\.\\s]+$", "");

        // Optional: cap length
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

            // Prevent duplicates (resource-pack + disk, or disk + disk)
            CONFIG_PRESETS.putIfAbsent(fallbackName, overrides);
        } catch (Throwable ignored) {
        }
    }


    public static double getMaxLifetimeSeconds(double configDefaultSeconds) {
        if (maxLifetimeOverrideSeconds > 0.0) {
            return Math.max(configDefaultSeconds, maxLifetimeOverrideSeconds);
        }
        return configDefaultSeconds;
    }

    /**
     * ORIGINAL behavior: base overrides come from the "elytra" (non-others) fields.
     */
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

        // If the pack entry existed but it contributed nothing, keep base unchanged.
        // (This is rare, but safe.)
        if (merged.isSameAs(base)) return base;

        return merged.resolve();
    }

    public static ResolvedTrailSettings resolveOthers(@Nullable String modelName, @Nullable String boneName, @Nullable ModConfig baseConfig) {
        if (baseConfig == null) return ResolvedTrailSettings.defaults();

        String normalizedModelKey = normalizeModelKey(modelName);
        ModelTrailConfig modelConfig = (normalizedModelKey == null) ? null : MODEL_TRAIL_CONFIGS.get(normalizedModelKey);

        // If enabled, "others" use the same base defaults as local config.
        TrailOverrides mergedOverrides = baseConfig.useSameDefaultsforOthers
                ? TrailOverrides.fromBase(baseConfig)
                : TrailOverrides.fromOthersDefaults(baseConfig);

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
    private static @Nullable TrailOverrides getPresetOverrides(@Nullable String name) {
        if (name == null) return null;
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) return null;
        return CONFIG_PRESETS.get(key);
    }
    public static final class TrailOverrides {
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
        @Nullable Integer trailType;

        public static TrailOverrides fromBase(ModConfig baseConfig) {
            TrailOverrides overrides = new TrailOverrides();
            overrides.enableTrail = baseConfig.enableTrail;
            overrides.enableRandomWidth = baseConfig.enableRandomWidth;
            overrides.speedDependentTrail = baseConfig.speedDependentTrail;
            overrides.cameraDistanceFade = baseConfig.cameraDistanceFade;

            overrides.maxWidth = baseConfig.width;
            overrides.trailLifetime = baseConfig.trailLifetime;
            overrides.trailMinSpeed = baseConfig.trailMinSpeed;
            overrides.startRampDistance = baseConfig.startRampDistance;
            overrides.endRampDistance = baseConfig.endRampDistance;
            overrides.randomWidthVariation = baseConfig.randomWidthVariation;

            overrides.color = parseHexColor(baseConfig.color);
            overrides.prideTrail = baseConfig.prideTrail;
            overrides.trailType = ClientPlayerConfigStore.encodeTrailType(baseConfig.glowingTrails, baseConfig.translucentTrails, baseConfig.wireframeTrails);
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
            o.trailType = s.trailType();
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

            overrides.color = parseHexColor(readString(json, "color"));
            overrides.prideTrail = readString(json, "prideTrail");
            overrides.fadeStart = readBoolean(json, "fadeStart");
            overrides.fadeStartDistance = readDouble(json,"fadeStartDistance");
            overrides.fadeEnd = readBoolean(json,"fadeEnd");
            overrides.trailType = ClientPlayerConfigStore.encodeTrailType(Boolean.TRUE.equals(readBoolean(json, "glowingTrails")), Boolean.TRUE.equals(readBoolean(json, "translucentTrails")), Boolean.TRUE.equals(readBoolean(json, "wireframeTrails")));
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
                    && prideTrail == null;
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
            return merged;
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
                    color,
                    prideTrail,
                    asTrue(fadeStart),
                    asNumber(fadeStartDistance),
                    asTrue(fadeEnd),
                    trailType
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
            int trailType
    ) {
        public static ResolvedTrailSettings defaults() {
            return new ResolvedTrailSettings(
                    getConfig().enableTrailOthersDefault,
                    getConfig().enableRandomWidthOthersDefault,
                    getConfig().speedDependentTrailOthersDefault,
                    getConfig().cameraDistanceFadeOthersDefault,
                    getConfig().widthOthersDefault,
                    getConfig().trailLifetimeOthersDefault,
                    getConfig().trailMinSpeedOthersDefault,
                    getConfig().startRampDistanceOthersDefault,
                    getConfig().endRampDistanceOthersDefault,
                    getConfig().randomWidthVariationOthersDefault,
                    parseHexColor(getConfig().colorOthersDefault),
                    getConfig().prideTrailOthersDefault,
                    getConfig().fadeStart,
                    getConfig().fadeStartDistance,
                    getConfig().fadeEnd,
                    ClientPlayerConfigStore.encodeTrailType(getConfig().glowingTrails, getConfig().translucentTrails, getConfig().wireframeTrails)
            );
        }
    }
    public static int parseHexColor(String s) {
        if (s == null) return 16777215;
        String t = s.trim();
        if (t.isEmpty()) return 16777215;
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
        return 16777215;
    }
    public static String toHexColorString(int color) {
        // Keep only RGB, ignore alpha
        int rgb = color;
        return String.format("#%06X", rgb);
    }
    public static int getAlpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    /** Returns RGB color only (0xRRGGBB) from ARGB int. */
    public static int getColorRgb(int argb) {
        return argb & 0xFFFFFF;
    }

    /** Combines separate alpha (0-255) + RGB (0xRRGGBB) into ARGB int. */
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
        ResolvedTrailSettings defaults = ResolvedTrailSettings.defaults();
        if (playerConfig == null) return defaults;

        // If PlayerConfig has enableTrail(), use it. Otherwise fall back to defaults.enableTrail().
        boolean enableTrail;
        try {
            enableTrail = playerConfig.enableTrail();
        } catch (Throwable ignored) {
            enableTrail = defaults.enableTrail();
        }

        return new ResolvedTrailSettings(
                enableTrail,
                playerConfig.enableRandomWidth(),  // not in PlayerConfig
                playerConfig.speedDependentTrail(),
                defaults.cameraDistanceFade(),          // not in PlayerConfig
                playerConfig.maxWidth(),
                playerConfig.trailLifetime(),
                playerConfig.trailMinSpeed(),
                playerConfig.startRampDistance(),
                playerConfig.endRampDistance(),
                playerConfig.randomWidthVariation(),
                playerConfig.color(),
                safePride(playerConfig, defaults),
                playerConfig.fadeStart(),
                playerConfig.fadeStartDistance(),
                playerConfig.fadeEnd(),
                playerConfig.trailType()
        );
    }

    private static @Nullable String safePride(PlayerConfig playerConfig, ResolvedTrailSettings defaults) {
        try {
            String v = playerConfig.prideTrail();
            if (v == null) return defaults.prideTrail();
            v = v.trim();
            return v.isEmpty() ? "" : v;
        } catch (Throwable ignored) {
            return defaults.prideTrail();
        }
    }
}
