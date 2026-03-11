package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.compat.flashback.FlashbackCompat;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.util.EasingUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.concurrent.ConcurrentHashMap;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static java.lang.Math.clamp;


public final class ClientPlayerConfigStore
{
    public static final ConcurrentHashMap<Integer, PlayerConfig> CLIENT_PLAYER_CONFIGS = new ConcurrentHashMap<>();
    public static final boolean FLASHBACK_LOADED = FabricLoader.getInstance().isModLoaded("flashback");
    public static PlayerConfig CLIENT_CONFIG;

    public static PlayerConfig CLIENT_OTHERS_CONFIG;

    public static void refreshLocalConfigs()
    {
        setLocalPlayerConfig();
        setClientOthersConfig();
    }

    public static void setLocalPlayerConfig() {
        var config = getConfig();
        CLIENT_CONFIG = config.clientPlayerConfig.getPlayerConfig();
    }

    public static void setClientOthersConfig ()
    {

        CLIENT_OTHERS_CONFIG = getConfig().otherPlayerConfig.getPlayerConfig();
    }

    public static PlayerConfig getLocalPlayerConfig() {
        if(CLIENT_CONFIG != null)
        {
            return CLIENT_CONFIG;
        }
        setLocalPlayerConfig();
        return CLIENT_CONFIG;
    }

