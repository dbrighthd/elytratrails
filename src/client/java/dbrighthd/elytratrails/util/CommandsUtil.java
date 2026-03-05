package dbrighthd.elytratrails.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.network.GetAllRequestC2SPayload;
import dbrighthd.elytratrails.rendering.TrailSystem;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.CLIENT_PLAYER_CONFIGS;

/**
 * Sets up some handy client commands
 */
public class CommandsUtil {
    public static void init()
    {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("elytratrails").then(ClientCommandManager.literal("debug")
                        .executes(CommandsUtil::debugCommand))
        ));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("elytratrails").then(ClientCommandManager.literal("debugmodels")
                        .executes(CommandsUtil::debugModelsCommand))
        ));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("elytratrails")
                        .then(ClientCommandManager.literal("clear")
                                .executes(CommandsUtil::clearCommand))
        ));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("elytratrails")
                        .then(ClientCommandManager.literal("getconfigs")
                                .executes(CommandsUtil::requestTrailConfigs))
        ));
    }
    private static int clearCommand(CommandContext<FabricClientCommandSource> context)
    {
        int trailcount = TrailSystem.getTrailManager().trailsNumber();
        int activetrailcount = TrailSystem.getTrailManager().activeTrailsNumber(); // or whatever your real method is
        TrailSystem.getTrailManager().removeAllTrails();

        context.getSource().sendFeedback(
                Component.literal("Cleared " + trailcount + " trails, " + activetrailcount + " of which were active.")
        );
        return Command.SINGLE_SUCCESS;
    }
    private static int debugCommand(CommandContext<FabricClientCommandSource> context)
    {
        Minecraft mc = Minecraft.getInstance();
        int trailcount = TrailSystem.getTrailManager().trailsNumber();
        int activetrailcount = TrailSystem.getTrailManager().activeTrailsNumber();
        context.getSource().sendFeedback(Component.literal("Stored Configs: " + CLIENT_PLAYER_CONFIGS.size()));
        for(var pair : CLIENT_PLAYER_CONFIGS.entrySet())
        {
            assert mc.level != null;
            Entity entity = mc.level.getEntity(pair.getKey());
            if(entity instanceof Player player)
            {
                context.getSource().sendFeedback(Component.literal(player.getName().getString()));
            }
        }
        context.getSource().sendFeedback(Component.literal("Current Trails: " + trailcount));
        context.getSource().sendFeedback(Component.literal("Active Trails:  " + activetrailcount));
        return Command.SINGLE_SUCCESS;
    }
    private static int debugModelsCommand(CommandContext<FabricClientCommandSource> context)
    {
        context.getSource().sendFeedback(Component.literal("Current Model Overrides: "));

        for(String model : TrailPackConfigManager.getModelStrings())
        {
            context.getSource().sendFeedback(Component.literal(model));

        }
        return Command.SINGLE_SUCCESS;
    }
    private static int requestTrailConfigs(CommandContext<FabricClientCommandSource> context)
    {
        ClientPlayNetworking.send(new GetAllRequestC2SPayload());
        return Command.SINGLE_SUCCESS;
    }
}
