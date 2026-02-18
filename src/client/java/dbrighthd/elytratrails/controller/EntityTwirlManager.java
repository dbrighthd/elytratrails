package dbrighthd.elytratrails.controller;

import dbrighthd.elytratrails.network.TwirlStateC2SPayload;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public final class EntityTwirlManager {
    private static final double TAU = Math.PI * 2.0;
    private static final double HALF_TURN = Math.PI;

    private static final double DURATION_S = 0.5;
    private static final double HALF_DURATION_S = DURATION_S * 0.5;

    private static final double OMEGA_RAD_S = (Math.PI * Math.PI) / DURATION_S;
    private static final double TURN360_DURATION_S = TAU / OMEGA_RAD_S;

    private enum Kind { NORMAL, CONTINUOUS }
    private enum Phase { EASE_IN_180, CONSTANT_360, EASE_OUT_180 }

    private static final class TwirlData {
        final int entityId;

        TwirlData(int entityId) {
            this.entityId = entityId;
        }

        Kind kind = Kind.NORMAL;
        boolean active = false;
        int dir = 1;

        long startNanos = 0L;

        Phase phase = Phase.EASE_IN_180;
        long phaseStartNanos = 0L;
        double baseAngleRad = 0.0;
    }

    private static final Int2ObjectOpenHashMap<TwirlData> BY_ENTITY = new Int2ObjectOpenHashMap<>();

    public static void setEntityTwirlState(int entityId, int twirlState) {
        TwirlState state = TwirlState.fromId(twirlState);
        int dir = TwirlState.dirFromId(twirlState);

        if (state == TwirlState.OFF) {
            BY_ENTITY.remove(entityId);
            return;
        }

        TwirlData data = BY_ENTITY.get(entityId);
        if (data == null) {
            data = new TwirlData(entityId);
            BY_ENTITY.put(entityId, data);
        }

        data.active = true;
        data.dir = dir;

        long now = Util.getNanos();

        switch (state) {
            case NORMAL -> {
                data.kind = Kind.NORMAL;
                data.startNanos = now;
            }

            case CONTINUOUS_BEGIN -> {
                data.kind = Kind.CONTINUOUS;
                data.phase = Phase.EASE_IN_180;
                data.phaseStartNanos = now;
                data.baseAngleRad = 0.0;
            }

            case CONTINUOUS_MIDDLE -> {
                // Keepalive / loop boundary ping.
                // IMPORTANT: don't snap phases or restart time; that changes the visible feel.
                // We'll only ensure we're not accidentally in NORMAL.
                data.kind = Kind.CONTINUOUS;

                // If they weren't already continuous, do nothing else.
                // If they are already in CONSTANT_360, no action needed.
                // If they are in EASE_OUT (shouldn't happen unless packets reorder),
                // we also leave it alone to avoid "restarting" visuals.
            }

            case CONTINUOUS_END -> {
                data.kind = Kind.CONTINUOUS;

                // Start ease-out from current angle (smooth).
                double angleNow = currentContinuousAngle(data, now);
                data.phase = Phase.EASE_OUT_180;
                data.baseAngleRad = angleNow;
                data.phaseStartNanos = now;
            }

            default -> {}
        }
    }

    public static float getExtraRollRadians(int entityId, float partialTick) {
        TwirlData data = BY_ENTITY.get(entityId);
        if (data == null || !data.active) return 0f;

        long now = Util.getNanos();
        return (data.kind == Kind.NORMAL) ? computeNormal(data, now) : computeContinuous(data, now);
    }

    private static float computeNormal(TwirlData data, long now) {
        double t = (now - data.startNanos) / (DURATION_S * 1_000_000_000.0);

        if (t >= 1.0) {
            BY_ENTITY.remove(data.entityId);
            return 0f;
        }

        t = Mth.clamp(t, 0.0, 1.0);
        double eased = 0.5 - 0.5 * Math.cos(Math.PI * t);
        return -(float) (data.dir * eased * TAU);
    }

    private static float computeContinuous(TwirlData data, long now) {
        for (int guard = 0; guard < 10; guard++) {
            double elapsedS = (now - data.phaseStartNanos) / 1_000_000_000.0;

            switch (data.phase) {
                case EASE_IN_180 -> {
                    if (elapsedS >= HALF_DURATION_S) {
                        data.baseAngleRad += data.dir * HALF_TURN;
                        data.phaseStartNanos += (long) (HALF_DURATION_S * 1_000_000_000.0);
                        data.phase = Phase.CONSTANT_360;
                        continue;
                    }

                    double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);
                    double roll = HALF_TURN * (1.0 - Math.cos((Math.PI * 0.5) * u));
                    return (float) (data.baseAngleRad + data.dir * roll);
                }

                case CONSTANT_360 -> {
                    // FIX: loop forever; only END packet moves to EASE_OUT_180.
                    while (elapsedS >= TURN360_DURATION_S) {
                        data.baseAngleRad += data.dir * TAU;
                        data.phaseStartNanos += (long) (TURN360_DURATION_S * 1_000_000_000.0);
                        elapsedS = (now - data.phaseStartNanos) / 1_000_000_000.0;
                    }

                    double a = Mth.clamp(OMEGA_RAD_S * elapsedS, 0.0, TAU);
                    return (float) (data.baseAngleRad + data.dir * a);
                }

                case EASE_OUT_180 -> {
                    if (elapsedS >= HALF_DURATION_S) {
                        BY_ENTITY.remove(data.entityId);
                        return 0f;
                    }

                    double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);
                    double roll = HALF_TURN * Math.sin((Math.PI * 0.5) * u);
                    return (float) (data.baseAngleRad + data.dir * roll);
                }
            }
        }

        BY_ENTITY.remove(data.entityId);
        return 0f;
    }

    private static double currentContinuousAngle(TwirlData data, long now) {
        if (data.phaseStartNanos == 0L) return data.baseAngleRad;

        double elapsedS = (now - data.phaseStartNanos) / 1_000_000_000.0;

        return switch (data.phase) {
            case EASE_IN_180 -> {
                double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);
                double roll = HALF_TURN * (1.0 - Math.cos((Math.PI * 0.5) * u));
                yield data.baseAngleRad + data.dir * roll;
            }
            case CONSTANT_360 -> {
                double a = Mth.clamp(OMEGA_RAD_S * elapsedS, 0.0, TAU);
                yield data.baseAngleRad + data.dir * a;
            }
            case EASE_OUT_180 -> {
                double u = Mth.clamp(elapsedS / HALF_DURATION_S, 0.0, 1.0);
                double roll = HALF_TURN * Math.sin((Math.PI * 0.5) * u);
                yield data.baseAngleRad + data.dir * roll;
            }
        };
    }

    public static void clearEntity(int entityId) {
        BY_ENTITY.remove(entityId);
    }

    public static void clearAll() {
        BY_ENTITY.clear();
    }

    public static void sendStatePacket(int twirlState) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return;

        TwirlStateC2SPayload payload = new TwirlStateC2SPayload(twirlState);
        if (!ClientPlayNetworking.canSend(payload.type())) return;

        ClientPlayNetworking.send(payload);
    }

    private EntityTwirlManager() {}
}
