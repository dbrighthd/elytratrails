package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record TwirlStateS2CPayload(int entityId, int twirlState) implements CustomPacketPayload {
    public static final Identifier TWIRL_STATE_PAYLOAD_ID = Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "twirl_state");
    public static final CustomPacketPayload.Type<@NotNull TwirlStateS2CPayload> ID = new CustomPacketPayload.Type<>(TWIRL_STATE_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, TwirlStateS2CPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, TwirlStateS2CPayload::entityId, ByteBufCodecs.VAR_INT,TwirlStateS2CPayload::twirlState, TwirlStateS2CPayload::new);
    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
