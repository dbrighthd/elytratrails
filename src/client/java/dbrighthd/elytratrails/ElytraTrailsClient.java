package dbrighthd.elytratrails;

import dbrighthd.elytratrails.compat.Compatibility;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.handler.ParticleHandler;
import dbrighthd.elytratrails.handler.TrailRenderHandler;
import dbrighthd.elytratrails.handler.WingTipSamplerHandler;
import dbrighthd.elytratrails.network.RegisterPacketsClient;
import dbrighthd.elytratrails.rendering.TrailPipelines;
import dbrighthd.elytratrails.rendering.TrailSystem;
import dbrighthd.elytratrails.trailrendering.TrailRenderType;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static dbrighthd.elytratrails.compat.emf.EmfTrailSpawnerRegistry.onResourceReload;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.refreshLocalConfigs;


@SuppressWarnings("deprecation")
public class ElytraTrailsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient()
	{
		Compatibility.init();
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		ElytraTrailsKeybind.init();
//		TrailRenderType.init();
//		TrailRenderHandler.init();
		TrailPipelines.init();
		TrailSystem.init();
//		WingTipSamplerHandler.init();
		ParticleHandler.init();
		refreshLocalConfigs();
		RegisterPacketsClient.initClient();

		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
			new SimpleSynchronousResourceReloadListener() {
				@Override
				public @NotNull Identifier getFabricId() {
					return Objects.requireNonNull(Identifier.tryParse("elytratrails:trail_pack_configs"));
				}

				@Override
				public void onResourceManagerReload(@NotNull ResourceManager manager) {
					dbrighthd.elytratrails.trailrendering.TrailTextureRegistry
							.reloadNow(manager);


					if (FabricLoader.getInstance().isModLoaded("entity_model_features")) {
						onResourceReload();
					}
					TrailPackConfigManager.reload(manager);
				}
			}
		);

	}

	public static ModConfig getConfig()
	{
		return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}

	public static void setConfig(ModConfig modConfig)
	{
		AutoConfig.getConfigHolder(ModConfig.class).setConfig(modConfig);
	}
}
