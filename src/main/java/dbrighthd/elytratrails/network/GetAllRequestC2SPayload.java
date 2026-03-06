package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record GetAllRequestC2SPayload() implements CustomPacketPayload {
    public static final Identifier ID_RAW =
            Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "get_all_configs");

    public static final Type<@NotNull GetAllRequestC2SPayload> ID =
            new Type<>(ID_RAW);

    // No bytes written/read.
    public static final StreamCodec<RegistryFriendlyByteBuf, GetAllRequestC2SPayload> CODEC =
            StreamCodec.unit(new GetAllRequestC2SPayload());
    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}