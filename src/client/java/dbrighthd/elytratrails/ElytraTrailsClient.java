package dbrighthd.elytratrails;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.config.ModMenuConfigManager;
import com.terraformersmc.modmenu.event.ModMenuEventHandler;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.handler.ParticleHandler;
import dbrighthd.elytratrails.handler.TrailRenderHandler;
import dbrighthd.elytratrails.handler.WingTipSamplerHandler;
import dbrighthd.elytratrails.network.RegisterPacketsClient;
import dbrighthd.elytratrails.trailrendering.TrailRenderType;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static dbrighthd.elytratrails.compat.emf.EmfTrailSpawnerRegistry.onResourceReload;


@SuppressWarnings("deprecation")
public class ElytraTrailsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient()
	{
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		ElytraTrailsKeybind.init();
		TrailRenderType.init();
		TrailRenderHandler.init();
		WingTipSamplerHandler.init();
		ParticleHandler.init();
		RegisterPacketsClient.initClient();
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
				new SimpleSynchronousResourceReloadListener() {
					@Override
					public @NotNull Identifier getFabricId() {
                        return Objects.requireNonNull(Identifier.tryParse("elytratrails:trail_pack_configs"));
					}

					@Override
					public void onResourceManagerReload(@NotNull ResourceManager manager) {
						onResourceReload();
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
