package dbrighthd.elytratrails.network;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ConcurrentHashMap;

public class ServerPlayerConfigStore {
    public static final ConcurrentHashMap<Integer, PlayerConfig> SERVER_PLAYER_CONFIGS = new ConcurrentHashMap<>();

    public static void registerDisconnectCleanup() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;

            RemoveFromStoreS2CPayload serverPayload = new RemoveFromStoreS2CPayload(player.getId());
            for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(serverPlayer, serverPayload);
            }
            SERVER_PLAYER_CONFIGS.remove(player.getId());
        });
    }
}
