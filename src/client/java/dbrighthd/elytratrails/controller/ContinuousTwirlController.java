package dbrighthd.elytratrails.controller;

import dbrighthd.elytratrails.util.EasingUtil;
import dbrighthd.elytratrails.util.TimeUtil;
import net.minecraft.util.Mth;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.util.EasingUtil.*;

/**
 * This class handles the continuous twirling for when you hold down the continuous twirling keys
 */
public final class ContinuousTwirlController {
    private static final double HALF_TURN = Math.PI;

    private static double HALF_DURATION_S;

    private static double OMEGA_RAD_S;
    private static double TURN360_DURATION_S;

    private static final double BACK_EASE_OUT_PEAK_U = 0.54;
    private static final double BACK_EASE_IN_START_U = 0.481219;

    private enum Phase {
        EASE_IN_180,
        CONSTANT_360,
        EASE_OUT_180
    }

    private static long phaseStartNanos = 0L;
    private static Phase phase = Phase.EASE_IN_180;

    private static double baseAngleRad = 0.0;

    private static boolean active = false;

    private static boolean keyDown = false;
    private static int pendingMode = 1;

    private static int holdMode = 1;

    private static int currentDir = 1;
    private static int nextAltDir = 1;

    private static boolean endSent = false;

    private static boolean reverseQueued = false;
    private static int reverseQueuedDir = 0;
    private static EaseType easeType;

    private static int bufferedReverseTicks = 0;
    private static int bufferedReverseDir = 0;

    public static void setDurations() {
        double DURATION_S = Math.max(getConfig().clientPlayerConfig.twirlTime, 0.0001);
        HALF_DURATION_S = DURATION_S * 0.5;
        OMEGA_RAD_S = (Math.PI * Math.PI) / DURATION_S;
        TURN360_DURATION_S = Math.TAU / OMEGA_RAD_S;
        HALF_DURATION_S *= getEaseMult(getConfig().clientPlayerConfig.easeType);
        easeType = getConfig().clientPlayerConfig.easeType;
    }

    public static void tickContinuousTwirlKey(boolean isDown, int desiredMode) {
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

        int effectiveOppositeRequest = 0;

        if (keyDown && pendingMode != 0 && pendingMode == -currentDir) {
            effectiveOppositeRequest = pendingMode;
        } else if (bufferedReverseTicks > 0 && bufferedReverseDir == -currentDir) {
            effectiveOppositeRequest = bufferedReverseDir;
        }

        if (active
                && phase == Phase.EASE_OUT_180
                && effectiveOppositeRequest != 0
                && easeType == EasingUtil.EaseType.Back) {

            long now = TimeUtil.currentNanos();
            double elapsedS = (now - phaseStartNanos) / 1_000_000_000.0;
            double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);

            if (u < BACK_EASE_OUT_PEAK_U) {
                reverseQueued = true;
                reverseQueuedDir = effectiveOppositeRequest;

                if (bufferedReverseDir == effectiveOppositeRequest) {
                    bufferedReverseTicks = 0;
                    bufferedReverseDir = 0;
                }
            }
        }

        if (!active || (!keyDown && bufferedReverseTicks == 0)) {
            if (!reverseQueued || reverseQueuedDir != -currentDir) {
                reverseQueued = false;
                reverseQueuedDir = 0;
            }
        }
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

        if (phase != Phase.EASE_OUT_180) {
            return true;
        }

