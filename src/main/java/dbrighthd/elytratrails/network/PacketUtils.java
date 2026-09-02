package dbrighthd.elytratrails.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;

public class PacketUtils {
    public static void sendToAllPlayers(MinecraftServer server, CustomPacketPayload serverPayload)
    {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, serverPayload);
        }
    }

    @SuppressWarnings("unused")
    public static void sendAllConfigToAllPlayers(MinecraftServer server)
    {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendAllConfigToPlayer(player);
        }
    }

    public static void sendAllConfigToPlayer(ServerPlayer player)
    {
        for (Map.Entry<Integer, CompoundTag> configPair : ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.entrySet()) {
            PlayerConfigS2CPayload serverPayload = new PlayerConfigS2CPayload(configPair.getKey(), configPair.getValue());
            ServerPlayNetworking.send(player, serverPayload);
        }
    }
}