    public static PlayerConfig getLocalPlayerConfigToSend()
    {
        var config = getConfig();
        if(config.showTrailToOtherPlayers)
        {
            return getLocalPlayerConfig();
        }
        else
        {
            return config.clientPlayerConfig.getHiddenPlayerConfig();
        }
    }
    public static PlayerConfig getLocalPlayerConfigOthers() {
        var config = getConfig();

        if (config.useSameDefaultsForOthers) {
            return getLocalPlayerConfig();
        }

        if(CLIENT_OTHERS_CONFIG != null)
        {
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
        if (!getConfig().syncWithServer)
        {
            return;
        }
        PlayerConfig incomingConfig = fromTag(configTag,entityId);

        double safeMaxWidth = clamp(incomingConfig.maxWidth(), 0, getConfig().maxOnlineWidth);
        double safeLifetime = clamp(incomingConfig.trailLifetime(), 0, getConfig().maxOnlineLifetime);

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

        CLIENT_PLAYER_CONFIGS.put(entityId,safe);
    }

    public static PlayerConfig fromTag(CompoundTag tag, int eid) {
        ModConfig cfg = getConfig();
        PlayerConfig fallbackConfig;

        if (Minecraft.getInstance().player != null && eid == Minecraft.getInstance().player.getId()) {
            fallbackConfig = cfg.clientPlayerConfig.getPlayerConfig();
        } else {
            fallbackConfig = cfg.otherPlayerConfig.getPlayerConfig();
        }

        boolean enableTrail = tag.getBooleanOr("enableTrail", fallbackConfig.enableTrail());
        boolean enableRandomWidth = tag.getBooleanOr("enableRandomWidth", fallbackConfig.enableRandomWidth());
        boolean speedDependentTrail = tag.getBooleanOr("speedDependentTrail", fallbackConfig.speedDependentTrail());
        double trailMinSpeed = tag.getDoubleOr("trailMinSpeed", fallbackConfig.trailMinSpeed());
        boolean trailMovesWithElytraAngle = tag.getBooleanOr("trailMovesWithElytraAngle", fallbackConfig.trailMovesWithElytraAngle());
        double maxWidth = tag.getDoubleOr("maxWidth", fallbackConfig.maxWidth());
        double trailLifetime = tag.getDoubleOr("trailLifetime", fallbackConfig.trailLifetime());
        double startRampDistance = tag.getDoubleOr("startRampDistance", fallbackConfig.startRampDistance());
        double endRampDistance = tag.getDoubleOr("endRampDistance", fallbackConfig.endRampDistance());
        int color = tag.getIntOr("color", fallbackConfig.color());
        double randomWidthVariation = tag.getDoubleOr("randomWidthVariation", fallbackConfig.randomWidthVariation());
        String prideTrail = tag.getStringOr("prideTrail", fallbackConfig.prideTrail());
        boolean fadeStart = tag.getBooleanOr("fadeStart", fallbackConfig.fadeStart());
        double fadeStartDistance = tag.getDoubleOr("fadeStartDistance", fallbackConfig.fadeStartDistance());
        boolean lifeTimeFade = tag.getBooleanOr("lifeTimeFade", fallbackConfig.lifeTimeFade());

        boolean glowingTrails = tag.getBooleanOr("glowingTrails", fallbackConfig.glowingTrails());
        boolean translucentTrails = tag.getBooleanOr("translucentTrails", fallbackConfig.translucentTrails());
        boolean wireframeTrails = tag.getBooleanOr("wireframeTrails", fallbackConfig.wireframeTrails());

        boolean alwaysShowTrailDuringTwirl = tag.getBooleanOr("alwaysShowTrailDuringTwirl", fallbackConfig.alwaysShowTrailDuringTwirl());
        String prideTrailRight = tag.getStringOr("prideTrailRight", fallbackConfig.prideTrailRight());
        double twirlTime = tag.getDoubleOr("twirlTime", fallbackConfig.twirlTime());
        boolean increaseWidthOverTime = tag.getBooleanOr("increaseWidthOverTime", fallbackConfig.increaseWidthOverTime());
        double startingWidthMultiplier = tag.getDoubleOr("startingWidthMultiplier", fallbackConfig.startingWidthMultiplier());
        double endingWidthMultiplier = tag.getDoubleOr("endingWidthMultiplier", fallbackConfig.endingWidthMultiplier());

        double distanceTillTrailStart = tag.getDoubleOr("distanceTillTrailStart", fallbackConfig.distanceTillTrailStart());

        EasingUtil.EaseType easeType = readEnum(tag, "easeType", EasingUtil.EaseType.class, fallbackConfig.easeType());

        boolean endDistanceFade = tag.getBooleanOr("endDistanceFade", fallbackConfig.endDistanceFade());
        double endDistanceFadeAmount = tag.getDoubleOr("endDistanceFadeAmount", fallbackConfig.endDistanceFadeAmount());

        String playerName = tag.getStringOr("playerName", fallbackConfig.playerName());
        boolean speedBasedAlpha = tag.getBooleanOr("speedBasedAlpha", fallbackConfig.speedBasedAlpha());
        double minAlphaSpeed = tag.getDoubleOr("minAlphaSpeed", fallbackConfig.minAlphaSpeed());
        double maxAlphaSpeed = tag.getDoubleOr("maxAlphaSpeed",fallbackConfig.maxAlphaSpeed());
        boolean speedBasedWidth = tag.getBooleanOr("speedBasedWidth", fallbackConfig.speedBasedWidth());
        double minWidthSpeed = tag.getDoubleOr("minWidthSpeed", fallbackConfig.minWidthSpeed());
        double maxWidthSpeed = tag.getDoubleOr("maxWidthSpeed", fallbackConfig.maxWidthSpeed());
        boolean trailMovesWithAngleOfAttack = tag.getBooleanOr("trailMovesWithAngleOfAttack",fallbackConfig.trailMovesWithAngleOfAttack());
        boolean useColorBoth = tag.getBooleanOr("useColorBoth",fallbackConfig.useColorBoth());
        int colorRight = tag.getIntOr("colorRight", fallbackConfig.colorRight());
        double wingtipVerticalPosition = tag.getDoubleOr("wingtipVerticalPosition", fallbackConfig.wingtipVerticalPosition());
        double wingtipHorizontalPosition = tag.getDoubleOr("wingtipHorizontalPosition", fallbackConfig.wingtipHorizontalPosition());
        double wingtipDepthPosition = tag.getDoubleOr("wingtipDepthPosition", fallbackConfig.wingtipDepthPosition());
        double distanceTillTrailEnd = tag.getDoubleOr("distanceTillTrailEnd", fallbackConfig.distanceTillTrailEnd());
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

        String s = tag.getStringOr(key,"Sine");
        try {
            return Enum.valueOf(enumClass, s);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
    public static PlayerConfig getOrDefault(int entityId)
    {
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().player.getId() == entityId)
        {
            if(FLASHBACK_LOADED && FlashbackCompat.isInReplay()) //return the config that was set at the time if it exists
            {
                if (CLIENT_PLAYER_CONFIGS.containsKey(entityId))
                {
                    return CLIENT_PLAYER_CONFIGS.get(entityId);
                }
            }
            return getLocalPlayerConfig();
        }
        else if (CLIENT_PLAYER_CONFIGS.containsKey(entityId))
        {
            return CLIENT_PLAYER_CONFIGS.get(entityId);
        }
        return getLocalPlayerConfigOthers();
    }
}