        long now = TimeUtil.currentNanos();
        double elapsedS = (now - phaseStartNanos) / 1_000_000_000.0;
        double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);
        return u < BACK_EASE_OUT_PEAK_U;
    }

    private static void startSpin() {
        active = true;
        phase = Phase.EASE_IN_180;
        phaseStartNanos = TimeUtil.currentNanos();
        baseAngleRad = 0.0;

        holdMode = pendingMode;
        endSent = false;

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

        EntityTwirlManager.sendStatePacket(currentDir * 2);
    }

    private static void reverseFromBackEaseOutPeak() {
        holdMode = reverseQueuedDir;

        int oldDir = currentDir;
        int newDir = reverseQueuedDir;

        baseAngleRad += oldDir * HALF_TURN;

        currentDir = newDir;
        phase = Phase.EASE_IN_180;

        long now = TimeUtil.currentNanos();
        long peakOffset = (long) (BACK_EASE_IN_START_U * HALF_DURATION_S * 1_000_000_000.0);
        phaseStartNanos = now - peakOffset;

        endSent = false;

        reverseQueued = false;
        reverseQueuedDir = 0;

        bufferedReverseTicks = 0;
        bufferedReverseDir = 0;

        EntityTwirlManager.sendStatePacket(TwirlState.CONTINUOUS_REVERSE_SPLICE.signedId(currentDir));
    }

    private static boolean shouldContinueConstant() {
        return keyDown && pendingMode == holdMode;
    }

    private static double rollFirst180(double elapsedS) {
        double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);
        return HALF_TURN * easeIn(u, easeType);
    }

    private static double rollConstant360(double elapsedS) {
        double a = OMEGA_RAD_S * elapsedS;
        if (easeType == EaseType.Random) {
            return EasingUtil.easeRandom() * Math.TAU;
        }
        return Mth.clamp(a, 0.0, Math.TAU);
    }

    private static double rollLast180(double elapsedS) {
        double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);
        return HALF_TURN * easeOut(u, easeType);
    }

    private static void sendEndOnce() {
        if (!endSent) {
            endSent = true;
            EntityTwirlManager.sendStatePacket(currentDir * 4);
        }
    }

    public static float getExtraRollRadians() {
        if (!active) return 0f;

        long now = TimeUtil.currentNanos();

        for (int guard = 0; guard < 6; guard++) {
            double elapsedS = (now - phaseStartNanos) / 1_000_000_000.0;

            switch (phase) {
                case EASE_IN_180: {
                    if (elapsedS >= HALF_DURATION_S) {
                        baseAngleRad += currentDir * HALF_TURN;
                        phaseStartNanos += (long) (HALF_DURATION_S * 1_000_000_000.0);

                        phase = shouldContinueConstant() ? Phase.CONSTANT_360 : Phase.EASE_OUT_180;

                        if (phase == Phase.EASE_OUT_180) {
                            sendEndOnce();
                        }
                        continue;
                    }

                    double angle = baseAngleRad + currentDir * rollFirst180(elapsedS);
                    return (float) angle;
                }

                case CONSTANT_360: {
                    while (elapsedS >= TURN360_DURATION_S) {
                        baseAngleRad += currentDir * Math.TAU;
                        phaseStartNanos += (long) (TURN360_DURATION_S * 1_000_000_000.0);

                        if (shouldContinueConstant()) {
                            EntityTwirlManager.sendStatePacket(currentDir * 3);
                        }

                        elapsedS = (now - phaseStartNanos) / 1_000_000_000.0;

                        if (!shouldContinueConstant()) {
                            phase = Phase.EASE_OUT_180;
                            sendEndOnce();
                            break;
                        }
                    }

                    if (phase == Phase.EASE_OUT_180) {
                        continue;
                    }

                    double angle = baseAngleRad + currentDir * rollConstant360(elapsedS);
                    return (float) angle;
                }

                case EASE_OUT_180: {
                    double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);

                    if (easeType == EasingUtil.EaseType.Back
                            && reverseQueued
                            && reverseQueuedDir == -currentDir) {

                        if (u >= BACK_EASE_OUT_PEAK_U) {
                            reverseFromBackEaseOutPeak();
                            continue;
                        }

                        double angle = baseAngleRad + currentDir * rollLast180(elapsedS);
                        return (float) angle;
                    }

                    if (elapsedS >= HALF_DURATION_S) {
                        if (keyDown) {
                            startSpin();
                            return 0f;
                        }

                        active = false;
                        bufferedReverseTicks = 0;
                        bufferedReverseDir = 0;
                        return 0f;
                    }

                    double angle = baseAngleRad + currentDir * rollLast180(elapsedS);
                    return (float) angle;
                }
            }
        }

        active = false;
        bufferedReverseTicks = 0;
        bufferedReverseDir = 0;
        return 0f;
    }

    public static boolean isActive() {
        return active;
    }

    public static int getCurrentDir() {
        return currentDir;
    }

    private ContinuousTwirlController() {
    }
}