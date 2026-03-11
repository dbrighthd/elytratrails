package dbrighthd.elytratrails.config.pack;

import com.google.gson.JsonObject;
import dbrighthd.elytratrails.network.PlayerConfig;
import org.jetbrains.annotations.Nullable;

import static dbrighthd.elytratrails.config.pack.TrailPackConfigManager.parseHexColor;

/**
 * TrailOverrides store a json object that can have any or none of the properties in ResolvedTrailSettings. exists to have stuff override eachotheer,
 * useful for presets and also resource packs.
 *
 */
public record TrailOverrides(JsonObject values) {
    public TrailOverrides(@Nullable JsonObject values) {
        this.values = values == null ? new JsonObject() : values.deepCopy();
    }

    /**
     * I know reflection is slow but i kept forgetting to add stuff from PlayerConfig to every single usage of TrailOverrides so this was easier for expandablity
     */
    public static TrailOverrides fromBase(PlayerConfig baseConfig) {
        JsonObject json = new JsonObject();

        try
        {
            for (java.lang.reflect.RecordComponent component : PlayerConfig.class.getRecordComponents())
            {
                String name = component.getName();
                Object value = component.getAccessor().invoke(baseConfig);
                switch (value) {
                    case Boolean b -> json.addProperty(name, b);
                    case Number n -> json.addProperty(name, n);
                    case Character c -> json.addProperty(name, c);
                    case String s -> json.addProperty(name, s);
                    case Enum<?> e -> json.addProperty(name, e.name());
                    case null, default -> {}
                }

            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to copy PlayerConfig into TrailOverrides", e);
        }

        return new TrailOverrides(json);
    }

    public static @Nullable TrailOverrides fromJson(@Nullable JsonObject json) {
        if (json == null) return null;
        return new TrailOverrides(json);
    }

    @SuppressWarnings("unused")
    public boolean has(String key) {
        return values.has(key);
    }

    public Boolean getBoolean(String key) {
        if (!values.has(key) || !values.get(key).isJsonPrimitive() || !values.get(key).getAsJsonPrimitive().isBoolean()) {
            return false;
        }
        return values.get(key).getAsBoolean();
    }

    public Double getDouble(String key) {
        if (!values.has(key) || !values.get(key).isJsonPrimitive() || !values.get(key).getAsJsonPrimitive().isNumber()) {
            return 0.0;
        }
        return values.get(key).getAsDouble();
    }

    public @Nullable String getString(String key) {
        if (!values.has(key) || !values.get(key).isJsonPrimitive() || !values.get(key).getAsJsonPrimitive().isString()) {
            return "";
        }
        return values.get(key).getAsString();
    }

    @Override
    public JsonObject values() {
        return values.deepCopy();
    }

    public boolean isEmpty() {
        return values.entrySet().isEmpty();
    }

    public TrailOverrides with(@Nullable TrailOverrides other) {
        if (other == null) return this;

        JsonObject merged = this.values.deepCopy();
        JsonObject otherValues = other.values;
        for (String key : otherValues.keySet()) {
            merged.add(key, otherValues.get(key));
        }
        return new TrailOverrides(merged);
    }

    public ResolvedTrailSettings resolvedTrailSettings(boolean isLeftWing) {
        return new ResolvedTrailSettings(
                getBoolean("enableTrail"),
                getBoolean("enableRandomWidth"),
                getBoolean("speedDependentTrail"),
                getDouble("maxWidth"),
                getDouble("trailLifetime"),
                getDouble("trailMinSpeed"),
                getDouble("startRampDistance"),
                getDouble("endRampDistance"),
                getDouble("randomWidthVariation"),
                getTrailColor(isLeftWing),
                getTexture(isLeftWing),
                getBoolean("fadeStart"),
                getDouble("fadeStartDistance"),
                getBoolean("lifeTimeFade"),
                getBoolean("glowingTrails"),
                getBoolean("translucentTrails"),
                getBoolean("wireframeTrails"),
                getBoolean("alwaysShowTrailDuringTwirl"),
                getBoolean("increaseWidthOverTime"),
                getDouble("startingWidthMultiplier"),
                getDouble("endingWidthMultiplier"),
                getDouble("distanceTillTrailStart"),
                getBoolean("endDistanceFade"),
                getDouble("endDistanceFadeAmount"),
                getBoolean("speedBasedAlpha"),
                getDouble("minAlphaSpeed"),
                getDouble("maxAlphaSpeed"),
                getBoolean("speedBasedWidth"),
                getDouble("minWidthSpeed"),
                getDouble("maxWidthSpeed"),
                getDouble("distanceTillTrailEnd")
                );
    }
    public ResolvedSampleSettings resolvedSampleSettings() {
        return new ResolvedSampleSettings(
                getBoolean("speedDependentTrail"),
                getDouble("trailMinSpeed"),
                getDouble("xOffset"),
                getDouble("yOffset"),
                getDouble("zOffset"));
    }

    private int resolveColor(String key) {
        if (!values.has(key) || !values.get(key).isJsonPrimitive()) {
            return -1;//white
        }

        if (values.get(key).getAsJsonPrimitive().isNumber()) {
            return values.get(key).getAsInt();
        }

        if (values.get(key).getAsJsonPrimitive().isString()) {
            return parseHexColor(values.get(key).getAsString());
        }

        return -1; //white
    }

    private String getTexture(boolean isLeftWing) {
        String prideTrailRight = getString("prideTrailRight");
        if(!isLeftWing && (!(prideTrailRight == null || prideTrailRight.isEmpty())))
        {
            return prideTrailRight;
        }
        return getString("prideTrail");
    }

    private int getTrailColor(boolean isLeftWing)
    {
        return getBoolean("useColorBoth") ? resolveColor("color") : (isLeftWing ? resolveColor("color") : resolveColor("colorRight"));
    }
}