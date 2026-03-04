package dbrighthd.elytratrails.config;

import dbrighthd.elytratrails.ElytraTrailsKeybind;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.controller.ContinuousTwirlController;
import dbrighthd.elytratrails.controller.EasingUtil;
import dbrighthd.elytratrails.controller.TwirlController;
import dbrighthd.elytratrails.network.GetAllRequestC2SPayload;
import dbrighthd.elytratrails.network.PlayerConfigC2SPayload;
import dbrighthd.elytratrails.network.RemoveFromStoreC2SPayload;
import dbrighthd.elytratrails.rendering.TrailSystem;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.config.pack.TrailPackConfigManager.*;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.*;

public class ConfigScreenBuilder {

    public static Screen buildConfigScreen(Screen parent, ModConfig config)
    {
        config.clientPlayerConfig.justColor = decodeColors(config.clientPlayerConfig.color);
        config.clientPlayerConfig.justAlpha  = decodeAlpha(config.clientPlayerConfig.color);
        config.otherPlayerConfig.justColor = decodeColors(config.otherPlayerConfig.color);
        config.otherPlayerConfig.justAlpha = decodeAlpha(config.otherPlayerConfig.color);



        TrailPackConfigManager.reloadDiskPresets();
        String presetDefault = "";
        config.Preset = presetDefault;
        config.PresetOthers = presetDefault;
        List<String> presetNames = new ArrayList<>(TrailPackConfigManager.getPresets().keySet());
        Collections.sort(presetNames);
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



        general.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.general.desc"))
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableAllTrails"), config.enableAllTrails)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.enableAllTrails.@Tooltip"))
                .setSaveConsumer(newValue -> config.enableAllTrails = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.emfSupport"), config.emfSupport)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.emfSupport.@Tooltip"))
                .setSaveConsumer(newValue -> config.emfSupport = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.alwaysSnapTrail"), config.alwaysSnapTrail)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.alwaysSnapTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.alwaysSnapTrail = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.resourcePackOverride"), config.resourcePackOverride)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.resourcePackOverride.@Tooltip"))
                .setSaveConsumer(newValue -> config.resourcePackOverride = newValue)
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
        general.addEntry(entryBuilder.startKeyCodeField(
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
                .setDefaultValue(0.5)
                .setTooltip(Component.translatable("text.elytratrails.option.twirlTime.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.twirlTime = newValue)
                .build());
        twirling.addEntry(entryBuilder.startEnumSelector(
                        Component.translatable("text.elytratrails.option.easeType"),
                        EasingUtil.EaseType.class,
                        config.clientPlayerConfig.easeType)
                .setDefaultValue(EasingUtil.EaseType.Sine)
                .setTooltip(Component.translatable("text.elytratrails.option.easeType.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.easeType = newValue)
                .build());

        twirling.addEntry(entryBuilder.startKeyCodeField(
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
        twirling.addEntry(entryBuilder.startKeyCodeField(
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

        twirling.addEntry(entryBuilder.startKeyCodeField(
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
        twirling.addEntry(entryBuilder.startKeyCodeField(
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

        twirling.addEntry(entryBuilder.startKeyCodeField(
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



        elytra.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.elytra.desc"))
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableTrail"), config.clientPlayerConfig.enableTrail)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().enableTrail)
                .setTooltip(Component.translatable("text.elytratrails.option.enableTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.enableTrail = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.translucentTrails"), config.clientPlayerConfig.translucentTrails)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().translucentTrails)
                .setTooltip(Component.translatable("text.elytratrails.option.translucentTrails.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.translucentTrails = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.glowingTrails"), config.clientPlayerConfig.glowingTrails)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().glowingTrails)
                .setTooltip(Component.translatable("text.elytratrails.option.glowingTrails.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.glowingTrails = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableRandomWidth"), config.clientPlayerConfig.enableRandomWidth)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().enableRandomWidth)
                .setTooltip(Component.translatable("text.elytratrails.option.enableRandomWidth.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.enableRandomWidth = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.speedDependentTrail"), config.clientPlayerConfig.speedDependentTrail)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().speedDependentTrail)
                .setTooltip(Component.translatable("text.elytratrails.option.speedDependentTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.speedDependentTrail = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeFirstPersonTrail"), config.fadeFirstPersonTrail)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeFirstPersonTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.fadeFirstPersonTrail = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.trailMovesWithElytraAngle"), config.clientPlayerConfig.trailMovesWithElytraAngle)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().trailMovesWithElytraAngle)
                .setTooltip(Component.translatable("text.elytratrails.option.trailMovesWithElytraAngle.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.trailMovesWithElytraAngle = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.width"), config.clientPlayerConfig.maxWidth)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().maxWidth)
                .setTooltip(Component.translatable("text.elytratrails.option.width.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.maxWidth = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.firstPersonFadeTime"), config.firstPersonFadeTime)
                .setDefaultValue(0.2)
                .setTooltip(Component.translatable("text.elytratrails.option.firstPersonFadeTime.@Tooltip"))
                .setSaveConsumer(newValue -> config.firstPersonFadeTime = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.trailLifetime"), config.clientPlayerConfig.trailLifeTime)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().trailLifeTime)
                .setTooltip(Component.translatable("text.elytratrails.option.trailLifetime.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.trailLifeTime = newValue)
                .build());
        elytra.addEntry(entryBuilder.startIntField(Component.translatable("text.elytratrails.option.maxSamplePerSecond"), config.maxSamplePerSecond)
                .setDefaultValue(60)
                .setTooltip(Component.translatable("text.elytratrails.option.maxSamplePerSecond.@Tooltip"))
                .setSaveConsumer(newValue -> config.maxSamplePerSecond = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.trailMinSpeed"), config.clientPlayerConfig.trailMinSpeed)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().trailMinSpeed)
                .setTooltip(Component.translatable("text.elytratrails.option.trailMinSpeed.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.trailMinSpeed = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.startRampDistance"), config.clientPlayerConfig.startRampDistance)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().startRampDistance)
                .setTooltip(Component.translatable("text.elytratrails.option.startRampDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.startRampDistance = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.endRampDistance"), config.clientPlayerConfig.endRampDistance)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().endRampDistance)
                .setTooltip(Component.translatable("text.elytratrails.option.endRampDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.endRampDistance = newValue)
                .build());
        elytra.addEntry(entryBuilder.startColorField(Component.translatable("text.elytratrails.option.color"), config.clientPlayerConfig.justColor)
                .setDefaultValue(0xFFFFFF)
                .setTooltip(Component.translatable("text.elytratrails.option.color.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.justColor = newValue)
                .build());
        elytra.addEntry(entryBuilder.startIntField(Component.translatable("text.elytratrails.option.alpha"), config.clientPlayerConfig.justAlpha)
                .setDefaultValue(255)
                .setTooltip(Component.translatable("text.elytratrails.option.alpha.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.justAlpha = newValue)
                .build());
        elytra.addEntry(entryBuilder.startStrField(Component.translatable("text.elytratrails.option.prideTrail"), config.clientPlayerConfig.prideTrail)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().prideTrail)
                .setTooltip(Component.translatable("text.elytratrails.option.prideTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.prideTrail = newValue)
                .build());
        elytra.addEntry(entryBuilder.startStrField(Component.translatable("text.elytratrails.option.prideTrailRight"), config.clientPlayerConfig.prideTrailRight)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().prideTrailRight)
                .setTooltip(Component.translatable("text.elytratrails.option.prideTrailRight.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.prideTrailRight = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.randomWidthVariation"), config.clientPlayerConfig.randomWidthVariation)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().randomWidthVariation)
                .setTooltip(Component.translatable("text.elytratrails.option.randomWidthVariation.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.randomWidthVariation = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeStart"), config.clientPlayerConfig.fadeStart)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().fadeStart)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeStart.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.fadeStart = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.fadeStartDistance"), config.clientPlayerConfig.fadeStartDistance)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().fadeStartDistance)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeStartDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.fadeStartDistance = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeEnd"), config.clientPlayerConfig.fadeEnd)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().fadeEnd)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeEnd.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.fadeEnd = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.wireframeTrails"), config.clientPlayerConfig.wireframeTrails)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().wireframeTrails)
                .setTooltip(Component.translatable("text.elytratrails.option.wireframeTrails.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.wireframeTrails = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.increaseWidthOverTime"), config.clientPlayerConfig.increaseWidthOverTime)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().increaseWidthOverTime)
                .setTooltip(Component.translatable("text.elytratrails.option.increaseWidthOverTime.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.increaseWidthOverTime = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.startingWidthMultiplier"), config.clientPlayerConfig.startingWidthMultiplier)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().startingWidthMultiplier)
                .setTooltip(Component.translatable("text.elytratrails.option.startingWidthMultiplier.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.startingWidthMultiplier = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.endingWidthMultiplier"), config.clientPlayerConfig.endingWidthMultiplier)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().endingWidthMultiplier)
                .setTooltip(Component.translatable("text.elytratrails.option.endingWidthMultiplier.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.endingWidthMultiplier = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.distanceTillTrailStart"), config.clientPlayerConfig.distanceTillTrailStart)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().distanceTillTrailStart)
                .setTooltip(Component.translatable("text.elytratrails.option.distanceTillTrailStart.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.distanceTillTrailStart = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.endDistanceFade"), config.clientPlayerConfig.endDistanceFade)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().endDistanceFade)
                .setTooltip(Component.translatable("text.elytratrails.option.endDistanceFade.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.endDistanceFade = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.endDistanceFadeAmount"), config.clientPlayerConfig.endDistanceFadeAmount)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().endDistanceFadeAmount)
                .setTooltip(Component.translatable("text.elytratrails.option.endDistanceFadeAmount.@Tooltip"))
                .setSaveConsumer(newValue -> config.clientPlayerConfig.endDistanceFadeAmount = newValue)
                .build());
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
        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableTrailOthersDefault"), config.otherPlayerConfig.enableTrail)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().enableTrail)
                .setTooltip(Component.translatable("text.elytratrails.option.enableTrailOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.enableTrail = newValue)
                .build());
        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.translucentTrailsOthersDefault"), config.otherPlayerConfig.translucentTrails)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().translucentTrails)
                .setTooltip(Component.translatable("text.elytratrails.option.translucentTrailsOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.translucentTrails = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.glowingTrailsOthersDefault"), config.otherPlayerConfig.glowingTrails)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().glowingTrails)
                .setTooltip(Component.translatable("text.elytratrails.option.glowingTrailsOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.glowingTrails = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableRandomWidthOthersDefault"), config.otherPlayerConfig.enableRandomWidth)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().enableRandomWidth)
                .setTooltip(Component.translatable("text.elytratrails.option.enableRandomWidthOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.enableRandomWidth = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.speedDependentTrailOthersDefault"), config.otherPlayerConfig.speedDependentTrail)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().speedDependentTrail)
                .setTooltip(Component.translatable("text.elytratrails.option.speedDependentTrailOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.speedDependentTrail = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.trailMovesWithElytraAngleOthersDefault"), config.otherPlayerConfig.trailMovesWithElytraAngle)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().trailMovesWithElytraAngle)
                .setTooltip(Component.translatable("text.elytratrails.option.trailMovesWithElytraAngleOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.trailMovesWithElytraAngle = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.widthOthersDefault"), config.otherPlayerConfig.maxWidth)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().maxWidth)
                .setTooltip(Component.translatable("text.elytratrails.option.widthOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.maxWidth = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.trailLifetimeOthersDefault"), config.otherPlayerConfig.trailLifeTime)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().trailLifeTime)
                .setTooltip(Component.translatable("text.elytratrails.option.trailLifetimeOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.trailLifeTime = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.trailMinSpeedOthersDefault"), config.otherPlayerConfig.trailMinSpeed)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().trailMinSpeed)
                .setTooltip(Component.translatable("text.elytratrails.option.trailMinSpeedOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.trailMinSpeed = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.startRampDistanceOthersDefault"), config.otherPlayerConfig.startRampDistance)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().startRampDistance)
                .setTooltip(Component.translatable("text.elytratrails.option.startRampDistanceOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.startRampDistance = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.endRampDistanceOthersDefault"), config.otherPlayerConfig.endRampDistance)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().endRampDistance)
                .setTooltip(Component.translatable("text.elytratrails.option.endRampDistanceOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.endRampDistance = newValue)
                .build());

        others.addEntry(entryBuilder.startColorField(Component.translatable("text.elytratrails.option.colorOthersDefault"), config.otherPlayerConfig.justColor)
                .setDefaultValue(0xFFFFFF)
                .setTooltip(Component.translatable("text.elytratrails.option.colorOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.justColor = newValue)
                .build());

        others.addEntry(entryBuilder.startIntField(Component.translatable("text.elytratrails.option.alphaOthersDefault"), config.otherPlayerConfig.justAlpha)
                .setDefaultValue(255)
                .setTooltip(Component.translatable("text.elytratrails.option.alphaOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.justAlpha = newValue)
                .build());

        others.addEntry(entryBuilder.startStrField(Component.translatable("text.elytratrails.option.prideTrailOthersDefault"), config.otherPlayerConfig.prideTrail)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().prideTrail)
                .setTooltip(Component.translatable("text.elytratrails.option.prideTrailOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.prideTrail = newValue)
                .build());
        others.addEntry(entryBuilder.startStrField(Component.translatable("text.elytratrails.option.prideTrailRightOthersDefault"), config.otherPlayerConfig.prideTrailRight)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().prideTrailRight)
                .setTooltip(Component.translatable("text.elytratrails.option.prideTrailRightOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.prideTrailRight = newValue)
                .build());
        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.randomWidthVariationOthersDefault"), config.otherPlayerConfig.randomWidthVariation)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().randomWidthVariation)
                .setTooltip(Component.translatable("text.elytratrails.option.randomWidthVariationOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.randomWidthVariation = newValue)
                .build());
        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeStartOthersDefault"), config.otherPlayerConfig.fadeStart)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().fadeEnd)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeStartOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.fadeStart = newValue)
                .build());
        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.fadeStartDistanceOthersDefault"), config.otherPlayerConfig.fadeStartDistance)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().fadeStartDistance)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeStartDistanceOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.fadeStartDistance = newValue)
                .build());
        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeEndOthersDefault"), config.otherPlayerConfig.fadeEnd)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().fadeEnd)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeEndOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.fadeEnd = newValue)
                .build());
        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.wireframeTrailsOthersDefault"), config.otherPlayerConfig.wireframeTrails)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().wireframeTrails)
                .setTooltip(Component.translatable("text.elytratrails.option.wireframeTrailsOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.wireframeTrails = newValue)
                .build());
        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.increaseWidthOverTimeOthersDefault"), config.otherPlayerConfig.increaseWidthOverTime)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().increaseWidthOverTime)
                .setTooltip(Component.translatable("text.elytratrails.option.increaseWidthOverTimeOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.increaseWidthOverTime = newValue)
                .build());
        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.startingWidthMultiplierOthersDefault"), config.otherPlayerConfig.startingWidthMultiplier)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().startingWidthMultiplier)
                .setTooltip(Component.translatable("text.elytratrails.option.startingWidthMultiplierOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.startingWidthMultiplier = newValue)
                .build());
        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.endingWidthMultiplierOthersDefault"), config.otherPlayerConfig.endingWidthMultiplier)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().endingWidthMultiplier)
                .setTooltip(Component.translatable("text.elytratrails.option.endingWidthMultiplierOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.endingWidthMultiplier = newValue)
                .build());
        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.distanceTillTrailStartOthersDefault"), config.otherPlayerConfig.distanceTillTrailStart)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().distanceTillTrailStart)
                .setTooltip(Component.translatable("text.elytratrails.option.distanceTillTrailStartOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.distanceTillTrailStart = newValue)
                .build());
        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.endDistanceFadeOthersDefault"), config.otherPlayerConfig.endDistanceFade)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().endDistanceFade)
                .setTooltip(Component.translatable("text.elytratrails.option.endDistanceFadeOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.endDistanceFade = newValue)
                .build());
        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.endDistanceFadeAmountOthersDefault"), config.otherPlayerConfig.endDistanceFadeAmount)
                .setDefaultValue(ClientConfig.getDefaultClientConfig().endDistanceFadeAmount)
                .setTooltip(Component.translatable("text.elytratrails.option.endDistanceFadeAmountOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.otherPlayerConfig.endDistanceFadeAmount = newValue)
                .build());

        particles.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.particles.desc"))
                .build());
        particles.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableParticles"), config.enableParticles)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.enableParticles.@Tooltip"))
                .setSaveConsumer(newValue -> config.enableParticles = newValue)
                .build());

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
            config.clientPlayerConfig.color = encodeColor(config.clientPlayerConfig.justAlpha, config.clientPlayerConfig.justColor);
            config.otherPlayerConfig.color = encodeColor(config.otherPlayerConfig.justAlpha, config.otherPlayerConfig.justColor);
            exportPreset(config);
            applyPresetsToConfig(config);
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
            TrailPackConfigManager.applyPreset(false, config.PresetOthers,config);
            config.otherPlayerConfig.justColor = decodeColors(config.otherPlayerConfig.color);
            config.otherPlayerConfig.justAlpha = decodeAlpha(config.otherPlayerConfig.color);
        }
        if(!config.Preset.isEmpty())
        {
            TrailPackConfigManager.applyPreset(true, config.Preset,config);
            config.clientPlayerConfig.justColor = decodeColors(config.clientPlayerConfig.color);
            config.clientPlayerConfig.justAlpha  = decodeAlpha(config.clientPlayerConfig.color);
        }
        config.Preset = "";
        config.PresetOthers = "";
    }}
