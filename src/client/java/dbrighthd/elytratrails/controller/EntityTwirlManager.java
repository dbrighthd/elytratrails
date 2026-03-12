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
import static dbrighthd.elytratrails.util.EasingUtil.easeRandom;
import static dbrighthd.elytratrails.util.EasingUtil.getEaseMult;

/**
 * this class is for handling twirling from *OTHER PLAYERS* on a server.
 */
public final class EntityTwirlManager {
    private static final double HALF_TURN = Math.PI;

    //private static final double BACK_HALF_PEAK_U = 0.518781;
    private static final double BACK_HALF_START_U = 0.481219;
    private static final double BACK_HALF_PEAK_U = 0.57;

    private static final double BACK_NORMAL_PEAK_T = 0.78;

    //private static final double BACK_NORMAL_PEAK_T = 0.7593905;
    private static final double BACK_NORMAL_START_T = 0.2406095;

    private enum Kind {NORMAL, CONTINUOUS}

    private enum Phase {EASE_IN_180, CONSTANT_360, EASE_OUT_180}

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

        boolean normalReverseRequested = false;
        int normalReverseDir = 1;

        boolean continuousReverseRequested = false;
        int continuousReverseDir = 1;

        boolean normalRestartRequested = false;
        int normalRestartDir = 1;

        boolean continuousRestartRequested = false;
        int continuousRestartDir = 1;
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

        long now = TimeUtil.currentNanos();
        TwirlInfo info = getEntityTwirlConfigs(entityId);

