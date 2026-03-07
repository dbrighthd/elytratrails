package dbrighthd.elytratrails.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

/**
 * The config that gets serialized and stored
 */
@Config(name = "elytratrails")
public class ModConfig implements ConfigData {

    public ClientConfig clientPlayerConfig = ClientConfig.getDefaultClientConfig();
    public ClientConfig otherPlayerConfig = ClientConfig.getDefaultClientConfig();
    public boolean exportPreset = false;
    public String exportPresetName = "";
    public boolean enableAllTrails = true;
    public int maxSamplePerSecond = 60;
    public boolean fadeFirstPersonTrail = true;
    public double firstPersonFadeTime = 0.2;
    public boolean resourcePackOverride = true;
    public boolean fishysStupidCameraRoll = false;
    public boolean fishysStupidThirdPersonCameraRoll = false;
    public boolean emfSupport = true;
    public boolean extendedEmfSupport = true;
    public boolean tryWithoutEmf = true;
    public boolean enableTwirls = true;
    public boolean tryNearTrailFade = false;
    public boolean alwaysSnapTrail = true;
    public boolean logTrails = false;
    public ClearTrails clearTrailsOption = ClearTrails.NO;
    public enum ClearTrails {
        NO,
        CLEAR,
    }

    public boolean alwaysGlowWhenShaderTranslucent = true;

    //server stuff
    public boolean syncWithServer = true;

    public boolean shareTrail = true;

    public boolean showTrailToOtherPlayers = true;

    public double maxOnlineWidth = 5.0;

    public double maxOnlineLifetime = 120.0;

    public boolean useSameDefaultsForOthers = false;

    public boolean enableParticles = false;


    public ParticleChoice particle = ParticleChoice.POOF;
    public String Preset = "";
    public String PresetOthers = "";

    // "god I wish there was an easier way to do this"
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

    public int particleSpawnsPerTick = 3;

    public double particlesBlockRadius = 10;

    public double particlesVelocityAhead = 3;

    public double particlesVelocityBackwards = 0;
}
