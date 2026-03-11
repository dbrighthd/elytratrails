package dbrighthd.elytratrails.config;

import dbrighthd.elytratrails.util.EasingUtil;
import dbrighthd.elytratrails.network.PlayerConfig;
import net.minecraft.client.Minecraft;

/**
 * This class is basically the configs you can select in the "Your trails" or the "Other's Trails". I used to have them
 * just be copies of variables in ModConfig but this way is much cleaner and allows them to share the same defaults.
 * can be exported to PlayerConfig for use elsewhere.
 */
public class ClientConfig {
    public boolean enableTrail;
    public boolean enableRandomWidth;
    public boolean speedDependentTrail;
    public double trailMinSpeed;
    public boolean trailMovesWithElytraAngle;
    public double maxWidth;
    public double trailLifetime;
    public double startRampDistance;
    public double endRampDistance;
    public int color;
    public double randomWidthVariation;
    public String prideTrail;
    public boolean fadeStart;
    public double fadeStartDistance;
    public boolean lifeTimeFade;
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
    public boolean speedBasedAlpha;
    public double minAlphaSpeed;
    public double maxAlphaSpeed;
    public boolean speedBasedWidth;
    public double minWidthSpeed;
    public double maxWidthSpeed;
    public boolean trailMovesWithAngleOfAttack;
    public boolean useColorBoth;
    public int colorRight;
    public double wingtipVerticalPosition;
    public double wingtipHorizontalPosition;
    public double wingtipDepthPosition;
    public double distanceTillTrailEnd;

    public transient int justColor;
    public transient int justAlpha;
    public transient int justColorRight;
    public transient int justAlphaRight;
    public ClientConfig(
            boolean enableTrail,
            boolean enableRandomWidth,
            boolean speedDependentTrail,
            double trailMinSpeed,
            boolean trailMovesWithElytraAngle,
            double maxWidth,
            double trailLifetime,
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
            double endDistanceFadeAmount,
            boolean speedBasedAlpha,
            double minAlphaSpeed,
            double maxAlphaSpeed,
            boolean speedBasedWidth,
            double minWidthSpeed,
            double maxWidthSpeed,
            boolean trailMovesWithAngleOfAttack,
            boolean useColorBoth,
            int colorRight,
            double wingtipVerticalPosition,
            double wingtipHorizontalPosition,
            double wingtipDepthPosition,
            double distanceTillTrailEnd
    ) {
        this.enableTrail = enableTrail;
        this.enableRandomWidth = enableRandomWidth;
        this.speedDependentTrail = speedDependentTrail;
        this.trailMinSpeed = trailMinSpeed;
        this.trailMovesWithElytraAngle = trailMovesWithElytraAngle;
        this.maxWidth = maxWidth;
        this.trailLifetime = trailLifetime;
        this.startRampDistance = startRampDistance;
        this.endRampDistance = endRampDistance;
        this.color = color;
        this.randomWidthVariation = randomWidthVariation;
        this.prideTrail = prideTrail;
        this.fadeStart = fadeStart;
        this.fadeStartDistance = fadeStartDistance;
        this.lifeTimeFade = fadeEnd;
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
        this.speedBasedAlpha = speedBasedAlpha;
        this.minAlphaSpeed = minAlphaSpeed;
        this.maxAlphaSpeed = maxAlphaSpeed;
        this.speedBasedWidth = speedBasedWidth;
        this.minWidthSpeed = minWidthSpeed;
        this.maxWidthSpeed = maxWidthSpeed;
        this.trailMovesWithAngleOfAttack = trailMovesWithAngleOfAttack;
        this.useColorBoth = useColorBoth;
        this.colorRight = colorRight;
        this.wingtipVerticalPosition = wingtipVerticalPosition;
        this.wingtipHorizontalPosition = wingtipHorizontalPosition;
        this.wingtipDepthPosition = wingtipDepthPosition;
        this.distanceTillTrailEnd = distanceTillTrailEnd;
    }

    public ClientConfig() {
        this(
                true,
                false,
                true,
                0.8,
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
                0.67,
                false,
                1.0,
                5.0,
                0.0,
                EasingUtil.EaseType.Back,
                false,
                1.0,
                false,
                0.75,
                2.0,
                false,
                0.75,
                2.0,
                true,
                true,
                0xFFFFFFFF,
                0.5,
                1.0,
                1,
                0.0
        );
    }

    public static ClientConfig getDefaultClientConfig() {
        return new ClientConfig();
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
                Minecraft.getInstance().getUser().getName(),
                speedBasedAlpha,
                minAlphaSpeed,
                maxAlphaSpeed,
                speedBasedWidth,
                minWidthSpeed,
                maxWidthSpeed,
                trailMovesWithAngleOfAttack,
                useColorBoth,
                colorRight,wingtipVerticalPosition,
                wingtipHorizontalPosition,
                wingtipDepthPosition,
                distanceTillTrailEnd);
    }
    public PlayerConfig getHiddenPlayerConfig()
    {
        return new PlayerConfig(
                false,
                enableRandomWidth,
                speedDependentTrail,
                trailMinSpeed,
                false,
                0,
                0,
                startRampDistance,
                endRampDistance,
                color,
                randomWidthVariation,
                prideTrail,
                fadeStart,
                fadeStartDistance,
                lifeTimeFade,
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
                endDistanceFadeAmount,
                "Hidden User",
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
                distanceTillTrailEnd);
    }
}
