package dbrighthd.elytratrails.config;

import dbrighthd.elytratrails.ElytraTrailsKeybind;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.controller.ContinuousTwirlController;
import dbrighthd.elytratrails.util.EasingUtil;
import dbrighthd.elytratrails.controller.TwirlController;
import dbrighthd.elytratrails.network.GetAllRequestC2SPayload;
import dbrighthd.elytratrails.network.PlayerConfigC2SPayload;
import dbrighthd.elytratrails.network.RemoveFromStoreC2SPayload;
import dbrighthd.elytratrails.rendering.TrailSystem;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.ElytraTrailsClient.refreshConfig;
import static dbrighthd.elytratrails.config.pack.TrailPackConfigManager.*;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.*;

/**
 * Config screen built using cloth config
 */
public class ConfigScreenBuilder {

    public static Screen buildConfigScreen(Screen parent, ModConfig config)
    {
        decodeConfigColors(config.clientPlayerConfig);
        decodeConfigColors(config.otherPlayerConfig);
        ClientConfig defaultConfig = ClientConfig.getDefaultClientConfig();


        TrailPackConfigManager.reloadDiskPresets();
        String presetDefault = "";
        config.Preset = presetDefault;
        config.PresetOthers = presetDefault;
        List<String> presetNames = new ArrayList<>(TrailPackConfigManager.getPresets().keySet());
        Collections.sort(presetNames);
        presetNames.addFirst("default");
        presetNames.addFirst(presetDefault);
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("text.elytratrails.title"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("text.elytratrails.category.general"));
        ConfigCategory elytra = builder.getOrCreateCategory(Component.translatable("text.elytratrails.category.elytra"));
        ConfigCategory server = builder.getOrCreateCategory(Component.translatable("text.elytratrails.category.server"));
        ConfigCategory others = builder.getOrCreateCategory(Component.translatable("text.elytratrails.category.others"));
        ConfigCategory twirling = builder.getOrCreateCategory(Component.translatable("text.elytratrails.category.twirling"));
        ConfigCategory particles = builder.getOrCreateCategory(Component.translatable("text.elytratrails.category.particles"));
        ConfigCategory presets = builder.getOrCreateCategory(Component.translatable("text.elytratrails.category.presets"));
        ConfigCategory keybinds = builder.getOrCreateCategory(Component.translatable("text.elytratrails.category.keybinds"));
        SubCategoryBuilder advanced = entryBuilder.startSubCategory(Component.translatable("text.elytratrails.category.elytra.advanced"));

        SubCategoryBuilder advancedOthers = entryBuilder.startSubCategory(Component.translatable("text.elytratrails.category.elytra.advancedOthers"));


        general.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.general.desc"))
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableAllTrails"), config.enableAllTrails)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.enableAllTrails.@Tooltip"))
                .setSaveConsumer(newValue -> config.enableAllTrails = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.alwaysSnapTrail"), config.alwaysSnapTrail)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.alwaysSnapTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.alwaysSnapTrail = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.hardCodedFreshAnimationsPlayerWingtips"), config.hardCodedFreshAnimationsPlayerWingtips)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.hardCodedFreshAnimationsPlayerWingtips.@Tooltip"))
                .setSaveConsumer(newValue -> config.hardCodedFreshAnimationsPlayerWingtips = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.cameraDistanceFade"), config.tryNearTrailFade)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.cameraDistanceFade.@Tooltip"))
                .setSaveConsumer(newValue -> config.tryNearTrailFade = newValue)
                .build());
        general.addEntry(entryBuilder.startIntField(Component.translatable("text.elytratrails.option.maxSamplePerSecond"), config.maxSamplePerSecond)
                .setDefaultValue(60)
                .setTooltip(Component.translatable("text.elytratrails.option.maxSamplePerSecond.@Tooltip"))
                .setSaveConsumer(newValue -> config.maxSamplePerSecond = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeFirstPersonTrail"), config.fadeFirstPersonTrail)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeFirstPersonTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.fadeFirstPersonTrail = newValue)
                .build());
        general.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.firstPersonFadeTime"), config.firstPersonFadeTime)
                .setDefaultValue(0.2)
                .setTooltip(Component.translatable("text.elytratrails.option.firstPersonFadeTime.@Tooltip"))
                .setSaveConsumer(newValue -> config.firstPersonFadeTime = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.resourcePackOverride"), config.resourcePackOverride)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.resourcePackOverride.@Tooltip"))
                .setSaveConsumer(newValue -> config.resourcePackOverride = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.emfSupport"), config.emfSupport)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.emfSupport.@Tooltip"))
                .setSaveConsumer(newValue -> config.emfSupport = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.extendedEmfSupport"), config.extendedEmfSupport)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.extendedEmfSupport.@Tooltip"))
                .setSaveConsumer(newValue -> config.extendedEmfSupport = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.tryWithoutEmf"), config.tryWithoutEmf)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.tryWithoutEmf.@Tooltip"))
                .setSaveConsumer(newValue -> config.tryWithoutEmf = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.logTrails"), config.logTrails)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.logTrails.@Tooltip"))
                .setSaveConsumer(newValue -> config.logTrails = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.shaderOverride"), config.alwaysGlowWhenShaderTranslucent)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.shaderOverride.@Tooltip"))
                .setSaveConsumer(newValue -> config.alwaysGlowWhenShaderTranslucent = newValue)
                .requireRestart()
                .build());
        general.addEntry(entryBuilder.startEnumSelector(
                        Component.translatable("text.elytratrails.option.clearTrails"),
                        ModConfig.ClearTrails.class,
                        config.clearTrailsOption)
                .setTooltip(Component.translatable("text.elytratrails.option.clearTrails.@Tooltip"))
                .setSaveConsumer(newValue -> {
                    if(newValue == ModConfig.ClearTrails.CLEAR)
                        TrailSystem.getTrailManager().removeAllTrails();
                })
                .build());

        twirling.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.twirling.desc"))
                .build());
        twirling.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableTwirls"), config.enableTwirls)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.enableTwirls.@Tooltip"))
                .setSaveConsumer(newValue -> config.enableTwirls = newValue)
                .build());
        twirling.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.alwaysShowTrailDuringTwirl"), config.clientPlayerConfig.alwaysShowTrailDuringTwirl)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.alwaysShowTrailDuringTwirl.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.alwaysShowTrailDuringTwirl = newValue)
                .build());
        twirling.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.firstPersonTwirlCamera"), config.fishysStupidCameraRoll)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.firstPersonTwirlCamera.@Tooltip"))
                .setSaveConsumer(newValue -> config.fishysStupidCameraRoll = newValue)
                .build());
        twirling.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.thirdPersonTwirlCamera"), config.fishysStupidThirdPersonCameraRoll)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.thirdPersonTwirlCamera.@Tooltip"))
                .setSaveConsumer(newValue -> config.fishysStupidThirdPersonCameraRoll = newValue)
                .build());
        twirling.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.twirlTime"), config.clientPlayerConfig.twirlTime)
                .setDefaultValue(defaultConfig.twirlTime)
                .setTooltip(Component.translatable("text.elytratrails.option.twirlTime.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.twirlTime = newValue)
                .build());
        twirling.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.inputBuffer"), config.inputBuffer)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.inputBuffer.@Tooltip"))
                .setSaveConsumer(newValue -> config.inputBuffer = newValue)
                .build());

        twirling.addEntry(entryBuilder.startEnumSelector(
                        Component.translatable("text.elytratrails.option.easeType"),
                        EasingUtil.EaseType.class,
                        config.clientPlayerConfig.easeType)
                .setDefaultValue(defaultConfig.easeType)
                .setTooltip(Component.translatable("text.elytratrails.option.easeType.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.easeType = newValue)
                .build());

        keybinds.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.keybinds.desc"))
                .build());
        keybinds.addEntry(entryBuilder.startKeyCodeField(
                        Component.translatable("text.elytratrails.option.twirl_r_continuous_key"),
                        ElytraTrailsKeybind.DO_A_LIL_CONTINUOUS_TWIRL_R.key
                )
                .setDefaultValue(ElytraTrailsKeybind.DO_A_LIL_CONTINUOUS_TWIRL_R.getDefaultKey())
                .setTooltip(Component.translatable("text.elytratrails.option.twirl_r_continuous_key.@Tooltip"))
                .setKeySaveConsumer(newKey -> {
                    ElytraTrailsKeybind.DO_A_LIL_CONTINUOUS_TWIRL_R.setKey(newKey);
                    KeyMapping.resetMapping();
                })
                .build());
        keybinds.addEntry(entryBuilder.startKeyCodeField(
                        Component.translatable("text.elytratrails.option.twirl_l_continuous_key"),
                        ElytraTrailsKeybind.DO_A_LIL_CONTINUOUS_TWIRL_L.key
                )
                .setDefaultValue(ElytraTrailsKeybind.DO_A_LIL_CONTINUOUS_TWIRL_L.getDefaultKey())
                .setTooltip(Component.translatable("text.elytratrails.option.twirl_l_continuous_key.@Tooltip"))
                .setKeySaveConsumer(newKey -> {
                    ElytraTrailsKeybind.DO_A_LIL_CONTINUOUS_TWIRL_L.setKey(newKey);
                    KeyMapping.resetMapping();
                })
                .build());

        keybinds.addEntry(entryBuilder.startKeyCodeField(
                        Component.translatable("text.elytratrails.option.twirl_r_key"),
                        ElytraTrailsKeybind.DO_A_LIL_TWIRL_R.key
                )
                .setDefaultValue(ElytraTrailsKeybind.DO_A_LIL_TWIRL_R.getDefaultKey())
                .setTooltip(Component.translatable("text.elytratrails.option.twirl_r_key.@Tooltip"))
                .setKeySaveConsumer(newKey -> {
                    ElytraTrailsKeybind.DO_A_LIL_TWIRL_R.setKey(newKey);
                    KeyMapping.resetMapping();
                })
                .build());
        keybinds.addEntry(entryBuilder.startKeyCodeField(
                        Component.translatable("text.elytratrails.option.twirl_l_key"),
                        ElytraTrailsKeybind.DO_A_LIL_TWIRL_L.key
                )
                .setDefaultValue(ElytraTrailsKeybind.DO_A_LIL_TWIRL_L.getDefaultKey())
                .setTooltip(Component.translatable("text.elytratrails.option.twirl_l_key.@Tooltip"))
                .setKeySaveConsumer(newKey -> {
                    ElytraTrailsKeybind.DO_A_LIL_TWIRL_L.setKey(newKey);
                    KeyMapping.resetMapping();
                })
                .build());
        keybinds.addEntry(entryBuilder.startKeyCodeField(
                        Component.translatable("text.elytratrails.option.twirl_random_key"),
                        ElytraTrailsKeybind.DO_A_LIL_TWIRL_RANDOM.key
                )
                .setDefaultValue(ElytraTrailsKeybind.DO_A_LIL_TWIRL_RANDOM.getDefaultKey())
                .setTooltip(Component.translatable("text.elytratrails.option.twirl_random_key.@Tooltip"))
                .setKeySaveConsumer(newKey -> {
                    ElytraTrailsKeybind.DO_A_LIL_TWIRL_RANDOM.setKey(newKey);
                    KeyMapping.resetMapping();
                })
                .build());
        keybinds.addEntry(entryBuilder.startKeyCodeField(
                        Component.translatable("text.elytratrails.option.toggle_trails_key"),
                        ElytraTrailsKeybind.TOGGLE_TRAILS.key
                )
                .setDefaultValue(ElytraTrailsKeybind.TOGGLE_TRAILS.getDefaultKey())
                .setTooltip(Component.translatable("text.elytratrails.option.toggle_trails_key.@Tooltip"))
                .setKeySaveConsumer(newKey -> {
                    ElytraTrailsKeybind.TOGGLE_TRAILS.setKey(newKey);
                    KeyMapping.resetMapping();
                })
                .build());
        keybinds.addEntry(entryBuilder.startKeyCodeField(
                        Component.translatable("text.elytratrails.option.open_settings_key"),
                        ElytraTrailsKeybind.OPEN_SETTINGS.key
                )
                .setDefaultValue(ElytraTrailsKeybind.OPEN_SETTINGS.getDefaultKey())
                .setTooltip(Component.translatable("text.elytratrails.option.open_settings_key.@Tooltip"))
                .setKeySaveConsumer(newKey -> {
                    ElytraTrailsKeybind.OPEN_SETTINGS.setKey(newKey);
                    KeyMapping.resetMapping();
                })
                .build());



        elytra.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.elytra.desc"))
                .build());

        addSharedTrailEntries(
                elytra,
                entryBuilder,
                config.clientPlayerConfig,
                defaultConfig,
                advanced,
                ""
        );

        elytra.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.elytra.desc.export"))
                .build());

        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.export"), config.exportPreset)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.export.@Tooltip"))
                .setSaveConsumer(newValue -> config.exportPreset = newValue)
                .build());

        elytra.addEntry(entryBuilder.startStrField(Component.translatable("text.elytratrails.option.exportPresetName"), config.exportPresetName)
                .setDefaultValue("")
                .setTooltip(Component.translatable("text.elytratrails.option.exportPresetName.@Tooltip"))
                .setSaveConsumer(newValue -> config.exportPresetName = newValue)
                .build());



        server.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.server.desc"))
                .build());
        server.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.syncWithServer"), config.syncWithServer)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.syncWithServer.@Tooltip"))
                .setSaveConsumer(newValue -> config.syncWithServer = newValue)
                .build());
        server.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.shareTrail"), config.shareTrail)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.shareTrail.@Tooltip[0]"),Component.translatable("text.elytratrails.option.shareTrail.@Tooltip[1]"))
                .setSaveConsumer(newValue -> config.shareTrail = newValue)
                .build());
        server.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.showTrailToOtherPlayers"), config.showTrailToOtherPlayers)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.showTrailToOtherPlayers.@Tooltip"))
                .setSaveConsumer(newValue -> config.showTrailToOtherPlayers = newValue)
                .build());
        server.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.maxOnlineWidth"), config.maxOnlineWidth)
                .setDefaultValue(5.0)
                .setTooltip(Component.translatable("text.elytratrails.option.maxOnlineWidth.@Tooltip"))
                .setSaveConsumer(newValue -> config.maxOnlineWidth = newValue)
                .build());
        server.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.maxOnlineLifetime"), config.maxOnlineLifetime)
                .setDefaultValue(120.0)
                .setTooltip(Component.translatable("text.elytratrails.option.maxOnlineLifetime.@Tooltip"))
                .setSaveConsumer(newValue -> config.maxOnlineLifetime = newValue)
                .build());

        others.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.others.desc"))
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.useSameDefaultsforOthers"), config.useSameDefaultsForOthers)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.useSameDefaultsforOthers.@Tooltip"))
                .setSaveConsumer(newValue -> config.useSameDefaultsForOthers = newValue)
                .build());

        addSharedTrailEntries(
                others,
                entryBuilder,
                config.otherPlayerConfig,
                defaultConfig,
                advancedOthers,
                "OthersDefault"
        );

        particles.addEntry(entryBuilder.startDropdownMenu(
                        Component.translatable("text.elytratrails.option.particle"),
                        DropdownMenuBuilder.TopCellElementBuilder.of(
                                config.particle,
                                s -> {
                                    try {
                                        return ModConfig.ParticleChoice.valueOf(s.trim().toUpperCase(Locale.ROOT));
                                    } catch (IllegalArgumentException ignored) {
                                        return null;
                                    }
                                },
                                v -> Component.nullToEmpty(v.name())
                        ),
                        DropdownMenuBuilder.CellCreatorBuilder.of(14,160, 8, v -> Component.nullToEmpty((v).name())
                        )
                )
                .setDefaultValue(ModConfig.ParticleChoice.POOF)
                .setTooltip(Component.translatable("text.elytratrails.option.particle.@Tooltip"))
                .setSelections(Arrays.stream(ModConfig.ParticleChoice.values()).collect(Collectors.toSet()))
                .setSaveConsumer(newValue -> config.particle = newValue)
                .build());

        particles.addEntry(entryBuilder.startIntField(Component.translatable("text.elytratrails.option.particleSpawnsPerTick"), config.particleSpawnsPerTick)
                .setDefaultValue(3)
                .setTooltip(Component.translatable("text.elytratrails.option.particleSpawnsPerTick.@Tooltip"))
                .setSaveConsumer(newValue -> config.particleSpawnsPerTick = newValue)
                .build());
        particles.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.particlesBlockRadius"), config.particlesBlockRadius)
                .setDefaultValue(10.0)
                .setTooltip(Component.translatable("text.elytratrails.option.particlesBlockRadius.@Tooltip"))
                .setSaveConsumer(newValue -> config.particlesBlockRadius = newValue)
                .build());
        particles.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.particlesVelocityAhead"), config.particlesVelocityAhead)
                .setDefaultValue(3.0)
                .setTooltip(Component.translatable("text.elytratrails.option.particlesVelocityAhead.@Tooltip"))
                .setSaveConsumer(newValue -> config.particlesVelocityAhead = newValue)
                .build());
        particles.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.particlesVelocityBackwards"), config.particlesVelocityBackwards)
                .setDefaultValue(0)
                .setTooltip(Component.translatable("text.elytratrails.option.particlesVelocityBackwards.@Tooltip"))
                .setSaveConsumer(newValue -> config.particlesVelocityBackwards = newValue)
                .build());

        presets.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.presets.desc"))
                .build());
        presets.addEntry(
                entryBuilder.startDropdownMenu(
                                Component.translatable("text.elytratrails.option.Preset"),
                                config.Preset,
                                s -> s,
                                Component::nullToEmpty
                        )
                        .setSelections(presetNames)
                        .setDefaultValue(presetDefault)
                        .setTooltip(Component.translatable("text.elytratrails.option.Preset.@Tooltip"))
                        .setSaveConsumer(newValue -> config.Preset = newValue)
                        .build()
        );
        presets.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.presets.desc.others"))
                .build());
        presets.addEntry(
                entryBuilder.startDropdownMenu(
                                Component.translatable("text.elytratrails.option.PresetOthers"),
                                config.PresetOthers,
                                s -> s,
                                Component::nullToEmpty
                        )
                        .setSelections(presetNames)
                        .setDefaultValue(presetDefault)
                        .setTooltip(Component.translatable("text.elytratrails.option.PresetOthers.@Tooltip"))
                        .setSaveConsumer(newValue -> config.PresetOthers = newValue)
                        .build()
        );

        builder.setSavingRunnable(() -> {
            encodeConfigColors(config.clientPlayerConfig);
            encodeConfigColors(config.otherPlayerConfig);
            exportPreset(config);
            applyPresetsToConfig(config);
            refreshConfig();
            TwirlController.setDurations();
            ContinuousTwirlController.setDurations();
            AutoConfig.getConfigHolder(ModConfig.class).save();
                    var mc = Minecraft.getInstance();
                    refreshLocalConfigs();
                    if (mc.getConnection() != null && mc.player != null && mc.level != null) {
                        TrailSystem.getTrailManager().removeTrail(mc.player.getId());
                        TrailSystem.getWingtipSampler().removeAllEmfCache();

                        if(getConfig().shareTrail || !getConfig().showTrailToOtherPlayers)
                        {
                            ClientPlayNetworking.send(new PlayerConfigC2SPayload(getLocalPlayerConfigToSend().toTag()));
                        }
                        else
                        {
                            ClientPlayNetworking.send(new RemoveFromStoreC2SPayload());
                        }
                        if (!getConfig().syncWithServer) {
                            CLIENT_PLAYER_CONFIGS.clear();
                        } else if (CLIENT_PLAYER_CONFIGS.isEmpty()) {
                            ClientPlayNetworking.send(new GetAllRequestC2SPayload());
                        }
                    }
        });
        return builder.build();
    }
    private static void exportPreset(ModConfig config)
    {
        if(config.exportPreset)
        {
            exportTrailPresetToDisk(config.exportPresetName,config);
        }
        config.exportPresetName = "";
        config.exportPreset = false;
    }
    private static int encodeColor(int alpha, int color)
    {
        return withAlphaAndColor(alpha,color);
    }
    private static int decodeColors(int color)
    {
        return TrailPackConfigManager.getColorRgb(color);
    }
    private static int decodeAlpha(int color)
    {
        return TrailPackConfigManager.getAlpha(color);
    }
    private  static void applyPresetsToConfig(ModConfig config)
    {

        if(!config.PresetOthers.isEmpty())
        {
            if(config.PresetOthers.equals("default"))
            {
                config.otherPlayerConfig = ClientConfig.getDefaultClientConfig();
            }
            else
            {
                TrailPackConfigManager.applyPreset(false, config.PresetOthers,config);
            }
            decodeConfigColors(config.otherPlayerConfig);
        }
        if(!config.Preset.isEmpty())
        {
            if(config.Preset.equals("default"))
            {
                config.clientPlayerConfig = ClientConfig.getDefaultClientConfig();
            }
            else
            {
                TrailPackConfigManager.applyPreset(true, config.Preset,config);
            }
            decodeConfigColors(config.clientPlayerConfig);
        }
        config.Preset = "";
        config.PresetOthers = "";
    }
    private static void decodeConfigColors(ClientConfig config)
    {
        config.justColor = decodeColors(config.color);
        config.justAlpha  = decodeAlpha(config.color);
        config.justColorRight = decodeColors(config.colorRight);
        config.justAlphaRight  = decodeAlpha(config.colorRight);
    }
    private static void encodeConfigColors(ClientConfig config)
    {
        config.color = encodeColor(config.justAlpha, config.justColor);
        config.colorRight = encodeColor(config.justAlphaRight, config.justColorRight);
    }
    private static void addSharedTrailEntries(
            ConfigCategory category,
            ConfigEntryBuilder entryBuilder,
            ClientConfig targetConfig,
            ClientConfig defaultConfig,
            SubCategoryBuilder advancedOptions,
            String suffix
    ) {

        category.addEntry(entryBuilder.startBooleanToggle(option("enableTrail", suffix), targetConfig.enableTrail)
                .setDefaultValue(defaultConfig.enableTrail)
                .setTooltip(tooltip("enableTrail", suffix))
                .setSaveConsumer(newValue -> targetConfig.enableTrail = newValue)
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(option("translucentTrails", suffix), targetConfig.translucentTrails)
                .setDefaultValue(defaultConfig.translucentTrails)
                .setTooltip(tooltip("translucentTrails", suffix))
                .setSaveConsumer(newValue -> targetConfig.translucentTrails = newValue)
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(option("glowingTrails", suffix), targetConfig.glowingTrails)
                .setDefaultValue(defaultConfig.glowingTrails)
                .setTooltip(tooltip("glowingTrails", suffix))
                .setSaveConsumer(newValue -> targetConfig.glowingTrails = newValue)
                .build());

        advancedOptions.add(entryBuilder.startBooleanToggle(option("wireframeTrails", suffix), targetConfig.wireframeTrails)
                .setDefaultValue(defaultConfig.wireframeTrails)
                .setTooltip(tooltip("wireframeTrails", suffix))
                .setSaveConsumer(newValue -> targetConfig.wireframeTrails = newValue)
                .build());
        category.addEntry(entryBuilder.startBooleanToggle(option("enableRandomWidth", suffix), targetConfig.enableRandomWidth)
                .setDefaultValue(defaultConfig.enableRandomWidth)
                .setTooltip(tooltip("enableRandomWidth", suffix))
                .setSaveConsumer(newValue -> targetConfig.enableRandomWidth = newValue)
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(option("speedDependentTrail", suffix), targetConfig.speedDependentTrail)
                .setDefaultValue(defaultConfig.speedDependentTrail)
                .setTooltip(tooltip("speedDependentTrail", suffix))
                .setSaveConsumer(newValue -> targetConfig.speedDependentTrail = newValue)
                .build());

        advancedOptions.add(entryBuilder.startBooleanToggle(option("trailMovesWithElytraAngle", suffix), targetConfig.trailMovesWithElytraAngle)
                .setDefaultValue(defaultConfig.trailMovesWithElytraAngle)
                .setTooltip(tooltip("trailMovesWithElytraAngle", suffix))
                .setSaveConsumer(newValue -> targetConfig.trailMovesWithElytraAngle = newValue)
                .build());

        advancedOptions.add(entryBuilder.startBooleanToggle(option("trailMovesWithAngleOfAttack", suffix), targetConfig.trailMovesWithAngleOfAttack)
                .setDefaultValue(defaultConfig.trailMovesWithAngleOfAttack)
                .setTooltip(tooltip("trailMovesWithAngleOfAttack", suffix))
                .setSaveConsumer(newValue -> targetConfig.trailMovesWithAngleOfAttack = newValue)
                .build());
        advancedOptions.add(entryBuilder.startDoubleField(option("wingtipVerticalPosition", suffix), targetConfig.wingtipVerticalPosition)
                .setDefaultValue(defaultConfig.wingtipVerticalPosition)
                .setTooltip(tooltip("wingtipVerticalPosition", suffix))
                .setSaveConsumer(newValue -> targetConfig.wingtipVerticalPosition = newValue)
                .build());
        advancedOptions.add(entryBuilder.startDoubleField(option("wingtipHorizontalPosition", suffix), targetConfig.wingtipHorizontalPosition)
                .setDefaultValue(defaultConfig.wingtipHorizontalPosition)
                .setTooltip(tooltip("wingtipHorizontalPosition", suffix))
                .setSaveConsumer(newValue -> targetConfig.wingtipHorizontalPosition = newValue)
                .build());
        advancedOptions.add(entryBuilder.startDoubleField(option("wingtipDepthPosition", suffix), targetConfig.wingtipDepthPosition)
                .setDefaultValue(defaultConfig.wingtipDepthPosition)
                .setTooltip(tooltip("wingtipDepthPosition", suffix))
                .setSaveConsumer(newValue -> targetConfig.wingtipDepthPosition = newValue)
                .build());
        category.addEntry(entryBuilder.startDoubleField(option("width", suffix), targetConfig.maxWidth)
                .setDefaultValue(defaultConfig.maxWidth)
                .setTooltip(tooltip("width", suffix))
                .setSaveConsumer(newValue -> targetConfig.maxWidth = newValue)
                .build());

        category.addEntry(entryBuilder.startDoubleField(option("trailLifetime", suffix), targetConfig.trailLifetime)
                .setDefaultValue(defaultConfig.trailLifetime)
                .setTooltip(tooltip("trailLifetime", suffix))
                .setSaveConsumer(newValue -> targetConfig.trailLifetime = newValue)
                .build());


        advancedOptions.add(entryBuilder.startDoubleField(option("startRampDistance", suffix), targetConfig.startRampDistance)
                .setDefaultValue(defaultConfig.startRampDistance)
                .setTooltip(tooltip("startRampDistance", suffix))
                .setSaveConsumer(newValue -> targetConfig.startRampDistance = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("endRampDistance", suffix), targetConfig.endRampDistance)
                .setDefaultValue(defaultConfig.endRampDistance)
                .setTooltip(tooltip("endRampDistance", suffix))
                .setSaveConsumer(newValue -> targetConfig.endRampDistance = newValue)
                .build());

        category.addEntry(entryBuilder.startColorField(option("color", suffix), targetConfig.justColor)
                .setDefaultValue(0xFFFFFF)
                .setTooltip(tooltip("color", suffix))
                .setSaveConsumer(newValue -> targetConfig.justColor = newValue)
                .build());

        category.addEntry(entryBuilder.startIntField(option("alpha", suffix), targetConfig.justAlpha)
                .setDefaultValue(255)
                .setTooltip(tooltip("alpha", suffix))
                .setSaveConsumer(newValue -> targetConfig.justAlpha = newValue)
                .build());

        advancedOptions.add(entryBuilder.startBooleanToggle(option("useColorBoth", suffix), targetConfig.useColorBoth)
                .setDefaultValue(defaultConfig.useColorBoth)
                .setTooltip(tooltip("useColorBoth", suffix))
                .setSaveConsumer(newValue -> targetConfig.useColorBoth = newValue)
                .build());

        advancedOptions.add(entryBuilder.startColorField(option("colorRight", suffix), targetConfig.justColorRight)
                .setDefaultValue(0xFFFFFF)
                .setTooltip(tooltip("colorRight", suffix))
                .setSaveConsumer(newValue -> targetConfig.justColorRight = newValue)
                .build());

        advancedOptions.add(entryBuilder.startIntField(option("alphaRight", suffix), targetConfig.justAlphaRight)
                .setDefaultValue(255)
                .setTooltip(tooltip("alphaRight", suffix))
                .setSaveConsumer(newValue -> targetConfig.justAlphaRight = newValue)
                .build());

        category.addEntry(entryBuilder.startStrField(option("prideTrail", suffix), targetConfig.prideTrail)
                .setDefaultValue(defaultConfig.prideTrail)
                .setTooltip(tooltip("prideTrail", suffix))
                .setSaveConsumer(newValue -> targetConfig.prideTrail = newValue)
                .build());

        category.addEntry(entryBuilder.startStrField(option("prideTrailRight", suffix), targetConfig.prideTrailRight)
                .setDefaultValue(defaultConfig.prideTrailRight)
                .setTooltip(tooltip("prideTrailRight", suffix))
                .setSaveConsumer(newValue -> targetConfig.prideTrailRight = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("randomWidthVariation", suffix), targetConfig.randomWidthVariation)
                .setDefaultValue(defaultConfig.randomWidthVariation)
                .setTooltip(tooltip("randomWidthVariation", suffix))
                .setSaveConsumer(newValue -> targetConfig.randomWidthVariation = newValue)
                .build());


        advancedOptions.add(entryBuilder.startBooleanToggle(option("fadeStart", suffix), targetConfig.fadeStart)
                .setDefaultValue(defaultConfig.fadeStart)
                .setTooltip(tooltip("fadeStart", suffix))
                .setSaveConsumer(newValue -> targetConfig.fadeStart = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("fadeStartDistance", suffix), targetConfig.fadeStartDistance)
                .setDefaultValue(defaultConfig.fadeStartDistance)
                .setTooltip(tooltip("fadeStartDistance", suffix))
                .setSaveConsumer(newValue -> targetConfig.fadeStartDistance = newValue)
                .build());

        advancedOptions.add(entryBuilder.startBooleanToggle(option("endDistanceFade", suffix), targetConfig.endDistanceFade)
                .setDefaultValue(defaultConfig.endDistanceFade)
                .setTooltip(tooltip("endDistanceFade", suffix))
                .setSaveConsumer(newValue -> targetConfig.endDistanceFade = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("endDistanceFadeAmount", suffix), targetConfig.endDistanceFadeAmount)
                .setDefaultValue(defaultConfig.endDistanceFadeAmount)
                .setTooltip(tooltip("endDistanceFadeAmount", suffix))
                .setSaveConsumer(newValue -> targetConfig.endDistanceFadeAmount = newValue)
                .build());

        advancedOptions.add(entryBuilder.startBooleanToggle(option("fadeEnd", suffix), targetConfig.fadeEnd)
                .setDefaultValue(defaultConfig.fadeEnd)
                .setTooltip(tooltip("fadeEnd", suffix))
                .setSaveConsumer(newValue -> targetConfig.fadeEnd = newValue)
                .build());


        advancedOptions.add(entryBuilder.startBooleanToggle(option("increaseWidthOverTime", suffix), targetConfig.increaseWidthOverTime)
                .setDefaultValue(defaultConfig.increaseWidthOverTime)
                .setTooltip(tooltip("increaseWidthOverTime", suffix))
                .setSaveConsumer(newValue -> targetConfig.increaseWidthOverTime = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("startingWidthMultiplier", suffix), targetConfig.startingWidthMultiplier)
                .setDefaultValue(defaultConfig.startingWidthMultiplier)
                .setTooltip(tooltip("startingWidthMultiplier", suffix))
                .setSaveConsumer(newValue -> targetConfig.startingWidthMultiplier = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("endingWidthMultiplier", suffix), targetConfig.endingWidthMultiplier)
                .setDefaultValue(defaultConfig.endingWidthMultiplier)
                .setTooltip(tooltip("endingWidthMultiplier", suffix))
                .setSaveConsumer(newValue -> targetConfig.endingWidthMultiplier = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("distanceTillTrailStart", suffix), targetConfig.distanceTillTrailStart)
                .setDefaultValue(defaultConfig.distanceTillTrailStart)
                .setTooltip(tooltip("distanceTillTrailStart", suffix))
                .setSaveConsumer(newValue -> targetConfig.distanceTillTrailStart = newValue)
                .build());
        advancedOptions.add(entryBuilder.startDoubleField(option("distanceTillTrailEnd", suffix), targetConfig.distanceTillTrailEnd)
                .setDefaultValue(defaultConfig.distanceTillTrailEnd)
                .setTooltip(tooltip("distanceTillTrailEnd", suffix))
                .setSaveConsumer(newValue -> targetConfig.distanceTillTrailEnd = newValue)
                .build());

        category.addEntry(entryBuilder.startDoubleField(option("trailMinSpeed", suffix), targetConfig.trailMinSpeed)
                .setDefaultValue(defaultConfig.trailMinSpeed)
                .setTooltip(tooltip("trailMinSpeed", suffix))
                .setSaveConsumer(newValue -> targetConfig.trailMinSpeed = newValue)
                .build());
        advancedOptions.add(entryBuilder.startBooleanToggle(option("speedBasedAlpha", suffix), targetConfig.speedBasedAlpha)
                .setDefaultValue(defaultConfig.speedBasedAlpha)
                .setTooltip(tooltip("speedBasedAlpha", suffix))
                .setSaveConsumer(newValue -> targetConfig.speedBasedAlpha = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("minAlphaSpeed", suffix), targetConfig.minAlphaSpeed)
                .setDefaultValue(defaultConfig.minAlphaSpeed)
                .setTooltip(tooltip("minAlphaSpeed", suffix))
                .setSaveConsumer(newValue -> targetConfig.minAlphaSpeed = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("maxAlphaSpeed", suffix), targetConfig.maxAlphaSpeed)
                .setDefaultValue(defaultConfig.maxAlphaSpeed)
                .setTooltip(tooltip("maxAlphaSpeed", suffix))
                .setSaveConsumer(newValue -> targetConfig.maxAlphaSpeed = newValue)
                .build());

        advancedOptions.add(entryBuilder.startBooleanToggle(option("speedBasedWidth", suffix), targetConfig.speedBasedWidth)
                .setDefaultValue(defaultConfig.speedBasedWidth)
                .setTooltip(tooltip("speedBasedWidth", suffix))
                .setSaveConsumer(newValue -> targetConfig.speedBasedWidth = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("minWidthSpeed", suffix), targetConfig.minWidthSpeed)
                .setDefaultValue(defaultConfig.minWidthSpeed)
                .setTooltip(tooltip("minWidthSpeed", suffix))
                .setSaveConsumer(newValue -> targetConfig.minWidthSpeed = newValue)
                .build());

        advancedOptions.add(entryBuilder.startDoubleField(option("maxWidthSpeed", suffix), targetConfig.maxWidthSpeed)
                .setDefaultValue(defaultConfig.maxWidthSpeed)
                .setTooltip(tooltip("maxWidthSpeed", suffix))
                .setSaveConsumer(newValue -> targetConfig.maxWidthSpeed = newValue)
                .build());
        category.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.advanced"))
                .build());
        category.addEntry(advancedOptions.build());
    }

    private static Component option(String baseKey, String suffix) {
        return Component.translatable("text.elytratrails.option." + baseKey + suffix);
    }

    private static Component tooltip(String baseKey, String suffix) {
        return Component.translatable("text.elytratrails.option." + baseKey + suffix + ".@Tooltip");
    }
}