package dbrighthd.elytratrails;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public final class SpinController {
    private static final double TAU = Math.PI * 2.0;

    //one twirl duration
    private static final double DURATION_S = 0.5;

    private static long startNanos = 0L;
    private static boolean active = false;

    private static boolean keyDown = false;

    private static int pendingMode = +1;

    private static int currentDir = +1;

    private static int nextAltDir = +1;

    public static void tickTwirlKey(boolean isDown, int desiredMode) {
        boolean wasDown = keyDown;
        keyDown = isDown;

        if (keyDown) {
            pendingMode = (desiredMode < 0) ? -1 : (desiredMode > 0 ? +1 : 0);
        }

        if (keyDown && !wasDown && !active) {
            startSpin();
        }
    }

    private static void startSpin() {
        active = true;
        startNanos = Util.getNanos();

        if (pendingMode == 0) {
            currentDir = nextAltDir;
            nextAltDir = -nextAltDir;
        } else {
            currentDir = pendingMode;
        }
    }
    public static float getExtraRollRadians(float partialTick) {
        if (!active) return 0f;

        long now = Util.getNanos();
        double t = (now - startNanos) / (DURATION_S * 1_000_000_000.0);

        if (t >= 1.0) {
            if (keyDown) {
                startSpin();
                return 0f;
            } else {
                active = false;
                return 0f;
            }
        }

        t = Mth.clamp(t, 0.0, 1.0);

        double eased = 0.5 - 0.5 * Math.cos(Math.PI * t);

        return (float) (currentDir * eased * TAU);
    }

    private SpinController() {}
}
