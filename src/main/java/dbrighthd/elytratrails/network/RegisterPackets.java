package dbrighthd.elytratrails.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class RegisterPackets {
    public static void initCommon() {
        PayloadTypeRegistry.playS2C().register(TwirlStateS2CPayload.ID,TwirlStateS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TwirlStateC2SPayload.ID,TwirlStateC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerConfigS2CPayload.ID,PlayerConfigS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PlayerConfigC2SPayload.ID,PlayerConfigC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GetAllRequestC2SPayload.ID,GetAllRequestC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoveFromStoreS2CPayload.ID,RemoveFromStoreS2CPayload.CODEC);
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
            ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.put(entity.getId(),payload.playerConfig());
            PlayerConfigS2CPayload serverPayload = new PlayerConfigS2CPayload(entity.getId(), payload.playerConfig());
            for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, serverPayload);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(GetAllRequestC2SPayload.ID, (payload, context) -> {
            for(Map.Entry<Integer, PlayerConfig> configPair : ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.entrySet())
            {
                PlayerConfigS2CPayload serverPayload = new PlayerConfigS2CPayload(configPair.getKey(), configPair.getValue());
                ServerPlayNetworking.send(context.player(), serverPayload);
            }
        });
    }
}
