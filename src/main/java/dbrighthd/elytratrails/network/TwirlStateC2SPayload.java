package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TwirlStateC2SPayload(int twirlState) implements CustomPacketPayload {
    public static final Identifier TWIRL_STATE_PAYLOAD_ID = Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "twirl_state");
    public static final CustomPacketPayload.Type<TwirlStateC2SPayload> ID = new CustomPacketPayload.Type<>(TWIRL_STATE_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, TwirlStateC2SPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,TwirlStateC2SPayload::twirlState, TwirlStateC2SPayload::new);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
