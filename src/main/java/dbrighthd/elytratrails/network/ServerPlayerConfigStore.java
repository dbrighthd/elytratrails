package dbrighthd.elytratrails.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ConcurrentHashMap;

import static dbrighthd.elytratrails.network.RegisterPackets.playersReceivedWarnings;

public class ServerPlayerConfigStore {
    public static final ConcurrentHashMap<Integer, CompoundTag> SERVER_PLAYER_CONFIGS = new ConcurrentHashMap<>();

    public static void registerDisconnectCleanup() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            playersReceivedWarnings.remove(player.getUUID());
            RemoveFromStoreS2CPayload serverPayload = new RemoveFromStoreS2CPayload(player.getId());
            for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(serverPlayer, serverPayload);
            }
            SERVER_PLAYER_CONFIGS.remove(player.getId());
        });
    }
}
