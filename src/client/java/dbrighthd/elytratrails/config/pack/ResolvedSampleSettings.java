package dbrighthd.elytratrails.config.pack;

public record ResolvedSampleSettings(
        boolean useWithoutEmf,
        boolean speedDependentTrail,
        double trailMinSpeed,
        double xOffset,
        double yOffset,
        double zOffset) {
    public static ResolvedSampleSettings defaults() {
        return new ResolvedSampleSettings(false, false, 0, 0, 0, 0);
    }

}
