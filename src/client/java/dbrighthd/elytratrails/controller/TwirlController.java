package dbrighthd.elytratrails.controller;

import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.util.TimeUtil;
import net.minecraft.util.Mth;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.controller.EasingUtil.easeBoth;
import static dbrighthd.elytratrails.controller.EasingUtil.easeBothBack;

public final class TwirlController {
    private static long startNanos = 0L;
    private static boolean active = false;
    private static double DURATION_S;
    private static boolean keyDown = false;
    private static int pendingMode = 1;

    private static int currentDir = 1;
    private static int nextAltDir = 1;

    public static void tickTwirlKey(boolean isDown, int desiredMode) {
        boolean wasDown = keyDown;
        keyDown = isDown;

        if (keyDown) {
            pendingMode = Integer.compare(desiredMode, 0);
        }

        if (keyDown && !wasDown && !active) {
            startSpin();
        }
    }

    private static void startSpin() {
        active = true;
        startNanos = TimeUtil.currentNanos();

        if (pendingMode == 0) {
            currentDir = nextAltDir;
            nextAltDir = -nextAltDir;
        } else {
            currentDir = pendingMode;
        }

        // Send "normal twirl" to server/others (sign encodes direction)
        // +/-1 = NORMAL
        EntityTwirlManager.sendStatePacket(currentDir);
    }

    public static float getExtraRollRadians() {
        if (!active) return 0f;

        long now = TimeUtil.currentNanos();
        double t = (now - startNanos) / (DURATION_S * 1_000_000_000.0);

        if (t >= 1.0) {
            if (keyDown) {
                startSpin();
            } else {
                active = false;
            }
            return 0f;
        }

        t = Mth.clamp(t, 0.0, 1.0);
        double eased = easeBoth(t, getConfig().clientPlayerConfig.easeType);
        return (float) (currentDir * eased * Math.TAU);
    }

    public static void setDurations()
    {
        DURATION_S = Math.max(getConfig().clientPlayerConfig.twirlTime,0.1);
        if(getConfig().clientPlayerConfig.easeType == EasingUtil.EaseType.Back)
        {
            DURATION_S *= 4;
        }
    }
    public static boolean isActive() {
        return active;
    }

    private TwirlController() {}
}
