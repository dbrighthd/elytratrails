package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerConfigC2SPayload(CompoundTag configTag) implements CustomPacketPayload {
    public static final Identifier PLAYER_CONFIG_PAYLOAD_ID =
            Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "player_config");
    public static final Type<PlayerConfigC2SPayload> ID = new Type<>(PLAYER_CONFIG_PAYLOAD_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerConfigC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, PlayerConfigC2SPayload::configTag,
                    PlayerConfigC2SPayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}