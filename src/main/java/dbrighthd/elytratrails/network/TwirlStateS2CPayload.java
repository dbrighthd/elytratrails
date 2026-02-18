package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TwirlStateS2CPayload(int entityId, int twirlState) implements CustomPacketPayload {
    public static final Identifier TWIRL_STATE_PAYLOAD_ID = Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "twirl_state");
    public static final CustomPacketPayload.Type<TwirlStateS2CPayload> ID = new CustomPacketPayload.Type<>(TWIRL_STATE_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, TwirlStateS2CPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, TwirlStateS2CPayload::entityId, ByteBufCodecs.VAR_INT,TwirlStateS2CPayload::twirlState, TwirlStateS2CPayload::new);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
