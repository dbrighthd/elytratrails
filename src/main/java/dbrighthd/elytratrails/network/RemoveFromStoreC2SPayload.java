package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RemoveFromStoreC2SPayload() implements CustomPacketPayload {
    public static final Identifier ID_RAW =
            Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "remove_from_store");

    public static final Type<RemoveFromStoreC2SPayload> ID =
            new Type<>(ID_RAW);

    // No bytes written/read.
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveFromStoreC2SPayload> CODEC =
            StreamCodec.unit(new RemoveFromStoreC2SPayload());
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
