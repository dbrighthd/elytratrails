package dbrighthd.elytratrails.controller;

import dbrighthd.elytratrails.util.EasingUtil;
import dbrighthd.elytratrails.util.TimeUtil;
import net.minecraft.util.Mth;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.util.EasingUtil.easeBoth;

/**
 * This class is for the single twirls
 */
public final class TwirlController {
    private static long startNanos = 0L;
    private static boolean active = false;
    private static double DURATION_S;
    private static boolean keyDown = false;
    private static int pendingMode = 1;

    private static int currentDir = 1;
    private static int nextAltDir = 1;

    private static double baseAngleRad = 0.0;

    private static boolean reverseQueued = false;
    private static int reverseQueuedDir = 0;

    private static final double BACK_HALF_PEAK_U = 0.518781;
    private static final double BACK_HALF_START_U = 0.481219;

    private static final double BACK_TWIRL_PEAK_T = 0.5 + 0.5 * BACK_HALF_PEAK_U;   // 0.7593905
    private static final double BACK_TWIRL_START_T = 0.5 * BACK_HALF_START_U;        // 0.2406095

    public static void tickTwirlKey(boolean isDown, int desiredMode) {
        boolean wasDown = keyDown;
        keyDown = isDown;

        if (keyDown) {
            pendingMode = Integer.compare(desiredMode, 0);
        }

        if (keyDown && !wasDown && !active) {
            startSpin();
            return;
        }

        int queuedDir = getQueuedDirection();

        if (active
                && keyDown
                && queuedDir != 0
                && queuedDir == -currentDir
                && getConfig().clientPlayerConfig.easeType == EasingUtil.EaseType.Back) {

            long now = TimeUtil.currentNanos();
            double t = (now - startNanos) / (DURATION_S * 1_000_000_000.0);
            t = Mth.clamp(t, 0.0, 1.0);

            if (t >= 0.5 && t < BACK_TWIRL_PEAK_T) {
                reverseQueued = true;
                reverseQueuedDir = queuedDir;
            } else {
                reverseQueued = false;
                reverseQueuedDir = 0;
            }
        } else if (!keyDown || queuedDir == currentDir || queuedDir == 0) {
            reverseQueued = false;
            reverseQueuedDir = 0;
        }
    }

    private static int getQueuedDirection() {
        if (!keyDown) return 0;

        if (pendingMode == 0) {
            return nextAltDir;
        }

        return pendingMode;
    }

    private static void startSpin() {
        active = true;
        startNanos = TimeUtil.currentNanos();
        baseAngleRad = 0.0;

        reverseQueued = false;
        reverseQueuedDir = 0;

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

    private static void reverseFromBackPeak() {
        int oldDir = currentDir;
        int newDir = reverseQueuedDir;

        baseAngleRad += oldDir * Math.TAU;

        currentDir = newDir;

        if (pendingMode == 0) {
            nextAltDir = -currentDir;
        }

        long now = TimeUtil.currentNanos();
        long offset = (long) (BACK_TWIRL_START_T * DURATION_S * 1_000_000_000.0);
        startNanos = now - offset;

        reverseQueued = false;
        reverseQueuedDir = 0;

        EntityTwirlManager.sendStatePacket(TwirlState.NORMAL_REVERSE_SPLICE.signedId(currentDir));
    }

    public static float getExtraRollRadians() {
        if (!active) return 0f;

        long now = TimeUtil.currentNanos();
        double t = (now - startNanos) / (DURATION_S * 1_000_000_000.0);

        if (getConfig().clientPlayerConfig.easeType == EasingUtil.EaseType.Back
                && reverseQueued
                && reverseQueuedDir == -currentDir) {
            double clampedT = Mth.clamp(t, 0.0, 1.0);

            if (clampedT >= BACK_TWIRL_PEAK_T) {
                reverseFromBackPeak();
                return getExtraRollRadians();
            }
        }

        if (t >= 1.0) {
            reverseQueued = false;
            reverseQueuedDir = 0;

            if (keyDown) {
                startSpin();
            } else {
                active = false;
            }
            return 0f;
        }

        t = Mth.clamp(t, 0.0, 1.0);
        double eased = easeBoth(t, getConfig().clientPlayerConfig.easeType);
        return (float) (baseAngleRad + currentDir * eased * Math.TAU);
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