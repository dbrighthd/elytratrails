package dbrighthd.elytratrails.network;

public record PlayerConfig(boolean enableTrail, boolean enableRandomWidth, boolean speedDependentTrail, double trailMinSpeed, boolean trailMovesWithElytraAngle, double maxWidth, double trailLifetime, double startRampDistance, double endRampDistance, String color, double randomWidthVariation) {
}
