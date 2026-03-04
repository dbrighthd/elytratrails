package dbrighthd.elytratrails.config;

import dbrighthd.elytratrails.controller.EasingUtil;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;

public class ClientConfig {
    public boolean enableTrail;
    public boolean enableRandomWidth;
    public boolean speedDependentTrail;
    public double trailMinSpeed;
    public boolean trailMovesWithElytraAngle;
    public double maxWidth;
    public double trailLifeTime;
    public double startRampDistance;
    public double endRampDistance;
    public int color;
    public double randomWidthVariation;
    public String prideTrail;
    public boolean fadeStart;
    public double fadeStartDistance;
    public boolean fadeEnd;
    public boolean glowingTrails;
    public boolean translucentTrails;
    public boolean wireframeTrails;
    public boolean alwaysShowTrailDuringTwirl;
    public String prideTrailRight;
    public double twirlTime;
    public boolean increaseWidthOverTime;
    public double startingWidthMultiplier;
    public double endingWidthMultiplier;
    public double distanceTillTrailStart;
    public EasingUtil.EaseType easeType;
    public boolean endDistanceFade;
    public double endDistanceFadeAmount;

    public int justColor;
    public int justAlpha;

    public ClientConfig(
            boolean enableTrail,
            boolean enableRandomWidth,
            boolean speedDependentTrail,
            double trailMinSpeed,
            boolean trailMovesWithElytraAngle,
            double maxWidth,
            double trailLifeTime,
            double startRampDistance,
            double endRampDistance,
            int color,
            double randomWidthVariation,
            String prideTrail,
            boolean fadeStart,
            double fadeStartDistance,
            boolean fadeEnd,
            boolean glowingTrails,
            boolean translucentTrails,
            boolean wireframeTrails,
            boolean alwaysShowTrailDuringTwirl,
            String prideTrailRight,
            double twirlTime,
            boolean increaseWidthOverTime,
            double startingWidthMultiplier,
            double endingWidthMultiplier,
            double distanceTillTrailStart,
            EasingUtil.EaseType easeType,
            boolean endDistanceFade,
            double endDistanceFadeAmount
    ) {
        this.enableTrail = enableTrail;
        this.enableRandomWidth = enableRandomWidth;
        this.speedDependentTrail = speedDependentTrail;
        this.trailMinSpeed = trailMinSpeed;
        this.trailMovesWithElytraAngle = trailMovesWithElytraAngle;
        this.maxWidth = maxWidth;
        this.trailLifeTime = trailLifeTime;
        this.startRampDistance = startRampDistance;
        this.endRampDistance = endRampDistance;
        this.color = color;
        this.randomWidthVariation = randomWidthVariation;
        this.prideTrail = prideTrail;
        this.fadeStart = fadeStart;
        this.fadeStartDistance = fadeStartDistance;
        this.fadeEnd = fadeEnd;
        this.glowingTrails = glowingTrails;
        this.translucentTrails = translucentTrails;
        this.wireframeTrails = wireframeTrails;
        this.alwaysShowTrailDuringTwirl = alwaysShowTrailDuringTwirl;
        this.prideTrailRight = prideTrailRight;
        this.twirlTime = twirlTime;
        this.increaseWidthOverTime = increaseWidthOverTime;
        this.startingWidthMultiplier = startingWidthMultiplier;
        this.endingWidthMultiplier = endingWidthMultiplier;
        this.distanceTillTrailStart = distanceTillTrailStart;
        this.easeType = easeType;
        this.endDistanceFade = endDistanceFade;
        this.endDistanceFadeAmount = endDistanceFadeAmount;
    }

    public static ClientConfig getDefaultClientConfig()
    {
        return new ClientConfig(
                true,
                false,
                true,
                0.75,
                true,
                0.05,
                2.5,
                4.0,
                10.0,
                0xFFFFFFFF,
                0.5,
                "",
                false,
                1.0,
                true,
                false,
                true,
                false,
                false,
                "",
                0.5,
                false,
                1.0,
                5.0,
                0.0,
                EasingUtil.EaseType.Sine,
                false,
                1.0
        );
    }
    public PlayerConfig getPlayerConfig()
    {
        return new PlayerConfig(
                enableTrail,
                enableRandomWidth,
                speedDependentTrail,
                trailMinSpeed,
                trailMovesWithElytraAngle,
                maxWidth,
                trailLifeTime,
                startRampDistance,
                endRampDistance,
                color,
                randomWidthVariation,
                prideTrail,
                fadeStart,
                fadeStartDistance,
                fadeEnd,
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
                endDistanceFadeAmount);
    }
    public PlayerConfig getHiddenPlayerConfig()
    {
        return new PlayerConfig(
                false,
                enableRandomWidth,
                speedDependentTrail,
                trailMinSpeed,
                trailMovesWithElytraAngle,
                0,
                0,
                startRampDistance,
                endRampDistance,
                color,
                randomWidthVariation,
                prideTrail,
                fadeStart,
                fadeStartDistance,
                fadeEnd,
                false,
                false,
                false,
                false,
                prideTrailRight,
                twirlTime,
                increaseWidthOverTime,
                startingWidthMultiplier,
                endingWidthMultiplier,
                distanceTillTrailStart,
                easeType,
                endDistanceFade,
                endDistanceFadeAmount);
    }
}
