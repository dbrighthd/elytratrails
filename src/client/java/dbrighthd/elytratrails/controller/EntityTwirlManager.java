package dbrighthd.elytratrails.controller;

import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import dbrighthd.elytratrails.network.TwirlStateC2SPayload;
import dbrighthd.elytratrails.util.EasingUtil;
import dbrighthd.elytratrails.util.TimeUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

/**
 * this class is for handling twirling from *OTHER PLAYERS* on a server.
 */
public final class EntityTwirlManager {
    private static final double HALF_TURN = Math.PI;

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
        boolean endRequested = false;
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

        long now = TimeUtil.currentNanos();

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
                data.endRequested = false;
            }

            case CONTINUOUS_MIDDLE -> {
                data.kind = Kind.CONTINUOUS;
            }

            case CONTINUOUS_END -> {
                data.kind = Kind.CONTINUOUS;
                data.endRequested = true;
            }

            default -> {}
        }
    }

    private record TwirlInfo(double duration, double half_duration, double omega_rad_s, double turn360_duration, EasingUtil.EaseType easeType){}

    private static TwirlInfo getEntityTwirlConfigs(int entityId)
    {
        PlayerConfig playerConfig = ClientPlayerConfigStore.getOrDefault(entityId);
        double DURATION_S = Math.max(playerConfig.twirlTime(),0.1);
        double HALF_DURATION_S = DURATION_S * 0.5;
        double OMEGA_RAD_S = (Math.PI * Math.PI) / DURATION_S;
        double TURN360_DURATION_S = Math.TAU / OMEGA_RAD_S;
        EasingUtil.EaseType easeType = playerConfig.easeType();
        if(playerConfig.easeType() == EasingUtil.EaseType.Back)
        {
            HALF_DURATION_S *= 4;
        }
        if(playerConfig.easeType() == EasingUtil.EaseType.None)
        {
            HALF_DURATION_S /= 1.5;
        }
        return new TwirlInfo(DURATION_S,HALF_DURATION_S,OMEGA_RAD_S,TURN360_DURATION_S,easeType);
    }

    public static float getExtraRollRadians(int entityId) {
        TwirlData data = BY_ENTITY.get(entityId);
        if (data == null || !data.active) return 0f;

        long now = TimeUtil.currentNanos();
        return (data.kind == Kind.NORMAL) ? computeNormal(data, now, getEntityTwirlConfigs(entityId)) : computeContinuous(data, now, getEntityTwirlConfigs(entityId));
    }

    private static float computeNormal(TwirlData data, long now, TwirlInfo twirlInfo) {
        double t = (now - data.startNanos) / (twirlInfo.duration * 1_000_000_000.0);

        if (t >= 1.0) {
            BY_ENTITY.remove(data.entityId);
            return 0f;
        }

        t = Mth.clamp(t, 0.0, 1.0);
        double eased = 0.5 - 0.5 * Math.cos(Math.PI * t);
        return -(float) (data.dir * eased * Math.TAU);
    }

    private static float computeContinuous(TwirlData data, long now, TwirlInfo twirlInfo) {
        for (int guard = 0; guard < 10; guard++) {
            double elapsedS = (now - data.phaseStartNanos) / 1_000_000_000.0;

            switch (data.phase) {
                case EASE_IN_180 -> {
                    if (elapsedS >= twirlInfo.half_duration) {
                        data.baseAngleRad += data.dir * HALF_TURN;
                        data.phaseStartNanos += (long) (twirlInfo.half_duration * 1_000_000_000.0);
                        data.phase = Phase.CONSTANT_360;
                        continue;
                    }

                    double u = Mth.clamp(elapsedS / twirlInfo.half_duration, 0.0, 1.0);
                    double roll = HALF_TURN * EasingUtil.easeIn(u, twirlInfo.easeType);
                    return (float) (data.baseAngleRad + data.dir * roll);
                }

                case CONSTANT_360 -> {
                    while (elapsedS >= twirlInfo.turn360_duration()) {
                        data.baseAngleRad += data.dir * Math.TAU;
                        data.phaseStartNanos += (long) (twirlInfo.turn360_duration() * 1_000_000_000.0);
                        elapsedS = (now - data.phaseStartNanos) / 1_000_000_000.0;

                        if (data.endRequested) {
                            data.phase = Phase.EASE_OUT_180;
                            data.phaseStartNanos = now;
                            return (float) data.baseAngleRad;
                        }
                    }

                    double a = Mth.clamp(twirlInfo.omega_rad_s() * elapsedS, 0.0, Math.TAU);
                    return (float) (data.baseAngleRad + data.dir * a);
                }

                case EASE_OUT_180 -> {
                    if (elapsedS >= twirlInfo.half_duration) {
                        BY_ENTITY.remove(data.entityId);
                        return 0f;
                    }

                    double u = Mth.clamp(elapsedS / twirlInfo.half_duration, 0.0, 1.0);
                    double roll = HALF_TURN * EasingUtil.easeOut(u, twirlInfo.easeType);
                    return (float) (data.baseAngleRad + data.dir * roll);
                }
            }
        }

        BY_ENTITY.remove(data.entityId);
        return 0f;
    }

    private static double currentContinuousAngle(TwirlData data, long now, TwirlInfo twirlInfo) {
        if (data.phaseStartNanos == 0L) return data.baseAngleRad;

        double elapsedS = (now - data.phaseStartNanos) / 1_000_000_000.0;

        return switch (data.phase) {
            case EASE_IN_180 -> {
                double u = Mth.clamp(elapsedS / twirlInfo.half_duration, 0.0, 1.0);
                double roll = HALF_TURN * EasingUtil.easeIn(u, twirlInfo.easeType);
                yield data.baseAngleRad + data.dir * roll;
            }
            case CONSTANT_360 -> {
                double a = Mth.clamp(twirlInfo.omega_rad_s * elapsedS, 0.0, Math.TAU);
                yield data.baseAngleRad + data.dir * a;
            }
            case EASE_OUT_180 -> {
                double u = Mth.clamp(elapsedS / twirlInfo.half_duration, 0.0, 1.0);
                double roll = HALF_TURN * EasingUtil.easeOut(u, twirlInfo.easeType);
                yield data.baseAngleRad + data.dir * roll;
            }
        };
    }

    @SuppressWarnings("unused")
    public static void clearEntity(int entityId) {
        BY_ENTITY.remove(entityId);
    }

    @SuppressWarnings("unused")
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

    public static boolean isRolling(int entityId)
    {
        if(!getConfig().enableTwirls)
        {
            return false;
        }
        if(Minecraft.getInstance().player != null && entityId == Minecraft.getInstance().player.getId())
        {
            return TwirlRoll.isAnyActive();
        }
        TwirlData data = BY_ENTITY.get(entityId);
        return data != null && data.active;

    }
    private EntityTwirlManager() {}
}