        switch (state) {
            case NORMAL -> {
                if (!data.active) {
                    startNormalNow(data, dir, now);
                } else {
                    queueNormalStartAfterFinish(data, dir);
                }
            }

            case NORMAL_REVERSE_SPLICE -> {
                if (!data.active) {
                    startNormalNow(data, dir, now);
                } else if (data.kind == Kind.NORMAL) {
                    double t = Mth.clamp((now - data.startNanos) / (info.duration() * 1_000_000_000.0), 0.0, 1.0);

                    if (info.easeType() == EasingUtil.EaseType.Back
                            && dir == -data.dir
                            && t < BACK_NORMAL_PEAK_T) {
                        clearContinuousReverseRequest(data);
                        clearContinuousRestartRequest(data);

                        data.normalReverseRequested = true;
                        data.normalReverseDir = dir;
                        clearNormalRestartRequest(data);
                    } else {
                        queueNormalStartAfterFinish(data, dir);
                    }
                } else {
                    queueNormalStartAfterFinish(data, dir);
                }
            }

            case CONTINUOUS_BEGIN -> {
                if (!data.active) {
                    startContinuousNow(data, dir, now);
                } else {
                    queueContinuousStartAfterFinish(data, dir);
                }
            }

            case CONTINUOUS_MIDDLE -> {
                data.active = true;
                data.kind = Kind.CONTINUOUS;
            }

            case CONTINUOUS_END -> {
                if (!data.active) {
                    return;
                }

                clearContinuousReverseRequest(data);
                clearContinuousRestartRequest(data);

                if (data.kind == Kind.CONTINUOUS) {
                    data.endRequested = true;
                }
            }

            case CONTINUOUS_REVERSE_SPLICE -> {
                if (!data.active) {
                    startContinuousNow(data, dir, now);
                } else if (data.kind == Kind.CONTINUOUS) {
                    boolean canStillSplice = true;

                    if (info.easeType() == EasingUtil.EaseType.Back && dir == -data.dir) {
                        if (data.phase == Phase.EASE_OUT_180) {
                            double u = Mth.clamp((now - data.phaseStartNanos) / (info.half_duration() * 1_000_000_000.0), 0.0, 1.0);
                            canStillSplice = u < BACK_HALF_PEAK_U;
                        }
                    }

                    if (info.easeType() == EasingUtil.EaseType.Back
                            && dir == -data.dir
                            && canStillSplice) {
                        clearNormalReverseRequest(data);
                        clearNormalRestartRequest(data);

                        data.continuousReverseRequested = true;
                        data.continuousReverseDir = dir;
                        clearContinuousRestartRequest(data);
                    } else {
                        queueContinuousStartAfterFinish(data, dir);
                    }
                } else {
                    queueContinuousStartAfterFinish(data, dir);
                }
            }

            default -> {
            }
        }
    }

    private record TwirlInfo(
            double duration,
            double half_duration,
            double omega_rad_s,
            double turn360_duration,
            EasingUtil.EaseType easeType
    ) {
    }

    private static TwirlInfo getEntityTwirlConfigs(int entityId) {
        PlayerConfig playerConfig = ClientPlayerConfigStore.getOrDefault(entityId);
        double DURATION_S = Math.max(playerConfig.twirlTime(), 0.0001);
        double HALF_DURATION_S = DURATION_S * 0.5;
        double OMEGA_RAD_S = (Math.PI * Math.PI) / DURATION_S;
        double TURN360_DURATION_S = Math.TAU / OMEGA_RAD_S;
        EasingUtil.EaseType easeType = playerConfig.easeType();

        DURATION_S *= getEaseMult(easeType);
        HALF_DURATION_S *= getEaseMult(easeType);

        return new TwirlInfo(DURATION_S, HALF_DURATION_S, OMEGA_RAD_S, TURN360_DURATION_S, easeType);
    }

    public static float getExtraRollRadians(int entityId) {
        TwirlData data = BY_ENTITY.get(entityId);
        if (data == null || !data.active) return 0f;

        long now = TimeUtil.currentNanos();
        TwirlInfo info = getEntityTwirlConfigs(entityId);
        return data.kind == Kind.NORMAL ? computeNormal(data, now, info) : computeContinuous(data, now, info);
    }

    private static float computeNormal(TwirlData data, long now, TwirlInfo info) {
        double t = (now - data.startNanos) / (info.duration() * 1_000_000_000.0);

        if (info.easeType() == EasingUtil.EaseType.Back
                && data.normalReverseRequested
                && data.normalReverseDir == -data.dir) {
            double clampedT = Mth.clamp(t, 0.0, 1.0);

            if (clampedT >= BACK_NORMAL_PEAK_T) {
                applyNormalReverseSplice(data, data.normalReverseDir, now, info);
                return computeNormal(data, now, info);
            }
        }

        if (t >= 1.0) {
            if (data.normalRestartRequested) {
                int dir = data.normalRestartDir;
                clearNormalRestartRequest(data);
                clearContinuousRestartRequest(data);
                startNormalNow(data, dir, now);
                return 0f;
            }

            if (data.continuousRestartRequested) {
                int dir = data.continuousRestartDir;
                clearContinuousRestartRequest(data);
                clearNormalRestartRequest(data);
                startContinuousNow(data, dir, now);
                return 0f;
            }

            BY_ENTITY.remove(data.entityId);
            return 0f;
        }

        t = Mth.clamp(t, 0.0, 1.0);
        double eased = EasingUtil.easeBoth(t, info.easeType());
        return (float) -(data.baseAngleRad + data.dir * eased * Math.TAU);
    }

    private static float computeContinuous(TwirlData data, long now, TwirlInfo info) {
        for (int guard = 0; guard < 10; guard++) {
            double elapsedS = (now - data.phaseStartNanos) / 1_000_000_000.0;

            switch (data.phase) {
                case EASE_IN_180 -> {
                    if (elapsedS >= info.half_duration()) {
                        data.baseAngleRad += data.dir * HALF_TURN;
                        data.phaseStartNanos += (long) (info.half_duration() * 1_000_000_000.0);

                        if (data.continuousReverseRequested
                                || data.endRequested
                                || data.normalRestartRequested
                                || data.continuousRestartRequested) {
                            data.phase = Phase.EASE_OUT_180;
                        } else {
                            data.phase = Phase.CONSTANT_360;
                        }
                        continue;
                    }

                    double u = Mth.clamp(elapsedS / info.half_duration(), 0.0, 1.0);
                    double roll = HALF_TURN * EasingUtil.easeIn(u, info.easeType());
                    return (float) (data.baseAngleRad + data.dir * roll);
                }

                case CONSTANT_360 -> {
                    while (elapsedS >= info.turn360_duration()) {
                        data.baseAngleRad += data.dir * Math.TAU;
                        data.phaseStartNanos += (long) (info.turn360_duration() * 1_000_000_000.0);
                        elapsedS = (now - data.phaseStartNanos) / 1_000_000_000.0;

                        if (data.continuousReverseRequested) {
                            data.phase = Phase.EASE_OUT_180;
                            data.phaseStartNanos = now;
                            data.endRequested = false;
                            return (float) data.baseAngleRad;
                        }

                        if (data.endRequested || data.normalRestartRequested || data.continuousRestartRequested) {
                            data.phase = Phase.EASE_OUT_180;
                            data.phaseStartNanos = now;
                            return (float) data.baseAngleRad;
                        }
                    }
                    if (info.easeType == EasingUtil.EaseType.Random) {
                        double a = easeRandom() * Math.TAU;
                        return (float) (data.baseAngleRad + data.dir * a);
                    }
                    double a = Mth.clamp(info.omega_rad_s() * elapsedS, 0.0, Math.TAU);
                    return (float) (data.baseAngleRad + data.dir * a);
                }

                case EASE_OUT_180 -> {
                    double u = Mth.clamp(elapsedS / info.half_duration(), 0.0, 1.0);

                    if (info.easeType() == EasingUtil.EaseType.Back
                            && data.continuousReverseRequested
                            && data.continuousReverseDir == -data.dir) {
                        if (u >= BACK_HALF_PEAK_U) {
                            applyContinuousReverseSplice(data, data.continuousReverseDir, now, info);
                            continue;
                        }
                    }

                    if (elapsedS >= info.half_duration()) {
                        if (data.normalRestartRequested) {
                            int dir = data.normalRestartDir;
                            clearNormalRestartRequest(data);
                            clearContinuousRestartRequest(data);
                            startNormalNow(data, dir, now);
                            return 0f;
                        }

                        if (data.continuousRestartRequested) {
                            int dir = data.continuousRestartDir;
                            clearContinuousRestartRequest(data);
                            clearNormalRestartRequest(data);
                            startContinuousNow(data, dir, now);
                            return 0f;
                        }

                        BY_ENTITY.remove(data.entityId);
                        return 0f;
                    }

                    double roll = HALF_TURN * EasingUtil.easeOut(u, info.easeType());
                    return (float) (data.baseAngleRad + data.dir * roll);
                }
            }
        }

        BY_ENTITY.remove(data.entityId);
        return 0f;
    }

    private static void queueNormalStartAfterFinish(TwirlData data, int dir) {
        clearNormalReverseRequest(data);
        clearContinuousReverseRequest(data);
        clearContinuousRestartRequest(data);

        data.normalRestartRequested = true;
        data.normalRestartDir = dir;

        if (data.kind == Kind.CONTINUOUS) {
            data.endRequested = true;
        }
    }

    private static void queueContinuousStartAfterFinish(TwirlData data, int dir) {
        clearNormalReverseRequest(data);
        clearContinuousReverseRequest(data);
        clearNormalRestartRequest(data);

        data.continuousRestartRequested = true;
        data.continuousRestartDir = dir;

        if (data.kind == Kind.CONTINUOUS) {
            data.endRequested = true;
        }
    }

    private static void startNormalNow(TwirlData data, int dir, long now) {
        data.kind = Kind.NORMAL;
        data.active = true;
        data.dir = dir;
        data.startNanos = now;
        data.baseAngleRad = 0.0;
        data.endRequested = false;

        clearNormalReverseRequest(data);
        clearContinuousReverseRequest(data);
        clearNormalRestartRequest(data);
        clearContinuousRestartRequest(data);
    }

    private static void startContinuousNow(TwirlData data, int dir, long now) {
        data.kind = Kind.CONTINUOUS;
        data.active = true;
        data.dir = dir;
        data.phase = Phase.EASE_IN_180;
        data.phaseStartNanos = now;
        data.baseAngleRad = 0.0;
        data.endRequested = false;

        clearNormalReverseRequest(data);
        clearContinuousReverseRequest(data);
        clearNormalRestartRequest(data);
        clearContinuousRestartRequest(data);
    }

    private static void applyNormalReverseSplice(TwirlData data, int dir, long now, TwirlInfo info) {
        double angle = currentAngle(data, now, info);

        data.kind = Kind.NORMAL;
        data.active = true;
        data.dir = dir;
        data.baseAngleRad = angle - dir * EasingUtil.easeBoth(BACK_NORMAL_START_T, info.easeType()) * Math.TAU;
        data.startNanos = now - (long) (BACK_NORMAL_START_T * info.duration() * 1_000_000_000.0);
        data.endRequested = false;

        clearNormalReverseRequest(data);
        clearContinuousReverseRequest(data);
        clearNormalRestartRequest(data);
        clearContinuousRestartRequest(data);
    }

    private static void applyContinuousReverseSplice(TwirlData data, int dir, long now, TwirlInfo info) {
        double angle = currentAngle(data, now, info);
        double startRoll = HALF_TURN * EasingUtil.easeIn(BACK_HALF_START_U, info.easeType());

        data.kind = Kind.CONTINUOUS;
        data.active = true;
        data.dir = dir;
        data.phase = Phase.EASE_IN_180;
        data.baseAngleRad = angle - dir * startRoll;
        data.phaseStartNanos = now - (long) (BACK_HALF_START_U * info.half_duration() * 1_000_000_000.0);
        data.endRequested = false;

        clearContinuousReverseRequest(data);
        clearNormalReverseRequest(data);
        clearNormalRestartRequest(data);
        clearContinuousRestartRequest(data);
    }

    private static double currentAngle(TwirlData data, long now, TwirlInfo info) {
        if (!data.active) return 0.0;

        return switch (data.kind) {
            case NORMAL -> currentNormalAngle(data, now, info);
            case CONTINUOUS -> currentContinuousAngle(data, now, info);
        };
    }

    private static double currentNormalAngle(TwirlData data, long now, TwirlInfo info) {
        double t = (now - data.startNanos) / (info.duration() * 1_000_000_000.0);
        t = Mth.clamp(t, 0.0, 1.0);

        double eased = EasingUtil.easeBoth(t, info.easeType());
        return data.baseAngleRad + data.dir * eased * Math.TAU;
    }

    private static double currentContinuousAngle(TwirlData data, long now, TwirlInfo info) {
        if (data.phaseStartNanos == 0L) return data.baseAngleRad;

        double elapsedS = (now - data.phaseStartNanos) / 1_000_000_000.0;

        return switch (data.phase) {
            case EASE_IN_180 -> {
                double u = Mth.clamp(elapsedS / info.half_duration(), 0.0, 1.0);
                double roll = HALF_TURN * EasingUtil.easeIn(u, info.easeType());
                yield data.baseAngleRad + data.dir * roll;
            }
            case CONSTANT_360 -> {
                if (info.easeType == EasingUtil.EaseType.Random) {
                    yield EasingUtil.easeRandom() * Math.TAU;
                }
                double a = Mth.clamp(info.omega_rad_s() * elapsedS, 0.0, Math.TAU);
                yield data.baseAngleRad + data.dir * a;
            }
            case EASE_OUT_180 -> {
                double u = Mth.clamp(elapsedS / info.half_duration(), 0.0, 1.0);
                double roll = HALF_TURN * EasingUtil.easeOut(u, info.easeType());
                yield data.baseAngleRad + data.dir * roll;
            }
        };
    }

    private static void clearNormalReverseRequest(TwirlData data) {
        data.normalReverseRequested = false;
        data.normalReverseDir = 1;
    }

    private static void clearContinuousReverseRequest(TwirlData data) {
        data.continuousReverseRequested = false;
        data.continuousReverseDir = 1;
    }

    private static void clearNormalRestartRequest(TwirlData data) {
        data.normalRestartRequested = false;
        data.normalRestartDir = 1;
    }

    private static void clearContinuousRestartRequest(TwirlData data) {
        data.continuousRestartRequested = false;
        data.continuousRestartDir = 1;
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
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return;

        TwirlStateC2SPayload payload = new TwirlStateC2SPayload(twirlState);
        if (!ClientPlayNetworking.canSend(payload.type())) return;

        ClientPlayNetworking.send(payload);
    }

    public static boolean isRolling(int entityId) {
        if (!getConfig().enableTwirls) {
            return false;
        }
        if (Minecraft.getInstance().player != null && entityId == Minecraft.getInstance().player.getId()) {
            return TwirlRoll.isAnyActive();
        }
        TwirlData data = BY_ENTITY.get(entityId);
        return data != null && data.active;
    }

    private EntityTwirlManager() {
    }
}