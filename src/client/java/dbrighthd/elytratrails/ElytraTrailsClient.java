package dbrighthd.elytratrails;

import dbrighthd.elytratrails.compat.Compatibility;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.controller.ContinuousTwirlController;
import dbrighthd.elytratrails.controller.TwirlController;
import dbrighthd.elytratrails.handler.ParticleHandler;
import dbrighthd.elytratrails.network.RegisterPacketsClient;
import dbrighthd.elytratrails.rendering.*;
import dbrighthd.elytratrails.handler.CommandHandler;
import dbrighthd.elytratrails.util.TimeUtil;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static dbrighthd.elytratrails.compat.emf.EmfTrailSpawnerRegistry.onResourceReload;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.refreshLocalConfigs;


@SuppressWarnings("deprecation")
public class ElytraTrailsClient implements ClientModInitializer {
	private static ModConfig modConfig;
	@Override
	public void onInitializeClient()
	{
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		refreshConfig();
		TimeUtil.init();
		Compatibility.init();
		ElytraTrailsKeybind.init();
		TrailPipelines.init();
		TrailSystem.init();
		ParticleHandler.init();
		refreshLocalConfigs();
		RegisterPacketsClient.initClient();
		CommandHandler.init();
		ContinuousTwirlController.setDurations();
		TwirlController.setDurations();
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
			new SimpleSynchronousResourceReloadListener() {
				@Override
				public @NotNull Identifier getFabricId() {
					return Objects.requireNonNull(Identifier.tryParse("elytratrails:trail_pack_configs"));
				}

				@Override
				public void onResourceManagerReload(@NotNull ResourceManager manager) {
					TrailTextureRegistry
							.reloadNow(manager);

					TrailSystem.getWingtipSampler().removeAllEmfCache();
					if (FabricLoader.getInstance().isModLoaded("entity_model_features")) {
						onResourceReload();
					}
					TrailPackConfigManager.reload(manager);
					TrailPackConfigManager.reloadPresets(manager);
				}
			}
		);
		FabricLoader.getInstance().getModContainer("elytratrails").ifPresent(container -> {
			boolean ok = ResourceManagerHelper.registerBuiltinResourcePack(
					Identifier.fromNamespaceAndPath("elytratrails", "arrowtrails"),
					container,
					Component.literal("arrowtrails"),
					ResourcePackActivationType.NORMAL
			);

			if (!ok) {
				ElytraTrails.LOGGER.warn("Failed to register built-in resource pack elytratrails:arrowtrials");
			}
		});


	}

	public static ModConfig getConfig()
	{
		return modConfig;
	}

	public static void setConfig(ModConfig modConfig)
	{
		AutoConfig.getConfigHolder(ModConfig.class).setConfig(modConfig);
	}

	public static void refreshConfig()
	{
		modConfig = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}
}
