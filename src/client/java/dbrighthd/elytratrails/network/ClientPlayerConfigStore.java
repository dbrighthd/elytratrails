package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import net.minecraft.client.Minecraft;

import java.util.concurrent.ConcurrentHashMap;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static java.lang.Math.clamp;


public final class ClientPlayerConfigStore
{
    public static final ConcurrentHashMap<Integer, CompiledPlayerConfig> CLIENT_PLAYER_CONFIGS = new ConcurrentHashMap<>();

    public record TrailRenderSettings(boolean glowing, boolean translucent, boolean wireframe){}

    public static CompiledPlayerConfig CLIENT_CONFIG;

    public static CompiledPlayerConfig CLIENT_OTHERS_CONFIG;

    public static void refreshLocalConfigs()
    {
        setLocalPlayerConfig();
        setClientOthersConfig();
    }

    public static PlayerConfig getLocalPlayerConfigInitial()
    {
        ModConfig config = getConfig();
        return
                new PlayerConfig(
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
                        config.prideTrail,
                        config.fadeStart,
                        config.fadeStartDistance,
                        config.fadeEnd,
                        encodeTrailType(config.glowingTrails, config.translucentTrails, config.wireframeTrails)
                );
    }

    public static void setLocalPlayerConfig() {
        var config = getConfig();
        PlayerConfig playerConfig =
                getLocalPlayerConfigInitial();
        PlayerConfigExtended playerConfigExtended =
                new PlayerConfigExtended(
                        config.alwaysShowTrailDuringTwirl,
                        config.prideTrailRight,
                        config.twirlTime,
                        config.increaseWidthOverTime,
                        config.startingWidthMultiplier,
                        config.endingWidthMultiplier,
                        config.distanceTillTrailStart,
                        easeTypeToString(config.easeType));
        CLIENT_CONFIG = new CompiledPlayerConfig(playerConfig,playerConfigExtended);
    }

    public static String easeTypeToString(ModConfig.EaseType easeType)
    {
        if(easeType == ModConfig.EaseType.Sine)
        {
            return "sin";
        }
        if(easeType == ModConfig.EaseType.Back)
        {
            return "back";
        }

        if(easeType == ModConfig.EaseType.None)
        {
            return "none";
        }
        return "sin";
    }

    public static int encodeTrailType(boolean glow, boolean translucent, boolean wireframe)
    {
        return (glow ? 2 : 1) * (translucent ? 3 : 1) * (wireframe? 5 : 1);
    }

    public static TrailRenderSettings decodeTrailType(int n)
    {
        return new TrailRenderSettings((n%2==0),(n%3==0),(n%5==0));
    }
    public static void setClientOthersConfig ()
    {

        PlayerConfig playerConfig = getClientOthersConfigInitial();

        PlayerConfigExtended playerConfigExtended = getClientOthersConfigExtended();

        CLIENT_OTHERS_CONFIG = new CompiledPlayerConfig(playerConfig,playerConfigExtended);
    }
    public static PlayerConfig getClientOthersConfigInitial()
    {
        var config = getConfig();
        return new PlayerConfig(
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
                config.prideTrailOthersDefault,
                config.fadeStartOthersDefault,
                config.fadeStartDistanceOthersDefault,
                config.fadeEndOthersDefault,
                encodeTrailType(config.glowingTrailsOthersDefault, config.translucentTrailsOthersDefault, config.wireframeTrailsOthersDefault)
        );
    }
    public static PlayerConfigExtended getClientOthersConfigExtended()
    {
        var config = getConfig();
        return new PlayerConfigExtended(
                config.alwaysShowTrailDuringTwirlOthersDefault,
                config.prideTrailRightOthersDefault,
                config.twirlTimeOthersDefault,
                config.increaseWidthOverTimeOthersDefault,
                config.startingWidthMultiplierOthersDefault,
                config.endingWidthMultiplierOthersDefault,
                config.distanceTillTrailStartOthersDefault,
                easeTypeToString(config.easeTypeOthersDefault));
    }

    public static CompiledPlayerConfig getLocalPlayerConfig() {
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
            return getLocalPlayerConfig().playerConfigInitial;
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
                    "",
                    config.fadeStart,
                    config.fadeStartDistance,
                    config.fadeEnd,
                    1
            );
        }
    }
    public static CompiledPlayerConfig getLocalPlayerConfigOthers() {
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

    public static void putSafeInitial(int entityId, PlayerConfig incoming) {
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
                safePride,
                incoming.fadeStart(),
                incoming.fadeStartDistance(),
                incoming.fadeEnd(),
                incoming.trailType()
        );

        putInitialConfig(entityId,safe);
    }

    public static void putInitialConfig(int eid, PlayerConfig playerConfigInitial)
    {
        if (CLIENT_PLAYER_CONFIGS.containsKey(eid))
        {
            CLIENT_PLAYER_CONFIGS.get(eid).updateInitialConfig(playerConfigInitial);
        }
        else
        {
            CLIENT_PLAYER_CONFIGS.put(eid, new CompiledPlayerConfig(playerConfigInitial));
        }
    }
    public static void putExtendedConfig(int eid, PlayerConfigExtended playerConfigExtended)
    {
        if (CLIENT_PLAYER_CONFIGS.containsKey(eid))
        {
            CLIENT_PLAYER_CONFIGS.get(eid).updateExtendedConfig(playerConfigExtended);
        }
        else
        {
            CLIENT_PLAYER_CONFIGS.put(eid, new CompiledPlayerConfig(playerConfigExtended));
        }
    }

    public static CompiledPlayerConfig getOrDefault(int entityId)
    {
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().player.getId() == entityId)
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