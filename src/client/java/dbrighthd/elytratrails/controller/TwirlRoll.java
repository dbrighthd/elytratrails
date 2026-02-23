
package dbrighthd.elytratrails.controller;

public final class TwirlRoll {
    private TwirlRoll() {}

    public static boolean isAnyActive() {
        return TwirlController.isActive() || ContinuousTwirlController.isActive();
    }

    public static float getExtraRollRadians() {
        if (ContinuousTwirlController.isActive()) {
            return -ContinuousTwirlController.getExtraRollRadians();
        }
        return TwirlController.getExtraRollRadians();
    }
}
