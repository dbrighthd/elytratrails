package dbrighthd.elytratrails.controller;

import dbrighthd.elytratrails.util.EasingUtil;
import dbrighthd.elytratrails.util.TimeUtil;
import net.minecraft.util.Mth;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.util.EasingUtil.easeBoth;
import static dbrighthd.elytratrails.util.EasingUtil.getEaseMult;

/**
 * This class is for the single twirls
 */
public final class TwirlController {
    private static long startNanos = 0L;
    private static boolean active = false;
    private static double DURATION_S;
    private static boolean keyDown = false;
    private static int pendingMode = 1;
    private static EasingUtil.EaseType easeType;
    private static int currentDir = 1;
    private static int nextAltDir = 1;

    private static double baseAngleRad = 0.0;

    private static boolean reverseQueued = false;
    private static int reverseQueuedDir = 0;

    private static int bufferedReverseTicks = 0;
    private static int bufferedReverseDir = 0;

    private static final double BACK_HALF_PEAK_U = 0.518781;
    private static final double BACK_HALF_START_U = 0.481219;

    private static final double BACK_TWIRL_PEAK_T = 0.5 + 0.5 * BACK_HALF_PEAK_U;   // 0.7593905
    private static final double BACK_TWIRL_START_T = 0.5 * BACK_HALF_START_U;        // 0.2406095

    public static void tickTwirlKey(boolean isDown, int desiredMode) {
        boolean wasDown = keyDown;
        keyDown = isDown;

        if (bufferedReverseTicks > 0) {
            bufferedReverseTicks--;
            if (bufferedReverseTicks == 0) {
                bufferedReverseDir = 0;
            }
        }

        if (keyDown) {
            pendingMode = Integer.compare(desiredMode, 0);
        }

        if (keyDown && !wasDown && !active) {
            startSpin();
            return;
        }

        int effectiveQueuedDir = 0;
        if (keyDown) {
            effectiveQueuedDir = getQueuedDirection();
        } else if (bufferedReverseTicks > 0) {
            effectiveQueuedDir = bufferedReverseDir;
        }

        if (active
                && easeType == EasingUtil.EaseType.Back
                && effectiveQueuedDir != 0
                && effectiveQueuedDir == -currentDir) {

            long now = TimeUtil.currentNanos();
            double t = (now - startNanos) / (DURATION_S * 1_000_000_000.0);
            t = Mth.clamp(t, 0.0, 1.0);

            if (t >= 0.5 && t < BACK_TWIRL_PEAK_T) {
                reverseQueued = true;
                reverseQueuedDir = effectiveQueuedDir;

                if (bufferedReverseDir == effectiveQueuedDir) {
                    bufferedReverseTicks = 0;
                    bufferedReverseDir = 0;
                }
                return;
            }

            if (t < BACK_TWIRL_PEAK_T) {
                return;
            }
        }

        if (!reverseQueued || reverseQueuedDir != -currentDir) {
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

    public static void bufferReverseRequest(int dir, int ticks) {
        dir = Integer.compare(dir, 0);
        if (dir == 0 || ticks <= 0) return;

        bufferedReverseTicks = ticks;
        bufferedReverseDir = dir;
    }

    public static boolean canBufferBackReverse(int dir) {
        dir = Integer.compare(dir, 0);
        return active
                && easeType == EasingUtil.EaseType.Back
                && dir == -currentDir;
    }

    public static boolean canStillReverseFromBufferedBackInput(int dir) {
        dir = Integer.compare(dir, 0);
        if (!active || easeType != EasingUtil.EaseType.Back || dir != -currentDir) {
            return false;
        }

        long now = TimeUtil.currentNanos();
        double t = (now - startNanos) / (DURATION_S * 1_000_000_000.0);
        t = Mth.clamp(t, 0.0, 1.0);
        return t < BACK_TWIRL_PEAK_T;
    }

    private static void startSpin() {
        active = true;
        startNanos = TimeUtil.currentNanos();
        baseAngleRad = 0.0;

        reverseQueued = false;
        reverseQueuedDir = 0;

        bufferedReverseTicks = 0;
        bufferedReverseDir = 0;

        if (pendingMode == 0) {
            currentDir = nextAltDir;
            nextAltDir = -nextAltDir;
        } else {
            currentDir = pendingMode;
        }

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

        bufferedReverseTicks = 0;
        bufferedReverseDir = 0;
        EntityTwirlManager.sendStatePacket(TwirlState.NORMAL_REVERSE_SPLICE.signedId(currentDir));
    }

    public static float getExtraRollRadians() {
        if (!active) return 0f;

        long now = TimeUtil.currentNanos();
        double t = (now - startNanos) / (DURATION_S * 1_000_000_000.0);

        if (easeType == EasingUtil.EaseType.Back
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
            bufferedReverseTicks = 0;
            bufferedReverseDir = 0;

            if (keyDown) {
                startSpin();
            } else {
                active = false;
            }
            return 0f;
        }

        t = Mth.clamp(t, 0.0, 1.0);
        double eased = easeBoth(t, easeType);
        return (float) (baseAngleRad + currentDir * eased * Math.TAU);
    }

    public static void setDurations() {
        easeType = getConfig().clientPlayerConfig.easeType;
        DURATION_S = Math.max(getConfig().clientPlayerConfig.twirlTime, 0.0001);
        DURATION_S *= getEaseMult(easeType);
    }

    public static boolean isActive() {
        return active;
    }

    private TwirlController() {
    }
}