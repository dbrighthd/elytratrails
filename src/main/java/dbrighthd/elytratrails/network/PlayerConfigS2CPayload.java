package dbrighthd.elytratrails.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerConfigS2CPayload(int entityId, PlayerConfig playerConfig) implements CustomPacketPayload {

    public static final Identifier PLAYER_CONFIG_PAYLOAD_ID =
            Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "player_config");

    public static final CustomPacketPayload.Type<PlayerConfigS2CPayload> ID =
            new CustomPacketPayload.Type<>(PLAYER_CONFIG_PAYLOAD_ID);

    // Same config codec as C2S (can be shared in a common helper if you want).
    public static final Codec<PlayerConfig> PLAYER_CONFIG_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("enableTrail").forGetter(PlayerConfig::enableTrail),
                    Codec.BOOL.fieldOf("enableRandomWidth").forGetter(PlayerConfig::enableRandomWidth),
                    Codec.BOOL.fieldOf("speedDependentTrail").forGetter(PlayerConfig::speedDependentTrail),
                    Codec.DOUBLE.fieldOf("trailMinSpeed").forGetter(PlayerConfig::trailMinSpeed),
                    Codec.BOOL.fieldOf("trailMovesWithElytraAngle").forGetter(PlayerConfig::trailMovesWithElytraAngle),
                    Codec.DOUBLE.fieldOf("maxWidth").forGetter(PlayerConfig::maxWidth),
                    Codec.DOUBLE.fieldOf("trailLifetime").forGetter(PlayerConfig::trailLifetime),
                    Codec.DOUBLE.fieldOf("startRampDistance").forGetter(PlayerConfig::startRampDistance),
                    Codec.DOUBLE.fieldOf("endRampDistance").forGetter(PlayerConfig::endRampDistance),
                    Codec.INT.fieldOf("color").forGetter(PlayerConfig::color),
                    Codec.DOUBLE.fieldOf("randomWidthVariation").forGetter(PlayerConfig::randomWidthVariation),
                    Codec.STRING.fieldOf("prideTrail").forGetter(PlayerConfig::prideTrail)
            ).apply(instance, PlayerConfig::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, PlayerConfig> PLAYER_CONFIG_STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(PLAYER_CONFIG_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerConfigS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PlayerConfigS2CPayload::entityId,
                    PLAYER_CONFIG_STREAM_CODEC, PlayerConfigS2CPayload::playerConfig,
                    PlayerConfigS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
