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

    // NEW: make sure we only send END once per spin
    private static boolean endSent = false;

    public static void setDurations()
    {
        double DURATION_S = Math.max(getConfig().clientPlayerConfig.twirlTime, 0.1);
        HALF_DURATION_S = DURATION_S * 0.5;
        OMEGA_RAD_S = (Math.PI * Math.PI) / DURATION_S;
        TURN360_DURATION_S = Math.TAU / OMEGA_RAD_S;
        if(getConfig().clientPlayerConfig.easeType == EasingUtil.EaseType.Back)
        {
            HALF_DURATION_S *= 4;
        }
        if(getConfig().clientPlayerConfig.easeType == EasingUtil.EaseType.None)
        {
            HALF_DURATION_S /= 1.5;
        }
    }

    public static void tickContinuousTwirlKey(boolean isDown, int desiredMode) {
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
        phase = Phase.EASE_IN_180;
        phaseStartNanos = TimeUtil.currentNanos();
        baseAngleRad = 0.0;

        holdMode = pendingMode;
        endSent = false;

        if (pendingMode == 0) {
            currentDir = nextAltDir;
            nextAltDir = -nextAltDir;
        } else {
            currentDir = pendingMode;
        }

        // +/-2 = CONTINUOUS_BEGIN
        EntityTwirlManager.sendStatePacket(currentDir * 2);
    }

    private static boolean shouldContinueConstant() {
        return keyDown && pendingMode == holdMode;
    }

    private static double rollFirst180(double elapsedS) {
        double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);
        return HALF_TURN * easeIn(u, getConfig().clientPlayerConfig.easeType);
    }

    private static double rollConstant360(double elapsedS) {
        double a = OMEGA_RAD_S * elapsedS;
        return Mth.clamp(a, 0.0, Math.TAU);
    }

    private static double rollLast180(double elapsedS) {
        double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);
        return HALF_TURN * easeOut(u, getConfig().clientPlayerConfig.easeType);
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

                        // NEW: completed a 360 while still holding => keepalive/loop ping
                        // +/-3 = CONTINUOUS_MIDDLE
                        if (shouldContinueConstant()) {
                            EntityTwirlManager.sendStatePacket(currentDir * 3);
                        }

                        elapsedS = (now - phaseStartNanos) / 1_000_000_000.0;

                        if (!shouldContinueConstant()) {
                            phase = Phase.EASE_OUT_180;

                            // NEW: tell others we're ending, but ONLY once we commit to ending
                            // (this preserves your "finish the current loop first" feel)
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
                    if (elapsedS >= HALF_DURATION_S) {
                        if (keyDown) {
                            startSpin();
                            return 0f;
                        }

                        active = false;
                        return 0f;
                    }
                    double angle = baseAngleRad + currentDir * rollLast180(elapsedS);
                    return (float) angle;
                }
            }
        }

        active = false;
        return 0f;
    }

    public static boolean isActive() {
        return active;
    }

    private ContinuousTwirlController() {}
}
