package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import net.minecraft.client.Minecraft;

import java.util.concurrent.ConcurrentHashMap;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static java.lang.Math.clamp;


public final class ClientPlayerConfigStore
{
    public static final ConcurrentHashMap<Integer, PlayerConfig> CLIENT_PLAYER_CONFIGS = new ConcurrentHashMap<>();

    public static PlayerConfig CLIENT_CONFIG;

    public static PlayerConfig CLIENT_OTHERS_CONFIG;

    public static void refreshLocalConfigs()
    {
        setLocalPlayerConfig();
        setClientOthersConfig();
    }
    public static void setLocalPlayerConfig() {
        var config = getConfig();
        CLIENT_CONFIG = new PlayerConfig(
                config.enableTrail,
                config.enableRandomWidth,
                config.speedDependentTrail,
                config.trailMinSpeed,
                config.trailMovesWithElytraAngle,
                config.width,
                config.trailLifetime,
                config.startRampDistance,
                config.endRampDistance,
                TrailPackConfigManager.parseHexColor(config.color),
                config.randomWidthVariation,
                config.prideTrail
        );
    }

    public static void setClientOthersConfig ()
    {
        var config = getConfig();
        CLIENT_OTHERS_CONFIG = new PlayerConfig(
                config.enableTrailOthersDefault,
                config.enableRandomWidthOthersDefault,
                config.speedDependentTrailOthersDefault,
                config.trailMinSpeedOthersDefault,
                config.trailMovesWithElytraAngleOthersDefault,
                config.widthOthersDefault,
                config.trailLifetimeOthersDefault,
                config.startRampDistanceOthersDefault,
                config.endRampDistanceOthersDefault,
                TrailPackConfigManager.parseHexColor(config.colorOthersDefault),
                config.randomWidthVariationOthersDefault,
                config.prideTrailOthersDefault
        );
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
            return new PlayerConfig(
                    false,
                    config.enableRandomWidth,
                    config.speedDependentTrail,
                    config.trailMinSpeed,
                    config.trailMovesWithElytraAngle,
                    config.width,
                    0,
                    config.startRampDistance,
                    config.endRampDistance,
                    0xFFFFFFFF,
                    config.randomWidthVariation,
                    ""
            );
        }
    }
    public static PlayerConfig getLocalPlayerConfigOthers() {
        var config = getConfig();

        // If enabled, "others" use the same defaults as the local (elytra) config.
        if (config.useSameDefaultsforOthers) {
            return getLocalPlayerConfig();
        }

        if(CLIENT_OTHERS_CONFIG != null)
        {
            return CLIENT_OTHERS_CONFIG;
        }
        setClientOthersConfig();
        return CLIENT_OTHERS_CONFIG;
    }

    public static void putSafe(int entityId, PlayerConfig incoming) {
        if (incoming == null) {
            CLIENT_PLAYER_CONFIGS.remove(entityId);
            return;
        }
        if (!getConfig().syncWithServer)
        {
            return;
        }

        double safeMaxWidth = clamp(incoming.maxWidth(), 0, getConfig().maxOnlineWidth);
        double safeLifetime = clamp(incoming.trailLifetime(), 0, getConfig().maxOnlineLifetime);

        double safeMinSpeed = Math.max(0.0, incoming.trailMinSpeed());
        double safeStartRamp = Math.max(0.0, incoming.startRampDistance());
        double safeEndRamp = Math.max(0.0, incoming.endRampDistance());
        double safeRandVar = Math.max(0.0, incoming.randomWidthVariation());

        int safeColor = incoming.color();

        String safePride = incoming.prideTrail();
        if (safePride != null) safePride = safePride.trim();
        if (safePride == null) safePride = "";
        if (safePride.length() > 128) safePride = safePride.substring(0, 128);

        PlayerConfig safe = new PlayerConfig(
                incoming.enableTrail(),
                incoming.enableRandomWidth(),
                incoming.speedDependentTrail(),
                safeMinSpeed,
                incoming.trailMovesWithElytraAngle(),
                safeMaxWidth,
                safeLifetime,
                safeStartRamp,
                safeEndRamp,
                safeColor,
                safeRandVar,
                safePride
        );

        CLIENT_PLAYER_CONFIGS.put(entityId, safe);


    }

    public static PlayerConfig getOrDefault(int entityId)
    {
        if(Minecraft.getInstance().player.getId() == entityId)
        {
            return getLocalPlayerConfig();
        }
        else if (CLIENT_PLAYER_CONFIGS.containsKey(entityId))
        {
            return CLIENT_PLAYER_CONFIGS.get(entityId);
        }
        return getLocalPlayerConfigOthers();
    }
}