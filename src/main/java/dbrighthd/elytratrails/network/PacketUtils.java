package dbrighthd.elytratrails.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;

public class PacketUtils {
    public static void sendToAllPlayers(ServerPlayNetworking.Context context, CustomPacketPayload serverPayload)
    {
        for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, serverPayload);
        }
    }

    public static CompoundTag getHiddenConfigTag()
    {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putBoolean("enableTrail",false);
        compoundTag.putString("playerName","Hidden Player");
        return compoundTag;
    }
    public static void sendAllConfigToAllPlayers(MinecraftServer server, boolean isEnabled)
    {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendAllConfigToPlayer(player, isEnabled);
        }
    }

    public static void sendAllConfigToPlayer(ServerPlayer player, boolean isEnabled)
    {
        for (Map.Entry<Integer, CompoundTag> configPair : ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.entrySet()) {
            PlayerConfigS2CPayload serverPayload = new PlayerConfigS2CPayload(configPair.getKey(), isEnabled ? configPair.getValue() : getHiddenConfigTag());
            ServerPlayNetworking.send(player, serverPayload);
        }
    }
}
