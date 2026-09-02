package dbrighthd.elytratrails.config.pack;

public record ResolvedSampleSettings(
        boolean useWithoutEmf,
        boolean speedDependentTrail,
        double trailMinSpeed,
        double xOffset,
        double yOffset,
        double zOffset,
        boolean billBoarded,
        boolean useColorOverride) {
    public static ResolvedSampleSettings defaults() {
        return new ResolvedSampleSettings(true, false, 0, 0, 0, 0, false, true);
    }
    public static ResolvedSampleSettings playerDefaults() {
        return new ResolvedSampleSettings(false, false, 0, 0, 0, 0, false, true);
    }

}
