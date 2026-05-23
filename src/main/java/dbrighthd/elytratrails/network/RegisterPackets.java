package dbrighthd.elytratrails.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class RegisterPackets {
    public static Set<UUID> playersReceivedWarnings = new HashSet<>();
    public static void initCommon() {
        PayloadTypeRegistry.playS2C().register(TwirlStateS2CPayload.ID,TwirlStateS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TwirlStateC2SPayload.ID,TwirlStateC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerConfigS2CPayload.ID,PlayerConfigS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PlayerConfigC2SPayload.ID,PlayerConfigC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GetAllRequestC2SPayload.ID,GetAllRequestC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoveFromStoreS2CPayload.ID,RemoveFromStoreS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RemoveFromStoreC2SPayload.ID,RemoveFromStoreC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LegacyPlayerConfigS2CPayload.ID,LegacyPlayerConfigS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LegacyPlayerConfigC2SPayload.ID,LegacyPlayerConfigC2SPayload.CODEC);
    }
    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(TwirlStateC2SPayload.ID, (payload, context) -> {
            Entity entity = context.player();
            TwirlStateS2CPayload serverPayload = new TwirlStateS2CPayload(entity.getId(), payload.twirlState());
            for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, serverPayload);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(PlayerConfigC2SPayload.ID, (payload, context) -> {
            Entity entity = context.player();
            ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.put(entity.getId(),payload.configTag());
            PlayerConfigS2CPayload serverPayload = new PlayerConfigS2CPayload(entity.getId(), payload.configTag());
            for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, serverPayload);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(LegacyPlayerConfigC2SPayload.ID, (payload, context) -> {
            if(!playersReceivedWarnings.contains(context.player().getUUID()))
            {
                context.player().displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cYou are using an outdated version of Elytra Contrails. To sync with this server, you must update to Elytra Contrails 1.4.0+"),
                        false
                );
            }
            playersReceivedWarnings.add(context.player().getUUID());
        });
        ServerPlayNetworking.registerGlobalReceiver(GetAllRequestC2SPayload.ID, (payload, context) -> {
            for(Map.Entry<Integer, CompoundTag> configPair : ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.entrySet())
            {
                PlayerConfigS2CPayload serverPayload = new PlayerConfigS2CPayload(configPair.getKey(), configPair.getValue());
                ServerPlayNetworking.send(context.player(), serverPayload);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(RemoveFromStoreC2SPayload.ID, (payload, context) -> {
            ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.remove(context.player().getId());
            for (ServerPlayer player : context.server().getPlayerList().getPlayers())
            {
                RemoveFromStoreS2CPayload serverPayload = new RemoveFromStoreS2CPayload(context.player().getId());
                ServerPlayNetworking.send(player, serverPayload);
            }
        });
    }
}
