package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record PlayerConfigS2CPayload(int entityId, CompoundTag configTag) implements CustomPacketPayload {
    public static final Identifier PLAYER_CONFIG_PAYLOAD_ID =
            Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "player_config_tag");
    public static final Type<@NotNull PlayerConfigS2CPayload> ID = new Type<>(PLAYER_CONFIG_PAYLOAD_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerConfigS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PlayerConfigS2CPayload::entityId,
                    ByteBufCodecs.COMPOUND_TAG, PlayerConfigS2CPayload::configTag,
                    PlayerConfigS2CPayload::new
            );

    @Override public @NotNull Type<? extends @NotNull CustomPacketPayload> type() { return ID; }
}