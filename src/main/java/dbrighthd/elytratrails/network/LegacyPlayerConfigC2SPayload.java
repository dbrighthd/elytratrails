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
 * never interpreted, only here to yell at players to update
 */
public record LegacyPlayerConfigC2SPayload(LegacyPlayerConfig playerConfig) implements CustomPacketPayload {
    public static final Identifier LEGACY_PLAYER_CONFIG_PAYLOAD_ID = Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "player_config");
    public static final CustomPacketPayload.Type<@NotNull LegacyPlayerConfigC2SPayload> ID = new CustomPacketPayload.Type<>(LEGACY_PLAYER_CONFIG_PAYLOAD_ID);
    public static final Codec<LegacyPlayerConfig> LEGACY_PLAYER_CONFIG_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("enableTrail").forGetter(LegacyPlayerConfig::enableTrail),
                    Codec.BOOL.fieldOf("enableRandomWidth").forGetter(LegacyPlayerConfig::enableRandomWidth),
                    Codec.BOOL.fieldOf("speedDependentTrail").forGetter(LegacyPlayerConfig::speedDependentTrail),
                    Codec.DOUBLE.fieldOf("trailMinSpeed").forGetter(LegacyPlayerConfig::trailMinSpeed),
                    Codec.BOOL.fieldOf("trailMovesWithElytraAngle").forGetter(LegacyPlayerConfig::trailMovesWithElytraAngle),
                    Codec.DOUBLE.fieldOf("maxWidth").forGetter(LegacyPlayerConfig::maxWidth),
                    Codec.DOUBLE.fieldOf("trailLifetime").forGetter(LegacyPlayerConfig::trailLifetime),
                    Codec.DOUBLE.fieldOf("startRampDistance").forGetter(LegacyPlayerConfig::startRampDistance),
                    Codec.DOUBLE.fieldOf("endRampDistance").forGetter(LegacyPlayerConfig::endRampDistance),
                    Codec.INT.fieldOf("color").forGetter(LegacyPlayerConfig::color),
                    Codec.DOUBLE.fieldOf("randomWidthVariation").forGetter(LegacyPlayerConfig::randomWidthVariation),
                    Codec.STRING.fieldOf("prideTrail").forGetter(LegacyPlayerConfig::prideTrail),
                    Codec.BOOL.fieldOf("fadeStart").forGetter(LegacyPlayerConfig::fadeStart),
                    Codec.DOUBLE.fieldOf("fadeStartDistance").forGetter(LegacyPlayerConfig::fadeStartDistance),
                    Codec.BOOL.fieldOf("fadeEnd").forGetter(LegacyPlayerConfig::fadeEnd),
                    Codec.INT.fieldOf("trailType").forGetter(LegacyPlayerConfig::trailType)
            ).apply(instance, LegacyPlayerConfig::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, LegacyPlayerConfigC2SPayload> CODEC =
            ByteBufCodecs.fromCodecWithRegistries(LEGACY_PLAYER_CONFIG_CODEC)
                    .map(LegacyPlayerConfigC2SPayload::new, LegacyPlayerConfigC2SPayload::playerConfig);
    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}