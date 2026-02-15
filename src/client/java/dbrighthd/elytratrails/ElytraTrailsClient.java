package dbrighthd.elytratrails;

import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.handler.ParticleHandler;
import dbrighthd.elytratrails.handler.TrailRenderHandler;
import dbrighthd.elytratrails.handler.WingTipSamplerHandler;
import dbrighthd.elytratrails.trailrendering.TrailRenderType;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;


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
