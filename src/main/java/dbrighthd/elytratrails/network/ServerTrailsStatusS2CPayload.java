package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ServerTrailsStatusS2CPayload(boolean enabled) implements CustomPacketPayload {
    public static final Identifier SERVER_TRAIL_STATE_PAYLOAD_ID = Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "trails_enabled");
    public static final CustomPacketPayload.Type<@NotNull ServerTrailsStatusS2CPayload> ID = new CustomPacketPayload.Type<>(SERVER_TRAIL_STATE_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerTrailsStatusS2CPayload> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL,ServerTrailsStatusS2CPayload::enabled, ServerTrailsStatusS2CPayload::new);
    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}