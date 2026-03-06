package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record RemoveFromStoreS2CPayload(int entityId) implements CustomPacketPayload {
    public static final Identifier TWIRL_STATE_PAYLOAD_ID = Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "remove_from_store");
    public static final CustomPacketPayload.Type<@NotNull RemoveFromStoreS2CPayload> ID = new CustomPacketPayload.Type<>(TWIRL_STATE_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveFromStoreS2CPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,RemoveFromStoreS2CPayload::entityId, RemoveFromStoreS2CPayload::new);
    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
