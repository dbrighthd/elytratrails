package dbrighthd.elytratrails.controller;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public final class ContinuousTwirlController {
    private static final double TAU = Math.PI * 2.0;
    private static final double HALF_TURN = Math.PI;

    private static final double DURATION_S = 0.5;
    private static final double HALF_DURATION_S = DURATION_S * 0.5;

    private static final double OMEGA_RAD_S = (Math.PI * Math.PI) / DURATION_S;
    private static final double TURN360_DURATION_S = TAU / OMEGA_RAD_S;

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
        phaseStartNanos = Util.getNanos();
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
        return HALF_TURN * (1.0 - Math.cos((Math.PI * 0.5) * u));
    }

    private static double rollConstant360(double elapsedS) {
        double a = OMEGA_RAD_S * elapsedS;
        return Mth.clamp(a, 0.0, TAU);
    }

    private static double rollLast180(double elapsedS) {
        double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);
        return HALF_TURN * Math.sin((Math.PI * 0.5) * u);
    }

    private static void sendEndOnce() {
        if (!endSent) {
            endSent = true;
            // +/-4 = CONTINUOUS_END
            EntityTwirlManager.sendStatePacket(currentDir * 4);
        }
    }

    public static float getExtraRollRadians() {
        if (!active) return 0f;

        long now = Util.getNanos();

        for (int guard = 0; guard < 6; guard++) {
            double elapsedS = (now - phaseStartNanos) / 1_000_000_000.0;

            switch (phase) {
                case EASE_IN_180: {
                    if (elapsedS >= HALF_DURATION_S) {
                        baseAngleRad += currentDir * HALF_TURN;
                        phaseStartNanos += (long) (HALF_DURATION_S * 1_000_000_000.0);

                        // identical behavior: decide whether to enter constant or ease out
                        phase = shouldContinueConstant() ? Phase.CONSTANT_360 : Phase.EASE_OUT_180;

                        // NEW: if we are going straight to ease-out (short tap), tell others now
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
                        baseAngleRad += currentDir * TAU;
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
