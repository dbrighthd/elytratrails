package dbrighthd.elytratrails.network;

//import dbrighthd.elytratrails.compat.flashback.FlashbackCompat;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.util.EasingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.concurrent.ConcurrentHashMap;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.compat.ModStatuses.FLASHBACK_LOADED;


public final class ClientPlayerConfigStore {
    public static final ConcurrentHashMap<Integer, PlayerConfig> CLIENT_PLAYER_CONFIGS = new ConcurrentHashMap<>();
    public static PlayerConfig CLIENT_CONFIG;

    public static PlayerConfig CLIENT_OTHERS_CONFIG;

    public static void refreshLocalConfigs() {
        setLocalPlayerConfig();
        setClientOthersConfig();
    }

    public static void setLocalPlayerConfig() {
        var config = getConfig();
        CLIENT_CONFIG = config.clientPlayerConfig.getPlayerConfig();
    }

    public static void setClientOthersConfig() {

        CLIENT_OTHERS_CONFIG = getConfig().otherPlayerConfig.getPlayerConfig();
    }

    public static PlayerConfig getLocalPlayerConfig() {
        if (CLIENT_CONFIG != null) {
            return CLIENT_CONFIG;
        }
        setLocalPlayerConfig();
        return CLIENT_CONFIG;
    }

    public static PlayerConfig getLocalPlayerConfigToSend() {
        var config = getConfig();
        if (config.showTrailToOtherPlayers) {
            return getLocalPlayerConfig();
        } else {
            return config.clientPlayerConfig.getHiddenPlayerConfig();
        }
    }

    public static PlayerConfig getLocalPlayerConfigOthers() {
        var config = getConfig();

        if (config.useSameDefaultsForOthers) {
            return getLocalPlayerConfig();
        }

        if (CLIENT_OTHERS_CONFIG != null) {
            return CLIENT_OTHERS_CONFIG;
        }
        setClientOthersConfig();
        return CLIENT_OTHERS_CONFIG;
    }

    public static void putSafeInitial(int entityId, CompoundTag configTag) {

        if (configTag == null) {
            CLIENT_PLAYER_CONFIGS.remove(entityId);
            return;
        }
        if (!getConfig().syncWithServer) {
            return;
        }
        PlayerConfig incomingConfig = fromTag(configTag, entityId);

        double safeMaxWidth = Math.min(Math.max(incomingConfig.maxWidth(), 0), getConfig().maxOnlineWidth);
        double safeLifetime = Math.min(Math.max(incomingConfig.trailLifetime(), 0), getConfig().maxOnlineLifetime);

        double safeMinSpeed = Math.max(0.0, incomingConfig.trailMinSpeed());
        double safeStartRamp = Math.max(0.0, incomingConfig.startRampDistance());
        double safeEndRamp = Math.max(0.0, incomingConfig.endRampDistance());
        double safeRandVar = Math.max(0.0, incomingConfig.randomWidthVariation());


        int safeColor = incomingConfig.color();

        String safePride = incomingConfig.prideTrail();
        if (safePride != null) safePride = safePride.trim();
        if (safePride == null) safePride = "";
        if (safePride.length() > 128) safePride = safePride.substring(0, 128);

        String safePrideRight = incomingConfig.prideTrailRight();
        if (safePrideRight != null) safePrideRight = safePrideRight.trim();
        if (safePrideRight == null) safePrideRight = "";
        if (safePrideRight.length() > 128) safePrideRight = safePrideRight.substring(0, 128);


        PlayerConfig safe = new PlayerConfig(
                incomingConfig.enableTrail(),
                incomingConfig.enableRandomWidth(),
                incomingConfig.speedDependentTrail(),
                safeMinSpeed,
                incomingConfig.trailMovesWithElytraAngle(),
                safeMaxWidth,
                safeLifetime,
                safeStartRamp,
                safeEndRamp,
                safeColor,
                safeRandVar,
                safePride,
                incomingConfig.fadeStart(),
                incomingConfig.fadeStartDistance(),
                incomingConfig.lifeTimeFade(),
                incomingConfig.glowingTrails(),
                incomingConfig.translucentTrails(),
                incomingConfig.wireframeTrails(),
                incomingConfig.alwaysShowTrailDuringTwirl(),
                safePrideRight,
                incomingConfig.twirlTime(),
                incomingConfig.increaseWidthOverTime(),
                incomingConfig.startingWidthMultiplier(),
                incomingConfig.endingWidthMultiplier(),
                incomingConfig.distanceTillTrailStart(),
                incomingConfig.easeType(),
                incomingConfig.endDistanceFade(),
                incomingConfig.endDistanceFadeAmount(),
                incomingConfig.playerName(),
                incomingConfig.speedBasedAlpha(),
                incomingConfig.minAlphaSpeed(),
                incomingConfig.maxAlphaSpeed(),
                incomingConfig.speedBasedWidth(),
                incomingConfig.minWidthSpeed(),
                incomingConfig.maxWidthSpeed(),
                incomingConfig.trailMovesWithAngleOfAttack(),
                incomingConfig.useColorBoth(),
                incomingConfig.colorRight(),
                incomingConfig.wingtipVerticalPosition(),
                incomingConfig.wingtipHorizontalPosition(),
                incomingConfig.wingtipDepthPosition(),
                incomingConfig.distanceTillTrailEnd()
        );

        CLIENT_PLAYER_CONFIGS.put(entityId, safe);
    }

