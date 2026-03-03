package dbrighthd.elytratrails.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerConfigExtendedS2CPayload(int entityId, PlayerConfigExtended playerConfigExtended) implements CustomPacketPayload {

    public static final Identifier PLAYER_CONFIG_PAYLOAD_ID =
            Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "player_config_extended");

    public static final Type<PlayerConfigExtendedS2CPayload> ID =
            new Type<>(PLAYER_CONFIG_PAYLOAD_ID);

    // Same config codec as C2S (can be shared in a common helper if you want).
    public static final Codec<PlayerConfigExtended> PLAYER_CONFIG_EXTENDED_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("alwaysShowTrailDuringTwirl").forGetter(PlayerConfigExtended::alwaysShowTrailDuringTwirl),
                    Codec.STRING.fieldOf("prideTrailRight").forGetter(PlayerConfigExtended::prideTrailRight),
                    Codec.DOUBLE.fieldOf("twirlTime").forGetter(PlayerConfigExtended::twirlTime),
                    Codec.BOOL.fieldOf("increaseWidthOverTime").forGetter(PlayerConfigExtended::increaseWidthOverTime),
                    Codec.DOUBLE.fieldOf("startingWidthMultiplier").forGetter(PlayerConfigExtended::startingWidthMultiplier),
                    Codec.DOUBLE.fieldOf("endingWidthMultiplier").forGetter(PlayerConfigExtended::endingWidthMultiplier),
                    Codec.DOUBLE.fieldOf("distanceTillTrailStart").forGetter(PlayerConfigExtended::distanceTillTrailStart),
                    Codec.STRING.fieldOf("easeType").forGetter(PlayerConfigExtended::easeType)
                    ).apply(instance, PlayerConfigExtended::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, PlayerConfigExtended> PLAYER_CONFIG_EXTENDED_STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(PLAYER_CONFIG_EXTENDED_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerConfigExtendedS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PlayerConfigExtendedS2CPayload::entityId,
                    PLAYER_CONFIG_EXTENDED_STREAM_CODEC, PlayerConfigExtendedS2CPayload::playerConfigExtended,
                    PlayerConfigExtendedS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
