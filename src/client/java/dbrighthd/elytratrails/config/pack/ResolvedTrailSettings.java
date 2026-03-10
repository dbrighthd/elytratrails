package dbrighthd.elytratrails.config.pack;

import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import static dbrighthd.elytratrails.config.pack.TrailPackConfigManager.resolveTrailFromPlayerConfig;

/**
 * ResolvedTrailSettings objects are the "final" state of a trail's settings, where nothing is ambiguous, and it's ready to render
 */
public record ResolvedTrailSettings(
        boolean enableTrail,
        boolean enableRandomWidth,
        boolean speedDependentTrail,
        double maxWidth,
        double trailLifetime,
        double trailMinSpeed,
        double startRampDistance,
        double endRampDistance,
        double randomWidthVariation,
        int color,
        String prideTrail,
        boolean fadeStart,
        double fadeStartDistance,
        boolean fadeEnd,
        boolean glowingTrails,
        boolean translucentTrails,
        boolean wireframeTrails,
        boolean alwaysShowTrailDuringTwirl,
        boolean increaseWidthOverTime,
        double startingWidthMultiplier,
        double endingWidthMultiplier,
        double distanceTillTrailStart,
        boolean endDistanceFade,
        double endDistanceFadeAmount,
        boolean speedBasedAlpha,
        double minAlphaSpeed,
        double maxAlphaSpeed,
        boolean speedBasedWidth,
        double minWidthSpeed,
        double maxWidthSpeed,
        double distanceTillTrailEnd
) {
    public static ResolvedTrailSettings defaults(boolean isLeftWing) {
        return resolveTrailFromPlayerConfig(ClientPlayerConfigStore.getLocalPlayerConfig(),isLeftWing);
    }
}

