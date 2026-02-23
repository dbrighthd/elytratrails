package dbrighthd.elytratrails.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "elytratrails")
public class ModConfig implements ConfigData {


    //general
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean enableAllTrails = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean translucentTrails = false;

    @SuppressWarnings("unused")
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.Excluded
    public boolean resourcePackOverride = true;


    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean emfSupport = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean glowingTrails = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean enableTwirls = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean alwaysShowTrailDuringTwirl = false;


    //your trail stuff
    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean enableTrail = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean enableRandomWidth = false;


    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean speedDependentTrail = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean fadeFirstPersonTrail = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean trailMovesWithElytraAngle = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean cameraDistanceFade = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double width = 0.1;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double firstPersonFadeTime = 0.3;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double trailLifetime = 2.5;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public int maxSamplePerSecond = 60;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double trailMinSpeed = 0.75;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double startRampDistance = 4.0;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double endRampDistance = 10.0;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public String color = "#FFFFFF";


    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public String prideTrail = "";

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double randomWidthVariation = 1;


    //server stuff
    @ConfigEntry.Category("server")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean syncWithServer = true;

    @ConfigEntry.Category("server")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean shareTrail = true;

    @ConfigEntry.Category("server")
    @ConfigEntry.Gui.Tooltip
    public boolean showTrailToOtherPlayers = true;

    @ConfigEntry.Category("server")
    @ConfigEntry.Gui.Tooltip
    public double maxOnlineWidth = 5.0;

    @ConfigEntry.Category("server")
    @ConfigEntry.Gui.Tooltip
    public double maxOnlineLifetime = 120.0;


    //others

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean useSameDefaultsforOthers = false;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean enableTrailOthersDefault = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean enableRandomWidthOthersDefault = false;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean speedDependentTrailOthersDefault = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean trailMovesWithElytraAngleOthersDefault = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean cameraDistanceFadeOthersDefault = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double widthOthersDefault = 0.1;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double trailLifetimeOthersDefault = 2.5;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double trailMinSpeedOthersDefault = 0.75;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double startRampDistanceOthersDefault = 4.0;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double endRampDistanceOthersDefault = 10.0;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public String colorOthersDefault = "#FFFFFF";

    /**
     * Optional pride flag id (from Pride Lib / mod id "pride") used for other players when no
     * per-player config is available.
     */
    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public String prideTrailOthersDefault = "";

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double randomWidthVariationOthersDefault = 1;

    //particles
    @ConfigEntry.Category("particles")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean enableParticles = false;





    @ConfigEntry.Category("particles")
    @ConfigEntry.Gui.Tooltip
    public ParticleChoice particle = ParticleChoice.CLOUD;

    public enum ParticleChoice {
        CLOUD, END_ROD, HAPPY_VILLAGER, FIREWORK, ASH,
    }

    @ConfigEntry.Category("particles") @ConfigEntry.Gui.Tooltip
    public int particleSpawnsPerTick = 3;

    @ConfigEntry.Category("particles") @ConfigEntry.Gui.Tooltip
    public double particlesBlockRadius = 10;

    @ConfigEntry.Category("particles") @ConfigEntry.Gui.Tooltip
    public double particlesVelocityAhead = 3;

    @ConfigEntry.Category("particles") @ConfigEntry.Gui.Tooltip
    public double particlesVelocityBackwards = 0;
}
