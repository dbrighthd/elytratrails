package dbrighthd.elytratrails.config.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dbrighthd.elytratrails.config.ClientConfig;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.network.PlayerConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
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
    private static final ConcurrentHashMap<EntityType<?>, ResolvedSampleSettings> entityDefaults = new ConcurrentHashMap<>();
    private static ResolvedSampleSettings defaultResolved;
    public static final Set<EntityType<?>> entitiesWithTrails = new HashSet<>();
    public static final Set<EntityType<?>> entitiesWithTrailOverrides = new HashSet<>();
    private static double maxLifetimeOverrideSeconds = -1.0;

    public static void clear() {
        MODEL_TRAIL_CONFIGS.clear();
        maxLifetimeOverrideSeconds = -1.0;
    }
    public static List<String> getModelStrings()
    {
        return Collections.list(MODEL_TRAIL_CONFIGS.keys());
    }
    public static ResolvedSampleSettings getDefaultEntitySettings(Entity entity)
    {
        return entityDefaults.getOrDefault(entity.getType(),defaultResolved);
    }
    public static void setDefaultEntitySettingsOverrides()
    {
        for(EntityType<?> entityType : entitiesWithTrailOverrides)
        {
            entityDefaults.put(entityType, resolveSample(entityType.toShortString()));
        }
        defaultResolved = ResolvedSampleSettings.defaults();
    }
    public static void setEntityDefaultModel(EntityType<?> entityType)
    {
            entityDefaults.put(entityType, resolveSample(entityType.toShortString()));
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean doesEntityHaveEmfTrails(Entity entity)
    {
        return entitiesWithTrails.contains(entity.getType());
    }
    public static boolean doesEntityHaveOverrides(Entity entity)
    {
        return entitiesWithTrailOverrides.contains(entity.getType());
    }
    public static void reload(@Nullable ResourceManager resourceManager) {
        clear();
        entitiesWithTrailOverrides.clear();
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

                EntityType.byString(sanitizeModelKey(modelKey)).ifPresent(entitiesWithTrailOverrides::add);
                loadAndCacheModelConfig(modelKey, resource);
                setDefaultEntitySettingsOverrides();
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
            applyPresetToConfig(overrides, modConfig.clientPlayerConfig);
        }
        else
        {
            applyPresetToConfig(overrides,modConfig.otherPlayerConfig);
        }
    }
    public static void applyPresetToConfig(TrailOverrides preset, ClientConfig config)
    {
        try {
            Map<String, Field> configFields = new HashMap<>();
            for (Field field : ClientConfig.class.getDeclaredFields())
            {
                field.setAccessible(true);
                configFields.put(field.getName(), field);
            }

            JsonObject values = preset.values();

            for (var entry : values.entrySet())
            {
                Field configField = configFields.get(entry.getKey());
                if (configField == null)
                {
                    continue;
                }

                JsonElement element = entry.getValue();
                if (element == null || !element.isJsonPrimitive()) {
                    continue;
                }

                Class<?> type = configField.getType(); //I got tired of changing the long apply preset class, so I made it do it dynamically

                if (type == boolean.class) {
                    if (element.getAsJsonPrimitive().isBoolean()) {
                        configField.setBoolean(config, element.getAsBoolean());
                    }
                } else if (type == double.class) {
                    if (element.getAsJsonPrimitive().isNumber()) {
                        configField.setDouble(config, element.getAsDouble());
                    }
                } else if (type == int.class) { //for manually-made presets, sometimes people write the hex string instead of the int so this checks for that
                    if (element.getAsJsonPrimitive().isNumber()) {
                        configField.setInt(config, element.getAsInt());
                    } else if (element.getAsJsonPrimitive().isString()) {
                        configField.setInt(config, parseHexColor(element.getAsString()));
                    }
                } else if (type == String.class) {
                    if (element.getAsJsonPrimitive().isString()) {
                        configField.set(config, element.getAsString());
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to apply preset to config", e);
        }
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
            if (overrides.isEmpty()) return;

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

    public static void exportTrailPresetToDisk(@Nullable String presetName, @Nullable ModConfig config) {
        if (config == null || presetName == null) return;

        String trimmed = presetName.trim();
        if (trimmed.isEmpty()) return;

        String safeFileName = sanitizePresetFileName(trimmed);
        if (safeFileName.isEmpty()) return;

        Path presetDir = FabricLoader.getInstance()
                .getGameDir()
                .resolve("config")
                .resolve("elytratrails_presets");

        Path outFile = presetDir.resolve(safeFileName + ".json");

        try {
            Files.createDirectories(presetDir);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject root = gson.toJsonTree(config.clientPlayerConfig).getAsJsonObject();

            root.remove("easeType");
            root.remove("twirlTime");
            // Overwrite if the file already exists, so people can actually update their presets instead of living with the endless suffering if they accidentally made their trail slightly too thin
            try (Writer writer = Files.newBufferedWriter(
                    outFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                gson.toJson(root, writer);
            }

        } catch (IOException ignored) {
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
            if (overrides.isEmpty()) return;

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
    public static ResolvedSampleSettings resolveSample(@Nullable String entityName) {
        String normalizedModelKey = normalizeModelKey(entityName);
        ModelTrailConfig modelConfig = (normalizedModelKey == null) ? null : MODEL_TRAIL_CONFIGS.get(normalizedModelKey);
        if (modelConfig != null && modelConfig.defaultOverrides != null) {
            if(modelConfig.defaultOverrides.values().has("parentPreset"))
            {
                TrailOverrides merged = modelConfig.defaultOverrides.with(getPresetOverrides(modelConfig.defaultOverrides.getString("parentPreset")));
                return merged.resolvedSampleSettings();
            }
            return modelConfig.defaultOverrides.resolvedSampleSettings();
        }
        return ResolvedSampleSettings.defaults();
    }
    public static ResolvedTrailSettings resolve(@Nullable String modelName, @Nullable String boneName, @Nullable PlayerConfig baseConfig, boolean isLeftWing) {
        if (baseConfig == null) return ResolvedTrailSettings.defaults(isLeftWing);
        if (boneName != null)
        {
            boneName = boneName.substring(boneName.lastIndexOf('/')+1).replace("EMF_","");
        }
        ModConfig mainConfig = getConfig();
        String normalizedModelKey = normalizeModelKey(modelName);
        ModelTrailConfig modelConfig = (normalizedModelKey == null) ? null : MODEL_TRAIL_CONFIGS.get(normalizedModelKey);
        TrailOverrides mergedOverrides = TrailOverrides.fromBase(baseConfig);
        if(!mainConfig.resourcePackOverride && (modelName != null && modelName.contains("elytra")))
        {
            return mergedOverrides.resolvedTrailSettings(isLeftWing);
        }
        if (modelConfig != null && modelConfig.defaultOverrides != null) {
            String parentPreset = modelConfig.defaultOverrides.getString("parentPreset");
            if(parentPreset != null)
            {
                if(mainConfig.logTrails)
                {
                    LOGGER.info("Parent default preset found in model {}, preset name: {}", modelName, parentPreset);
                }
                TrailOverrides preset = getPresetOverrides(parentPreset);
                mergedOverrides = mergedOverrides.with(preset);
            }
            mergedOverrides = mergedOverrides.with(modelConfig.defaultOverrides);
        }

        if (modelConfig != null && boneName != null) {
            String normalizedBoneKey = normalizeBoneKey(boneName);
            if (normalizedBoneKey != null) {
                TrailOverrides boneOverrides = modelConfig.boneOverrides.get(normalizedBoneKey);
                if (boneOverrides != null) {
                    String parentPreset = boneOverrides.getString("parentPreset");
                    if (parentPreset != null) {
                        if (mainConfig.logTrails) {
                            LOGGER.info("Parent preset found for bone {} in model {}, preset name: {}", boneName, modelName, parentPreset);
                        }
                        TrailOverrides preset = getPresetOverrides(parentPreset);
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


        ResolvedTrailSettings out = mergedOverrides.resolvedTrailSettings(isLeftWing);
        if(mainConfig.logTrails)
        {
            LOGGER.info("starting trail from model {} on bone {} with configs: {}", modelName, boneName, out);

        }
        return out;
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
    @SuppressWarnings("unused")
    public static String sanitizeModelKey(String modelKey)
    {
        return modelKey.replaceAll("[0-9]","");
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
            if (!defaultsFromTopLevel.isEmpty()) {
                mergedDefaults = (mergedDefaults == null) ? defaultsFromTopLevel : mergedDefaults.with(defaultsFromTopLevel);
            }

            Map<String, TrailOverrides> boneOverrides = new ConcurrentHashMap<>();
            if (root.has("bones") && root.get("bones") instanceof JsonObject bonesObject) {
                for (var entry : bonesObject.entrySet()) {
                    if (!(entry.getValue() instanceof JsonObject boneObject)) continue;

                    TrailOverrides overrides = TrailOverrides.fromJson(boneObject);
                    if (overrides.isEmpty()) continue;

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

            if (defaultOverrides != null) {
                Double lifetime = defaultOverrides.getDouble("trailLifetime");
                max = Math.max(max, lifetime);
            }
            for (TrailOverrides overrides : boneOverrides.values()) {
                if (overrides != null) {
                    Double lifetime = overrides.getDouble("trailLifetime");
                    max = Math.max(max, lifetime);
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public static String withAlphaAndColorToHexString(int alpha, int rgb) {
        int a = alpha & 0xFF;
        int c = rgb & 0xFFFFFF;
        int argb = (a << 24) | c;
        return String.format("#%08X", argb); // #AARRGGBB
    }
    public static ResolvedTrailSettings resolveFromPlayerConfig(PlayerConfig playerConfig) {
        return new ResolvedTrailSettings(
                playerConfig.enableTrail(),
                playerConfig.enableRandomWidth(),
                playerConfig.speedDependentTrail(),
                playerConfig.maxWidth(),
                playerConfig.trailLifetime(),
                playerConfig.trailMinSpeed(),
                playerConfig.startRampDistance(),
                playerConfig.endRampDistance(),
                playerConfig.randomWidthVariation(),
                playerConfig.color(),
                safePride(playerConfig.prideTrail()),
                playerConfig.fadeStart(),
                playerConfig.fadeStartDistance(),
                playerConfig.fadeEnd(),
                playerConfig.glowingTrails(),
                playerConfig.translucentTrails(),
                playerConfig.wireframeTrails(),
                playerConfig.alwaysShowTrailDuringTwirl(),
                playerConfig.increaseWidthOverTime(),
                playerConfig.startingWidthMultiplier(),
                playerConfig.endingWidthMultiplier(),
                playerConfig.distanceTillTrailStart(),
                playerConfig.endDistanceFade(),
                playerConfig.endDistanceFadeAmount(),
                playerConfig.speedBasedAlpha(),
                playerConfig.minAlphaSpeed(),
                playerConfig.maxAlphaSpeed(),
                playerConfig.speedBasedWidth(),
                playerConfig.minWidthSpeed(),
                playerConfig.maxWidthSpeed()
        );
    }
    public static ResolvedTrailSettings resolveTrailFromPlayerConfig(PlayerConfig playerConfig, boolean isLeftWing) {
        return new ResolvedTrailSettings(
                playerConfig.enableTrail(),
                playerConfig.enableRandomWidth(),
                playerConfig.speedDependentTrail(),
                playerConfig.maxWidth(),
                playerConfig.trailLifetime(),
                playerConfig.trailMinSpeed(),
                playerConfig.startRampDistance(),
                playerConfig.endRampDistance(),
                playerConfig.randomWidthVariation(),
                getTrailColor(playerConfig,isLeftWing),
                getTexture(playerConfig,isLeftWing),
                playerConfig.fadeStart(),
                playerConfig.fadeStartDistance(),
                playerConfig.fadeEnd(),
                playerConfig.glowingTrails(),
                playerConfig.translucentTrails(),
                playerConfig.wireframeTrails(),
                playerConfig.alwaysShowTrailDuringTwirl(),
                playerConfig.increaseWidthOverTime(),
                playerConfig.startingWidthMultiplier(),
                playerConfig.endingWidthMultiplier(),
                playerConfig.distanceTillTrailStart(),
                playerConfig.endDistanceFade(),
                playerConfig.endDistanceFadeAmount(),
                playerConfig.speedBasedAlpha(),
                playerConfig.minAlphaSpeed(),
                playerConfig.maxAlphaSpeed(),
                playerConfig.speedBasedWidth(),
                playerConfig.minWidthSpeed(),
                playerConfig.maxWidthSpeed()
        );
    }
    private static String getTexture(PlayerConfig config, boolean isLeftWing) {
        if(!isLeftWing && (!(config.prideTrailRight() == null || config.prideTrailRight().isEmpty())))
        {
            return safePride(config.prideTrailRight());
        }
        return safePride(config.prideTrail());
    }

    private static int getTrailColor(PlayerConfig config, boolean isLeftWing)
    {
        return config.useColorBoth() ? config.color() : (isLeftWing ? config.color() : config.colorRight());
    }

    private static String safePride(String prideTrail) {
        try {
            String v = prideTrail;
            if (v == null) return "trail";
            v = v.trim();
            return v.isEmpty() ? "" : v;
        } catch (Throwable ignored) {
            return "trail";
        }
    }
}