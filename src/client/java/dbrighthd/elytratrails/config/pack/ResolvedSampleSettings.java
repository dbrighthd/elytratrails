package dbrighthd.elytratrails.config.pack;

/**
 * These are the unambiguous settings for what is needed when an entity is being sampled
 * @param useWithoutEmf Use sampling even without EMF present
 * @param speedDependentTrail Should it check for speed before sampling?
 * @param trailMinSpeed Minimum speed a trail should appear
 * @param xOffset X offset from model origin. In relative  model unites (1/16th of a block) with entities with models, in absolute blockpos for projectiles
 * @param yOffset Y offset from model origin. In relative model unites (1/16th of a block) with entities with models, in absolute blockpos for projectiles
 * @param zOffset Z offset from model origin. In relative model unites (1/16th of a block) with entities with models, in absolute blockpos for projectiles
 * @param billBoarded whether to billboard the XYZ offsets (useful for XP orbs and Ender Pearls)
 * @param useColorOverride whether to use the Color Override
 */
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
