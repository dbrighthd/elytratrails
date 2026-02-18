package dbrighthd.elytratrails;

import dbrighthd.elytratrails.network.RegisterPackets;
import dbrighthd.elytratrails.network.ServerPlayerConfigStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElytraTrails implements ModInitializer {
	public static final String MOD_ID = "elytratrails";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		RegisterPackets.initCommon();
		RegisterPackets.initServer();
		ServerPlayerConfigStore.registerDisconnectCleanup();
	}
}