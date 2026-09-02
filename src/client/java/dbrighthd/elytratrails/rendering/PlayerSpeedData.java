package dbrighthd.elytratrails.rendering;

/**
 * Data from player's speed to be used in trail rendering
 * @param speed speed at emission
 * @param aoa AOA at emission
 * @param aoaProvided if AOA is needed in the first place (if not, don't waste time calculating)
 */
public record PlayerSpeedData(double speed, float aoa, boolean aoaProvided) {
}
