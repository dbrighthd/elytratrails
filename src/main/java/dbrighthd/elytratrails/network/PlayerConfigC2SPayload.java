package dbrighthd.elytratrails.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;


public record PlayerConfigC2SPayload(PlayerConfig playerConfig) implements CustomPacketPayload {
    public static final Identifier PLAYER_CONFIG_PAYLOAD_ID = Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "player_config");
    public static final CustomPacketPayload.Type<PlayerConfigC2SPayload> ID = new CustomPacketPayload.Type<>(PLAYER_CONFIG_PAYLOAD_ID);
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
                    Codec.DOUBLE.fieldOf("endRampDistance").forGetter(PlayerConfig::endRampDistance), // <-- add this
                    Codec.STRING.fieldOf("color").forGetter(PlayerConfig::color),
                    Codec.DOUBLE.fieldOf("randomWidthVariation").forGetter(PlayerConfig::randomWidthVariation),
                    Codec.STRING.fieldOf("prideTrail").forGetter(PlayerConfig::prideTrail)
            ).apply(instance, PlayerConfig::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerConfigC2SPayload> CODEC =
            ByteBufCodecs.fromCodecWithRegistries(PLAYER_CONFIG_CODEC)
                    .map(PlayerConfigC2SPayload::new, PlayerConfigC2SPayload::playerConfig);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}