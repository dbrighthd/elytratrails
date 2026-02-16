package dbrighthd.elytratrails.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "elytratrails")
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public boolean enableTrail = true;
    @ConfigEntry.Gui.Tooltip
    public boolean enableRandomWidth = false;
    @ConfigEntry.Gui.Tooltip
    public boolean useSplineTrail = true;
    @ConfigEntry.Gui.Tooltip
    public boolean speedDependentTrail = true;
    @ConfigEntry.Gui.Tooltip
    public boolean firstPersonTrail = true;
    @ConfigEntry.Gui.Tooltip
    public boolean trailMovesWithElytraAngle = true;
    @ConfigEntry.Gui.Tooltip
    public boolean emfSupport = true;
    @ConfigEntry.Gui.Tooltip
    public boolean cameraDistanceFade = true;
    @ConfigEntry.Gui.Tooltip
    public double maxWidth = 0.04;
    @ConfigEntry.Gui.Tooltip
    public double firstPersonFadeTime = 0.1;
    @ConfigEntry.Gui.Tooltip
    public double trailLifetime = 1.5;
    @ConfigEntry.Gui.Tooltip
    public int maxSamplePerSecond = 60;
    @ConfigEntry.Gui.Tooltip
    public double trailMinSpeed = 0.7;
    @ConfigEntry.Gui.Tooltip
    public double startRampDistance = 4.0;
    @ConfigEntry.Gui.Tooltip
    public double endRampDistance = 4.0;



    @ConfigEntry.Gui.Tooltip
    public double randomWidthVariation = 0.5;
    @ConfigEntry.Gui.Tooltip
    public boolean enableParticles = false;

    @ConfigEntry.Gui.Tooltip
    public ParticleChoice particle = ParticleChoice.CLOUD;
    public enum ParticleChoice {
        CLOUD,
        END_ROD,
        HAPPY_VILLAGER,
        FIREWORK,
        ASH,
    }
    @ConfigEntry.Gui.Tooltip
    public int particleSpawnsPerTick = 1;






}