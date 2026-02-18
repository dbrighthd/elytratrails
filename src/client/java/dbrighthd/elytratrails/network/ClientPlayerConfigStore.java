package dbrighthd.elytratrails.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ConcurrentHashMap;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static java.lang.Math.clamp;


public final class ClientPlayerConfigStore
{
    public static final ConcurrentHashMap<Integer, PlayerConfig> CLIENT_PLAYER_CONFIGS = new ConcurrentHashMap<>();

    public static PlayerConfig getLocalPlayerConfig() {
        var config = getConfig();
        return new PlayerConfig(
                config.enableTrail,
                config.enableRandomWidth,
                config.speedDependentTrail,
                config.trailMinSpeed,
                config.trailMovesWithElytraAngle,
                config.maxWidth,
                config.trailLifetime,
                config.startRampDistance,
                config.endRampDistance,
                config.color,
                config.randomWidthVariation,
                config.prideTrail
        );
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
                    config.maxWidth,
                    0,
                    config.startRampDistance,
                    config.endRampDistance,
                    config.color,
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

        return new PlayerConfig(
                config.enableTrailOthersDefault,
                config.enableRandomWidthOthersDefault,
                config.speedDependentTrailOthersDefault,
                config.trailMinSpeedOthersDefault,
                config.trailMovesWithElytraAngleOthersDefault,
                config.maxWidthOthersDefault,
                config.trailLifetimeOthersDefault,
                config.startRampDistanceOthersDefault,
                config.endRampDistanceOthersDefault,
                config.colorOthersDefault,
                config.randomWidthVariationOthersDefault,
                config.prideTrailOthersDefault
        );
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

        String safeColor = incoming.color();
        if (safeColor != null) safeColor = safeColor.trim();
        if (safeColor != null && safeColor.isEmpty()) safeColor = null;

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