// TwirlRoll.java  (this is the missing piece that makes continuous twirls visible)
// Use TwirlRoll.getExtraRollRadians(...) wherever you currently apply roll to the model.
package dbrighthd.elytratrails.controller;

public final class TwirlRoll {
    private TwirlRoll() {}

    public static boolean isAnyActive() {
        return TwirlController.isActive() || ContinuousTwirlController.isActive();
    }

    public static float getExtraRollRadians(float partialTick) {
        if (ContinuousTwirlController.isActive()) {
            return ContinuousTwirlController.getExtraRollRadians(partialTick);
        }
        return -TwirlController.getExtraRollRadians(partialTick);
    }
}
