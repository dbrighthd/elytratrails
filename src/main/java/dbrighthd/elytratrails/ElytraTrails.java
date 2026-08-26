package dbrighthd.elytratrails;

import dbrighthd.elytratrails.network.RegisterPackets;
import dbrighthd.elytratrails.network.ServerPlayerConfigStore;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElytraTrails implements ModInitializer {
	public static final String MOD_ID = "elytratrails";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final GameRule<Boolean> ENABLE_PLAYER_TRAILS_GAMERULE = GameRuleBuilder
			.forBoolean(true)
			.category(GameRuleCategory.MISC)
			.buildAndRegister(Identifier.fromNamespaceAndPath(MOD_ID,"enable_trails"));
	
	@Override
	public void onInitialize() {
		RegisterPackets.initCommon();
		RegisterPackets.initServer();
		ServerPlayerConfigStore.registerDisconnectCleanup();
	}
}