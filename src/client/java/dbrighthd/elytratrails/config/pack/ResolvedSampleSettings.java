package dbrighthd.elytratrails.config.pack;

public record ResolvedSampleSettings(
        boolean speedDependentTrail,
        double trailMinSpeed,
        double xOffset,
        double yOffset,
        double zOffset) {
    public static ResolvedSampleSettings defaults() {
        return new ResolvedSampleSettings(false, 0, 0, 0, 0);
    }

}
