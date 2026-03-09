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

    public static TrailOverrides fromBase(PlayerConfig baseConfig) {
        JsonObject json = new JsonObject();
        json.addProperty("enableTrail", baseConfig.enableTrail());
        json.addProperty("enableRandomWidth", baseConfig.enableRandomWidth());
        json.addProperty("speedDependentTrail", baseConfig.speedDependentTrail());
        json.addProperty("maxWidth", baseConfig.maxWidth());
        json.addProperty("trailLifetime", baseConfig.trailLifetime());
        json.addProperty("trailMinSpeed", baseConfig.trailMinSpeed());
        json.addProperty("startRampDistance", baseConfig.startRampDistance());
        json.addProperty("endRampDistance", baseConfig.endRampDistance());
        json.addProperty("randomWidthVariation", baseConfig.randomWidthVariation());
        json.addProperty("color", baseConfig.color());
        json.addProperty("prideTrail", baseConfig.prideTrail());
        json.addProperty("fadeStart", baseConfig.fadeStart());
        json.addProperty("fadeStartDistance", baseConfig.fadeStartDistance());
        json.addProperty("fadeEnd", baseConfig.fadeEnd());
        json.addProperty("glowingTrails", baseConfig.glowingTrails());
        json.addProperty("translucentTrails", baseConfig.translucentTrails());
        json.addProperty("wireframeTrails", baseConfig.wireframeTrails());
        json.addProperty("alwaysShowTrailDuringTwirl", baseConfig.alwaysShowTrailDuringTwirl());
        json.addProperty("prideTrailRight", baseConfig.prideTrailRight());
        json.addProperty("twirlTime", baseConfig.twirlTime());
        json.addProperty("increaseWidthOverTime", baseConfig.increaseWidthOverTime());
        json.addProperty("startingWidthMultiplier", baseConfig.startingWidthMultiplier());
        json.addProperty("endingWidthMultiplier", baseConfig.endingWidthMultiplier());
        json.addProperty("distanceTillTrailStart", baseConfig.distanceTillTrailStart());
        json.addProperty("endDistanceFade", baseConfig.endDistanceFade());
        json.addProperty("endDistanceFadeAmount", baseConfig.endDistanceFadeAmount());
        json.addProperty("speedBasedAlpha", baseConfig.speedBasedAlpha());
        json.addProperty("minAlphaSpeed", baseConfig.minAlphaSpeed());
        json.addProperty("maxAlphaSpeed", baseConfig.maxAlphaSpeed());
        json.addProperty("speedBasedWidth", baseConfig.speedBasedWidth());
        json.addProperty("minWidthSpeed", baseConfig.minWidthSpeed());
        json.addProperty("maxWidthSpeed", baseConfig.maxWidthSpeed());
        json.addProperty("trailMovesWithAngleOfAttack", baseConfig.trailMovesWithAngleOfAttack());
        json.addProperty("trailMovesWithElytraAngle", baseConfig.trailMovesWithElytraAngle());
        json.addProperty("useColorBoth", baseConfig.useColorBoth());
        json.addProperty("colorRight", baseConfig.colorRight());
        json.addProperty("wingtipVerticalPosition", baseConfig.wingtipVerticalPosition());
        json.addProperty("wingtipHorizontalPosition", baseConfig.wingtipHorizontalPosition());
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

    public ResolvedTrailSettings resolve() {
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
                resolveColor("color"),
                getString("prideTrail"),
                getBoolean("fadeStart"),
                getDouble("fadeStartDistance"),
                getBoolean("fadeEnd"),
                getBoolean("glowingTrails"),
                getBoolean("translucentTrails"),
                getBoolean("wireframeTrails"),
                getBoolean("alwaysShowTrailDuringTwirl"),
                getString("prideTrailRight"),
                getDouble("twirlTime"),
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
                getBoolean("trailMovesWithAngleOfAttack"),
                getBoolean("useColorBoth"),
                resolveColor("colorRight")
                );
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
}