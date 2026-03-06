package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.util.EasingUtil;
import net.minecraft.nbt.CompoundTag;

public record PlayerConfig(
        boolean enableTrail,
        boolean enableRandomWidth,
        boolean speedDependentTrail,
        double trailMinSpeed,
        boolean trailMovesWithElytraAngle,
        double maxWidth,
        double trailLifetime,
        double startRampDistance,
        double endRampDistance,
        int color,
        double randomWidthVariation,
        String prideTrail,
        boolean fadeStart,
        double fadeStartDistance,
        boolean fadeEnd,
        boolean glowingTrails,
        boolean translucentTrails,
        boolean wireframeTrails,
        boolean alwaysShowTrailDuringTwirl,
        String prideTrailRight,
        double twirlTime,
        boolean increaseWidthOverTime,
        double startingWidthMultiplier,
        double endingWidthMultiplier,
        double distanceTillTrailStart,
        EasingUtil.EaseType easeType,
        boolean endDistanceFade,
        double endDistanceFadeAmount,
        String playerName
) {
    public static final int WIRE_VERSION = 1; // bump when you change meaning, not when you add fields

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("v", WIRE_VERSION);

        tag.putBoolean("enableTrail", enableTrail);
        tag.putBoolean("enableRandomWidth", enableRandomWidth);
        tag.putBoolean("speedDependentTrail", speedDependentTrail);
        tag.putDouble("trailMinSpeed", trailMinSpeed);
        tag.putBoolean("trailMovesWithElytraAngle", trailMovesWithElytraAngle);
        tag.putDouble("maxWidth", maxWidth);
        tag.putDouble("trailLifetime", trailLifetime);
        tag.putDouble("startRampDistance", startRampDistance);
        tag.putDouble("endRampDistance", endRampDistance);
        tag.putInt("color", color);
        tag.putDouble("randomWidthVariation", randomWidthVariation);
        tag.putString("prideTrail", prideTrail);
        tag.putBoolean("fadeStart", fadeStart);
        tag.putDouble("fadeStartDistance", fadeStartDistance);
        tag.putBoolean("fadeEnd", fadeEnd);
        tag.putBoolean("glowingTrails", glowingTrails);
        tag.putBoolean("translucentTrails", translucentTrails);
        tag.putBoolean("wireframeTrails", wireframeTrails);
        tag.putBoolean("alwaysShowTrailDuringTwirl", alwaysShowTrailDuringTwirl);
        tag.putString("prideTrailRight", prideTrailRight);
        tag.putDouble("twirlTime", twirlTime);
        tag.putBoolean("increaseWidthOverTime", increaseWidthOverTime);
        tag.putDouble("startingWidthMultiplier", startingWidthMultiplier);
        tag.putDouble("endingWidthMultiplier", endingWidthMultiplier);
        tag.putString("easeType", easeType.name());
        tag.putBoolean("endDistanceFade", endDistanceFade);
        tag.putDouble("endDistanceFadeAmount", endDistanceFadeAmount);
        tag.putString("playerName", playerName);


        return tag;
    }


}