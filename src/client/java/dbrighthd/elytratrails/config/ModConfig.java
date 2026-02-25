package dbrighthd.elytratrails.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "elytratrails")
public class ModConfig implements ConfigData {




    //your trail stuff
    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean enableTrail = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean translucentTrails = true;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean glowingTrails = true;


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
    public String color = "#FFFFFFFF";

    public int justColor = 0xFFFFFF;
    public int justAlpha = 255;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public String prideTrail = "";

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double randomWidthVariation = 1;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean fadeStart = false;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public double fadeStartDistance = 4.0;

    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean fadeEnd = true;


    @ConfigEntry.Category("elytra")
    @ConfigEntry.Gui.Tooltip
    public boolean wireframeTrails = false;

    public boolean exportPreset = false;
    public String exportPresetName = "";
    //general
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean enableAllTrails = true;



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
    public boolean enableTwirls = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean alwaysShowTrailDuringTwirl = false;

    public boolean logTrails = false;

    public ClearTrails clearTrailsOption = ClearTrails.NO;
    public enum ClearTrails {
        NO,
        CLEAR,
    }
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
    public boolean translucentTrailsOthersDefault = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean glowingTrailsOthersDefault = true;


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

    public int justAlphaOthersDefault = 255;

    public int justColorOthersDefault = 0xFFFFFF;

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

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean fadeStartOthersDefault = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public double fadeStartDistanceOthersDefault = 4.0;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean fadeEndOthersDefault = true;


    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean wireframeTrailsOthersDefault = false;

    //particles
    @ConfigEntry.Category("particles")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean enableParticles = false;


    @ConfigEntry.Category("particles")
    @ConfigEntry.Gui.Tooltip
    public ParticleChoice particle = ParticleChoice.POOF;
    public String Preset = "";
    public String PresetOthers = "";

    // "god i wish there was an easier way to do this"
    public enum ParticleChoice {
        ANGRY_VILLAGER,
        ASH,
        BUBBLE,
        BUBBLE_COLUMN_UP,
        BUBBLE_POP,
        CAMPFIRE_COSY_SMOKE,
        CAMPFIRE_SIGNAL_SMOKE,
        CHERRY_LEAVES,
        CLOUD,
        COMPOSTER,
        COPPER_FLAME,
        CRIMSON_SPORE,
        CRIT,
        CURRENT_DOWN,
        DAMAGE_INDICATOR,
        DOLPHIN,
        DRIPPING_DRIPSTONE_LAVA,
        DRIPPING_DRIPSTONE_WATER,
        DRIPPING_HONEY,
        DRIPPING_LAVA,
        DRIPPING_OBSIDIAN_TEAR,
        DRIPPING_WATER,
        DUST_PLUME,
        EGG_CRACK,
        ELECTRIC_SPARK,
        ELDER_GUARDIAN,
        ENCHANT,
        ENCHANTED_HIT,
        END_ROD,
        EXPLOSION,
        EXPLOSION_EMITTER,
        FALLING_DRIPSTONE_LAVA,
        FALLING_DRIPSTONE_WATER,
        FALLING_HONEY,
        FALLING_LAVA,
        FALLING_NECTAR,
        FALLING_OBSIDIAN_TEAR,
        FALLING_SPORE_BLOSSOM,
        FALLING_WATER,
        FIREFLY,
        FIREWORK,
        FISHING,
        FLAME,
        GLOW,
        GLOW_SQUID_INK,
        GUST,
        GUST_EMITTER_LARGE,
        GUST_EMITTER_SMALL,
        HAPPY_VILLAGER,
        HEART,
        INFESTED,
        ITEM_COBWEB,
        ITEM_SLIME,
        ITEM_SNOWBALL,
        LANDING_HONEY,
        LANDING_LAVA,
        LANDING_OBSIDIAN_TEAR,
        LARGE_SMOKE,
        LAVA,
        MYCELIUM,
        NAUTILUS,
        NOTE,
        OMINOUS_SPAWNING,
        PALE_OAK_LEAVES,
        POOF,
        PORTAL,
        RAIN,
        RAID_OMEN,
        REVERSE_PORTAL,
        SCULK_CHARGE_POP,
        SCULK_SOUL,
        SCRAPE,
        SMALL_FLAME,
        SMALL_GUST,
        SMOKE,
        SNEEZE,
        SNOWFLAKE,
        SONIC_BOOM,
        SOUL,
        SOUL_FIRE_FLAME,
        SPORE_BLOSSOM_AIR,
        SPLASH,
        SPIT,
        SQUID_INK,
        SWEEP_ATTACK,
        TOTEM_OF_UNDYING,
        TRIAL_OMEN,
        TRIAL_SPAWNER_DETECTED_PLAYER,
        TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS,
        UNDERWATER,
        VAULT_CONNECTION,
        WARPED_SPORE,
        WAX_OFF,
        WAX_ON,
        WHITE_ASH,
        WHITE_SMOKE,
        WITCH
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
