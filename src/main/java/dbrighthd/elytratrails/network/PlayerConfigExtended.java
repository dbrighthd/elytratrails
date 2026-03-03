package dbrighthd.elytratrails.network;

/**
 * Per-player trail settings that can be synced over the network.
 *
 * Keep this record small and stable; fields that should not hard-fail when missing in older packets
 * should be encoded with optional codecs.
 */
public record PlayerConfigExtended(
        boolean alwaysShowTrailDuringTwirl,
        String prideTrailRight,
        double twirlTime,
        boolean increaseWidthOverTime,
        double startingWidthMultiplier,
        double endingWidthMultiplier,
        double distanceTillTrailStart,
        String easeType
) {
}
