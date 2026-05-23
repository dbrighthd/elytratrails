package dbrighthd.elytratrails.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class ClientPackets {
    private ClientPackets() {}

    public static void sendPlayerConfig(CompoundTag configTag) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(configTag);

        ClientPlayNetworking.send(RegisterPackets.PLAYER_CONFIG_C2S, buf);
    }

    public static void sendRemoveFromStore() {
        FriendlyByteBuf buf = PacketByteBufs.create();

        ClientPlayNetworking.send(RegisterPackets.REMOVE_FROM_STORE_C2S, buf);
    }

    public static void sendGetAllRequest() {
        FriendlyByteBuf buf = PacketByteBufs.create();

        ClientPlayNetworking.send(RegisterPackets.GET_ALL_REQUEST_C2S, buf);
    }

    public static void sendTwirlState(boolean twirlState) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(twirlState);

        ClientPlayNetworking.send(RegisterPackets.TWIRL_STATE_C2S, buf);
    }
}