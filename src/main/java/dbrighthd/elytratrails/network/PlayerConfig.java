package dbrighthd.elytratrails.network;

/**
 * Per-player trail settings that can be synced over the network.
 *
 * Keep this record small and stable; fields that should not hard-fail when missing in older packets
 * should be encoded with optional codecs.
 */
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
        String prideTrail
) {
}
