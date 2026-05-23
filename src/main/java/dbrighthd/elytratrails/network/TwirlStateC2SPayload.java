package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record TwirlStateC2SPayload(int twirlState) implements CustomPacketPayload {
    public static final ResourceLocation TWIRL_STATE_PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath(ElytraTrails.MOD_ID, "twirl_state");
    public static final Type<@NotNull TwirlStateC2SPayload> ID = new Type<>(TWIRL_STATE_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, TwirlStateC2SPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,TwirlStateC2SPayload::twirlState, TwirlStateC2SPayload::new);
    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