    public static PlayerConfig fromTag(CompoundTag tag, int eid) {
        ModConfig cfg = getConfig();
        PlayerConfig fallbackConfig;

        if (Minecraft.getInstance().player != null && eid == Minecraft.getInstance().player.getId()) {
            fallbackConfig = cfg.clientPlayerConfig.getPlayerConfig();
        } else {
            fallbackConfig = cfg.otherPlayerConfig.getPlayerConfig();
        }

        boolean enableTrail = CompoundTagOrDefaults.getBooleanOr(tag, "enableTrail", fallbackConfig.enableTrail());
        boolean enableRandomWidth = CompoundTagOrDefaults.getBooleanOr(tag, "enableRandomWidth", fallbackConfig.enableRandomWidth());
        boolean speedDependentTrail = CompoundTagOrDefaults.getBooleanOr(tag, "speedDependentTrail", fallbackConfig.speedDependentTrail());
        double trailMinSpeed = CompoundTagOrDefaults.getDoubleOr(tag, "trailMinSpeed", fallbackConfig.trailMinSpeed());
        boolean trailMovesWithElytraAngle = CompoundTagOrDefaults.getBooleanOr(tag, "trailMovesWithElytraAngle", fallbackConfig.trailMovesWithElytraAngle());
        double maxWidth = CompoundTagOrDefaults.getDoubleOr(tag, "maxWidth", fallbackConfig.maxWidth());
        double trailLifetime = CompoundTagOrDefaults.getDoubleOr(tag, "trailLifetime", fallbackConfig.trailLifetime());
        double startRampDistance = CompoundTagOrDefaults.getDoubleOr(tag, "startRampDistance", fallbackConfig.startRampDistance());
        double endRampDistance = CompoundTagOrDefaults.getDoubleOr(tag, "endRampDistance", fallbackConfig.endRampDistance());
        int color = CompoundTagOrDefaults.getIntOr(tag, "color", fallbackConfig.color());
        double randomWidthVariation = CompoundTagOrDefaults.getDoubleOr(tag, "randomWidthVariation", fallbackConfig.randomWidthVariation());
        String prideTrail = CompoundTagOrDefaults.getStringOr(tag, "prideTrail", fallbackConfig.prideTrail());
        boolean fadeStart = CompoundTagOrDefaults.getBooleanOr(tag, "fadeStart", fallbackConfig.fadeStart());
        double fadeStartDistance = CompoundTagOrDefaults.getDoubleOr(tag, "fadeStartDistance", fallbackConfig.fadeStartDistance());
        boolean lifeTimeFade = CompoundTagOrDefaults.getBooleanOr(tag, "lifeTimeFade", fallbackConfig.lifeTimeFade());
        boolean glowingTrails = CompoundTagOrDefaults.getBooleanOr(tag, "glowingTrails", fallbackConfig.glowingTrails());
        boolean translucentTrails = CompoundTagOrDefaults.getBooleanOr(tag, "translucentTrails", fallbackConfig.translucentTrails());
        boolean wireframeTrails = CompoundTagOrDefaults.getBooleanOr(tag, "wireframeTrails", fallbackConfig.wireframeTrails());
        boolean alwaysShowTrailDuringTwirl = CompoundTagOrDefaults.getBooleanOr(tag, "alwaysShowTrailDuringTwirl", fallbackConfig.alwaysShowTrailDuringTwirl());
        String prideTrailRight = CompoundTagOrDefaults.getStringOr(tag, "prideTrailRight", fallbackConfig.prideTrailRight());
        double twirlTime = CompoundTagOrDefaults.getDoubleOr(tag, "twirlTime", fallbackConfig.twirlTime());
        boolean increaseWidthOverTime = CompoundTagOrDefaults.getBooleanOr(tag, "increaseWidthOverTime", fallbackConfig.increaseWidthOverTime());
        double startingWidthMultiplier = CompoundTagOrDefaults.getDoubleOr(tag, "startingWidthMultiplier", fallbackConfig.startingWidthMultiplier());
        double endingWidthMultiplier = CompoundTagOrDefaults.getDoubleOr(tag, "endingWidthMultiplier", fallbackConfig.endingWidthMultiplier());
        double distanceTillTrailStart = CompoundTagOrDefaults.getDoubleOr(tag, "distanceTillTrailStart", fallbackConfig.distanceTillTrailStart());
        EasingUtil.EaseType easeType = readEnum(tag, "easeType", EasingUtil.EaseType.class, fallbackConfig.easeType());
        boolean endDistanceFade = CompoundTagOrDefaults.getBooleanOr(tag, "endDistanceFade", fallbackConfig.endDistanceFade());
        double endDistanceFadeAmount = CompoundTagOrDefaults.getDoubleOr(tag, "endDistanceFadeAmount", fallbackConfig.endDistanceFadeAmount());
        String playerName = CompoundTagOrDefaults.getStringOr(tag, "playerName", fallbackConfig.playerName());
        boolean speedBasedAlpha = CompoundTagOrDefaults.getBooleanOr(tag, "speedBasedAlpha", fallbackConfig.speedBasedAlpha());
        double minAlphaSpeed = CompoundTagOrDefaults.getDoubleOr(tag, "minAlphaSpeed", fallbackConfig.minAlphaSpeed());
        double maxAlphaSpeed = CompoundTagOrDefaults.getDoubleOr(tag, "maxAlphaSpeed", fallbackConfig.maxAlphaSpeed());
        boolean speedBasedWidth = CompoundTagOrDefaults.getBooleanOr(tag, "speedBasedWidth", fallbackConfig.speedBasedWidth());
        double minWidthSpeed = CompoundTagOrDefaults.getDoubleOr(tag, "minWidthSpeed", fallbackConfig.minWidthSpeed());
        double maxWidthSpeed = CompoundTagOrDefaults.getDoubleOr(tag, "maxWidthSpeed", fallbackConfig.maxWidthSpeed());
        boolean trailMovesWithAngleOfAttack = CompoundTagOrDefaults.getBooleanOr(tag, "trailMovesWithAngleOfAttack", fallbackConfig.trailMovesWithAngleOfAttack());
        boolean useColorBoth = CompoundTagOrDefaults.getBooleanOr(tag, "useColorBoth", fallbackConfig.useColorBoth());
        int colorRight = CompoundTagOrDefaults.getIntOr(tag, "colorRight", fallbackConfig.colorRight());
        double wingtipVerticalPosition = CompoundTagOrDefaults.getDoubleOr(tag, "wingtipVerticalPosition", fallbackConfig.wingtipVerticalPosition());
        double wingtipHorizontalPosition = CompoundTagOrDefaults.getDoubleOr(tag, "wingtipHorizontalPosition", fallbackConfig.wingtipHorizontalPosition());
        double wingtipDepthPosition = CompoundTagOrDefaults.getDoubleOr(tag, "wingtipDepthPosition", fallbackConfig.wingtipDepthPosition());
        double distanceTillTrailEnd = CompoundTagOrDefaults.getDoubleOr(tag, "distanceTillTrailEnd", fallbackConfig.distanceTillTrailEnd());
        return new PlayerConfig(
                enableTrail,
                enableRandomWidth,
                speedDependentTrail,
                trailMinSpeed,
                trailMovesWithElytraAngle,
                maxWidth,
                trailLifetime,
                startRampDistance,
                endRampDistance,
                color,
                randomWidthVariation,
                prideTrail,
                fadeStart,
                fadeStartDistance,
                lifeTimeFade,
                glowingTrails,
                translucentTrails,
                wireframeTrails,
                alwaysShowTrailDuringTwirl,
                prideTrailRight,
                twirlTime,
                increaseWidthOverTime,
                startingWidthMultiplier,
                endingWidthMultiplier,
                distanceTillTrailStart,
                easeType,
                endDistanceFade,
                endDistanceFadeAmount,
                playerName,
                speedBasedAlpha,
                minAlphaSpeed,
                maxAlphaSpeed,
                speedBasedWidth,
                minWidthSpeed,
                maxWidthSpeed,
                trailMovesWithAngleOfAttack,
                useColorBoth,
                colorRight,
                wingtipVerticalPosition,
                wingtipHorizontalPosition,
                wingtipDepthPosition,
                distanceTillTrailEnd
        );
    }

    public static <E extends Enum<E>> E readEnum(CompoundTag tag, String key, Class<E> enumClass, E fallback) {
        if (!tag.contains(key)) return fallback;

        String s = CompoundTagOrDefaults.getStringOr(tag, key, "Sine");
        try {
            return Enum.valueOf(enumClass, s);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public static PlayerConfig getOrDefault(int entityId) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getId() == entityId) {
            if (FLASHBACK_LOADED) //return the config that was set at the time if it exists
            {
                if (CLIENT_PLAYER_CONFIGS.containsKey(entityId)) {
                    return CLIENT_PLAYER_CONFIGS.get(entityId);
                }
            }
            return getLocalPlayerConfig();
        } else if (CLIENT_PLAYER_CONFIGS.containsKey(entityId)) {
            return CLIENT_PLAYER_CONFIGS.get(entityId);
        }
        return getLocalPlayerConfigOthers();
    }
}