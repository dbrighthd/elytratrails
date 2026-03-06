package dbrighthd.elytratrails.network;

/**
 * This exists purely so that we can know when an old packet is recieved, so we can yell at the players
 */
public record LegacyPlayerConfig(
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
        int trailType){}