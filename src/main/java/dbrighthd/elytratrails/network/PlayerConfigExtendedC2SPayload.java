package dbrighthd.elytratrails.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;


public record PlayerConfigExtendedC2SPayload(PlayerConfigExtended playerConfigExtended) implements CustomPacketPayload {
    public static final Identifier PLAYER_CONFIG_PAYLOAD_ID = Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "player_config_extended");
    public static final Type<PlayerConfigExtendedC2SPayload> ID = new Type<>(PLAYER_CONFIG_PAYLOAD_ID);
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
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerConfigExtendedC2SPayload> CODEC =
            ByteBufCodecs.fromCodecWithRegistries(PLAYER_CONFIG_EXTENDED_CODEC)
                    .map(PlayerConfigExtendedC2SPayload::new, PlayerConfigExtendedC2SPayload::playerConfigExtended);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}