package dbrighthd.elytratrails.config;

import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.network.GetAllRequestC2SPayload;
import dbrighthd.elytratrails.network.PlayerConfigC2SPayload;
import dbrighthd.elytratrails.network.RemoveFromStoreC2SPayload;
import dbrighthd.elytratrails.rendering.TrailSystem;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.config.pack.TrailPackConfigManager.exportTrailPresetToDisk;
import static dbrighthd.elytratrails.config.pack.TrailPackConfigManager.parseHexColor;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.*;

public class ConfigScreenBuilder {
    public static Screen buildConfigScreen(Screen parent, ModConfig config)
    {
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
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableTwirls"), config.enableTwirls)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.enableTwirls.@Tooltip"))
                .setSaveConsumer(newValue -> config.enableTwirls = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.alwaysShowTrailDuringTwirl"), config.alwaysShowTrailDuringTwirl)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.alwaysShowTrailDuringTwirl.@Tooltip"))
                .setSaveConsumer(newValue -> config.alwaysShowTrailDuringTwirl = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.firstPersonTwirlCamera"), config.fishysStupidCameraRoll)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.firstPersonTwirlCamera.@Tooltip"))
                .setSaveConsumer(newValue -> config.fishysStupidCameraRoll = newValue)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.thirdPersonTwirlCamera"), config.fishysStupidThirdPersonCameraRoll)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.thirdPersonTwirlCamera.@Tooltip"))
                .setSaveConsumer(newValue -> config.fishysStupidThirdPersonCameraRoll = newValue)
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


        elytra.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("text.elytratrails.category.elytra.desc"))
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableTrail"), config.enableTrail)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.enableTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.enableTrail = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.translucentTrails"), config.translucentTrails)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.translucentTrails.@Tooltip"))
                .setSaveConsumer(newValue -> config.translucentTrails = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.glowingTrails"), config.glowingTrails)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.glowingTrails.@Tooltip"))
                .setSaveConsumer(newValue -> config.glowingTrails = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableRandomWidth"), config.enableRandomWidth)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.enableRandomWidth.@Tooltip"))
                .setSaveConsumer(newValue -> config.enableRandomWidth = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.speedDependentTrail"), config.speedDependentTrail)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.speedDependentTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.speedDependentTrail = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeFirstPersonTrail"), config.fadeFirstPersonTrail)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeFirstPersonTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.fadeFirstPersonTrail = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.trailMovesWithElytraAngle"), config.trailMovesWithElytraAngle)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.trailMovesWithElytraAngle.@Tooltip"))
                .setSaveConsumer(newValue -> config.trailMovesWithElytraAngle = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.width"), config.width)
                .setDefaultValue(0.05)
                .setTooltip(Component.translatable("text.elytratrails.option.width.@Tooltip"))
                .setSaveConsumer(newValue -> config.width = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.firstPersonFadeTime"), config.firstPersonFadeTime)
                .setDefaultValue(0.3)
                .setTooltip(Component.translatable("text.elytratrails.option.firstPersonFadeTime.@Tooltip"))
                .setSaveConsumer(newValue -> config.firstPersonFadeTime = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.trailLifetime"), config.trailLifetime)
                .setDefaultValue(2.5)
                .setTooltip(Component.translatable("text.elytratrails.option.trailLifetime.@Tooltip"))
                .setSaveConsumer(newValue -> config.trailLifetime = newValue)
                .build());
        elytra.addEntry(entryBuilder.startIntField(Component.translatable("text.elytratrails.option.maxSamplePerSecond"), config.maxSamplePerSecond)
                .setDefaultValue(60)
                .setTooltip(Component.translatable("text.elytratrails.option.maxSamplePerSecond.@Tooltip"))
                .setSaveConsumer(newValue -> config.maxSamplePerSecond = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.trailMinSpeed"), config.trailMinSpeed)
                .setDefaultValue(0.75)
                .setTooltip(Component.translatable("text.elytratrails.option.trailMinSpeed.@Tooltip"))
                .setSaveConsumer(newValue -> config.trailMinSpeed = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.startRampDistance"), config.startRampDistance)
                .setDefaultValue(4.0)
                .setTooltip(Component.translatable("text.elytratrails.option.startRampDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.startRampDistance = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.endRampDistance"), config.endRampDistance)
                .setDefaultValue(10.0)
                .setTooltip(Component.translatable("text.elytratrails.option.endRampDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.endRampDistance = newValue)
                .build());
        elytra.addEntry(entryBuilder.startColorField(Component.translatable("text.elytratrails.option.color"), config.justColor)
                .setDefaultValue(0xFFFFFF)
                .setTooltip(Component.translatable("text.elytratrails.option.color.@Tooltip"))
                .setSaveConsumer(newValue -> config.justColor = newValue)
                .build());
        elytra.addEntry(entryBuilder.startIntField(Component.translatable("text.elytratrails.option.alpha"), config.justAlpha)
                .setDefaultValue(255)
                .setTooltip(Component.translatable("text.elytratrails.option.alpha.@Tooltip"))
                .setSaveConsumer(newValue -> config.justAlpha = newValue)
                .build());
        elytra.addEntry(entryBuilder.startStrField(Component.translatable("text.elytratrails.option.prideTrail"), config.prideTrail)
                .setDefaultValue("")
                .setTooltip(Component.translatable("text.elytratrails.option.prideTrail.@Tooltip"))
                .setSaveConsumer(newValue -> config.prideTrail = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.randomWidthVariation"), config.randomWidthVariation)
                .setDefaultValue(1)
                .setTooltip(Component.translatable("text.elytratrails.option.randomWidthVariation.@Tooltip"))
                .setSaveConsumer(newValue -> config.randomWidthVariation = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeStart"), config.fadeStart)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeStart.@Tooltip"))
                .setSaveConsumer(newValue -> config.fadeStart = newValue)
                .build());
        elytra.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.fadeStartDistance"), config.fadeStartDistance)
                .setDefaultValue(4.0)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeStartDistance.@Tooltip"))
                .setSaveConsumer(newValue -> config.fadeStartDistance = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeEnd"), config.fadeEnd)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeEnd.@Tooltip"))
                .setSaveConsumer(newValue -> config.fadeEnd = newValue)
                .build());
        elytra.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.wireframeTrails"), config.wireframeTrails)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.wireframeTrails.@Tooltip"))
                .setSaveConsumer(newValue -> config.wireframeTrails = newValue)
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
        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableTrailOthersDefault"), config.enableTrailOthersDefault)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.enableTrailOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.enableTrailOthersDefault = newValue)
                .build());
        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.translucentTrailsOthersDefault"), config.translucentTrailsOthersDefault)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.translucentTrailsOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.translucentTrailsOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.glowingTrailsOthersDefault"), config.glowingTrailsOthersDefault)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.glowingTrailsOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.glowingTrailsOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.enableRandomWidthOthersDefault"), config.enableRandomWidthOthersDefault)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.enableRandomWidthOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.enableRandomWidthOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.speedDependentTrailOthersDefault"), config.speedDependentTrailOthersDefault)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.speedDependentTrailOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.speedDependentTrailOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.trailMovesWithElytraAngleOthersDefault"), config.trailMovesWithElytraAngleOthersDefault)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.trailMovesWithElytraAngleOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.trailMovesWithElytraAngleOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.widthOthersDefault"), config.widthOthersDefault)
                .setDefaultValue(0.1)
                .setTooltip(Component.translatable("text.elytratrails.option.widthOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.widthOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.trailLifetimeOthersDefault"), config.trailLifetimeOthersDefault)
                .setDefaultValue(2.5)
                .setTooltip(Component.translatable("text.elytratrails.option.trailLifetimeOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.trailLifetimeOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.trailMinSpeedOthersDefault"), config.trailMinSpeedOthersDefault)
                .setDefaultValue(0.75)
                .setTooltip(Component.translatable("text.elytratrails.option.trailMinSpeedOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.trailMinSpeedOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.startRampDistanceOthersDefault"), config.startRampDistanceOthersDefault)
                .setDefaultValue(4.0)
                .setTooltip(Component.translatable("text.elytratrails.option.startRampDistanceOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.startRampDistanceOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.endRampDistanceOthersDefault"), config.endRampDistanceOthersDefault)
                .setDefaultValue(10.0)
                .setTooltip(Component.translatable("text.elytratrails.option.endRampDistanceOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.endRampDistanceOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startColorField(Component.translatable("text.elytratrails.option.colorOthersDefault"), config.justColorOthersDefault)
                .setDefaultValue(0xFFFFFF)
                .setTooltip(Component.translatable("text.elytratrails.option.colorOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.justColorOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startIntField(Component.translatable("text.elytratrails.option.alphaOthersDefault"), config.justAlphaOthersDefault)
                .setDefaultValue(255)
                .setTooltip(Component.translatable("text.elytratrails.option.alphaOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.justAlphaOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startStrField(Component.translatable("text.elytratrails.option.prideTrailOthersDefault"), config.prideTrailOthersDefault)
                .setDefaultValue("")
                .setTooltip(Component.translatable("text.elytratrails.option.prideTrailOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.prideTrailOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.randomWidthVariationOthersDefault"), config.randomWidthVariationOthersDefault)
                .setDefaultValue(1.0)
                .setTooltip(Component.translatable("text.elytratrails.option.randomWidthVariationOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.randomWidthVariationOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeStartOthersDefault"), config.fadeStartOthersDefault)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeStartOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.fadeStartOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startDoubleField(Component.translatable("text.elytratrails.option.fadeStartDistanceOthersDefault"), config.fadeStartDistanceOthersDefault)
                .setDefaultValue(4.0)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeStartDistanceOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.fadeStartDistanceOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.fadeEndOthersDefault"), config.fadeEndOthersDefault)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.elytratrails.option.fadeEndOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.fadeEndOthersDefault = newValue)
                .build());

        others.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.elytratrails.option.wireframeTrailsOthersDefault"), config.wireframeTrailsOthersDefault)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.elytratrails.option.wireframeTrailsOthersDefault.@Tooltip"))
                .setSaveConsumer(newValue -> config.wireframeTrailsOthersDefault = newValue)
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
            encodeColors(config);
            exportPreset(config);
            applyPresetsToConfig(config);
            AutoConfig.getConfigHolder(ModConfig.class).save();
                    var mc = Minecraft.getInstance();
                    refreshLocalConfigs();
                    if (mc.getConnection() != null && mc.player != null && mc.level != null) {
                        TrailSystem.getTrailManager().removeTrail(mc.player.getId());
                        TrailSystem.getWingtipSampler().removeAllEmfCache();
                        if(getConfig().shareTrail || !getConfig().showTrailToOtherPlayers)
                        {
                            ClientPlayNetworking.send(new PlayerConfigC2SPayload(getLocalPlayerConfigToSend()));
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
    private static void encodeColors(ModConfig config)
    {
        config.color = TrailPackConfigManager.withAlphaAndColorToHexString(config.justAlpha,config.justColor);
        config.colorOthersDefault = TrailPackConfigManager.withAlphaAndColorToHexString(config.justAlphaOthersDefault,config.justColorOthersDefault);

    }
    private static void decodeColors(ModConfig config)
    {
        config.justColor = TrailPackConfigManager.getColorRgb(parseHexColor(config.color));
        config.justAlpha = TrailPackConfigManager.getAlpha(parseHexColor(config.color));
    }
    private static void decodeColorsOthers(ModConfig config)
    {
        config.justColorOthersDefault = TrailPackConfigManager.getColorRgb(parseHexColor(config.colorOthersDefault));
        config.justAlphaOthersDefault = TrailPackConfigManager.getAlpha(parseHexColor(config.colorOthersDefault));
    }
    private  static void applyPresetsToConfig(ModConfig config)
    {

        if(!config.PresetOthers.isEmpty())
        {
            TrailPackConfigManager.applyPreset(false, config.PresetOthers,config);
            decodeColorsOthers(config);
        }
        if(!config.Preset.isEmpty())
        {
            TrailPackConfigManager.applyPreset(true, config.Preset,config);
            decodeColors(config);
        }
        config.Preset = "";
        config.PresetOthers = "";
    }
}
