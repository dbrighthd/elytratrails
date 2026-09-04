package dbrighthd.elytratrails.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 *twirl
 */
public record NetworkTwirlS2CPayload(int entityId, NetworkTwirl networkTwirl) implements CustomPacketPayload {

    public static final Identifier NETWORK_TWIRL_PAYLOAD_ID =
            Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "twirl_data");

    public static final Type<@NotNull NetworkTwirlS2CPayload> ID =
            new Type<>(NETWORK_TWIRL_PAYLOAD_ID);

    public static final Codec<NetworkTwirl> NETWORK_TWIRL_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("twirlType").forGetter(NetworkTwirl::twirlType),
                    Codec.INT.fieldOf("direction").forGetter(NetworkTwirl::direction),
                    Codec.LONG.fieldOf("twirlTime").forGetter(NetworkTwirl::twirlTime),
                    Codec.INT.fieldOf("easeMode").forGetter(NetworkTwirl::easeMode),
                    Codec.INT.fieldOf("axis").forGetter(NetworkTwirl::axis),
                    Codec.DOUBLE.fieldOf("offset").forGetter(NetworkTwirl::offset)
            ).apply(instance, NetworkTwirl::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, NetworkTwirl> NETWORK_TWIRL_STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(NETWORK_TWIRL_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkTwirlS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, NetworkTwirlS2CPayload::entityId,
                    NETWORK_TWIRL_STREAM_CODEC, NetworkTwirlS2CPayload::networkTwirl,
                    NetworkTwirlS2CPayload::new
            );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}