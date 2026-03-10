package dbrighthd.elytratrails.config.pack;

import dbrighthd.elytratrails.network.ClientPlayerConfigStore;

import static dbrighthd.elytratrails.config.pack.TrailPackConfigManager.resolveFromPlayerConfig;

public record ResolvedSampleSettings(
    boolean speedDependentTrail,
    double trailMinSpeed,
    double xOffset,
    double yOffset,
    double zOffset){
    public static ResolvedSampleSettings defaults() {
        return new ResolvedSampleSettings(false, 0, 0, 0, 0);
    }

}
