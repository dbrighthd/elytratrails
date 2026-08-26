package dbrighthd.elytratrails.config;

import dbrighthd.elytratrails.twirling.EaseTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * The config that gets serialized and stored
 */
public class ModConfig {

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
    public boolean inputBuffer = true;
    public boolean applyWind = false;
    public double windScale = 1.0;
    public double windSpeed = 1.0;
    public ClearTrails clearTrailsOption = ClearTrails.NO;

    public enum ClearTrails {
        NO,
        CLEAR,
    }

    public boolean simplifyLighting = false;
    public boolean alwaysGlowWhenShaderTranslucent = true;

    //twirlstuff
    public String twirlOneEaseType = "Back";
    public String twirlTwoEaseType = "Sine";

    public double twirlOneTime = 0.67;
    public double twirlTwoTime = 0.67;

    public EaseTypes.AxisType twirlOneAxis = EaseTypes.AxisType.YP;
    public EaseTypes.AxisType twirlTwoAxis = EaseTypes.AxisType.YP;

    //server stuff
    public boolean syncWithServer = true;

    public boolean shareTrail = true;

    public boolean showTrailToOtherPlayers = true;

    public double maxOnlineWidth = 5.0;

    public double maxOnlineLifetime = 120.0;

    public boolean useSameDefaultsForOthers = false;

    public boolean enableParticles = false;
    public boolean useSplines = true;

    public ParticleOptions particle;

    public String Preset = "";
    public String PresetOthers = "";

    public int particleSpawnsPerTick = 3;

    public double particlesBlockRadius = 10;

    public double particlesVelocityAhead = 3;

    public double particlesVelocityBackwards = 0;

    public boolean skipLengthCheck = false;

    public LegacyTrailRender useOldRenderer = LegacyTrailRender.SHADERS;

    public enum LegacyTrailRender {
        SHADERS,
        NEVER,
        ALWAYS
    }

    public void validate() {
        if (clientPlayerConfig == null) {
            clientPlayerConfig = ClientConfig.getDefaultClientConfig();
        }
        if (otherPlayerConfig == null) {
            otherPlayerConfig = ClientConfig.getDefaultClientConfig();
        }

        //these next three dont matter and will be set to "" on config screen opening
        if (exportPresetName == null) {
            exportPresetName = "";
        }
        if (Preset == null) {
            Preset = "";
        }
        if (PresetOthers == null) {
            PresetOthers = "";
        }
        if (clearTrailsOption == null) {
            clearTrailsOption = ClearTrails.NO;
        }
        if (particle == null) {
            particle = ParticleTypes.POOF;
        }

        clientPlayerConfig.validate();
        otherPlayerConfig.validate();
    }
}
