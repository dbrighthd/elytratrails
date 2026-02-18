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
    public boolean resourcePackOverride = true;

    //your trail stuff
    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean enableAllTrails = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean enableTrail = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean enableRandomWidth = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean useSplineTrail = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean speedDependentTrail = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean firstPersonTrail = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean trailMovesWithElytraAngle = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean emfSupport = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean cameraDistanceFade = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double maxWidth = 0.04;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double firstPersonFadeTime = 0.1;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double trailLifetime = 5.0;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public int maxSamplePerSecond = 60;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double trailMinSpeed = 0.7;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double startRampDistance = 4.0;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double endRampDistance = 4.0;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public String color = "#FFFFFF";

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double randomWidthVariation = 0.5;


    //server stuff
    @ConfigEntry.Category("server")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean syncWithServer = true;

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
    public boolean useSameDefaultsforOthers = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean enableTrailOthersDefault = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean enableRandomWidthOthersDefault = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean useSplineTrailOthersDefault = true;

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
    public double maxWidthOthersDefault = 0.04;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double trailLifetimeOthersDefault = 5.0;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double trailMinSpeedOthersDefault = 0.7;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double startRampDistanceOthersDefault = 4.0;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double endRampDistanceOthersDefault = 4.0;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public String colorOthersDefault = "#FFFFFF";

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double randomWidthVariationOthersDefault = 0.5;

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
    public int particleSpawnsPerTick = 1;
}
