package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.ElytraTrails;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

//I think I miss 1.21.11 networking code
public class RegisterPackets {
    public static final ResourceLocation TWIRL_STATE_C2S =
            new ResourceLocation(ElytraTrails.MOD_ID, "twirl_state_c2s");

    public static final ResourceLocation TWIRL_STATE_S2C =
            new ResourceLocation(ElytraTrails.MOD_ID, "twirl_state_s2c");

    public static final ResourceLocation PLAYER_CONFIG_C2S =
            new ResourceLocation(ElytraTrails.MOD_ID, "player_config_c2s");

    public static final ResourceLocation PLAYER_CONFIG_S2C =
            new ResourceLocation(ElytraTrails.MOD_ID, "player_config_s2c");

    public static final ResourceLocation GET_ALL_REQUEST_C2S =
            new ResourceLocation(ElytraTrails.MOD_ID, "get_all_request_c2s");

    public static final ResourceLocation REMOVE_FROM_STORE_C2S =
            new ResourceLocation(ElytraTrails.MOD_ID, "remove_from_store_c2s");

    public static final ResourceLocation REMOVE_FROM_STORE_S2C =
            new ResourceLocation(ElytraTrails.MOD_ID, "remove_from_store_s2c");

    public static final ResourceLocation LEGACY_PLAYER_CONFIG_C2S =
            new ResourceLocation(ElytraTrails.MOD_ID, "legacy_player_config_c2s");

    public static final ResourceLocation LEGACY_PLAYER_CONFIG_S2C =
            new ResourceLocation(ElytraTrails.MOD_ID, "legacy_player_config_s2c");

    public static Set<UUID> playersReceivedWarnings = new HashSet<>();

    public static void initCommon() {
    }

    public static void initServer() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            int playerId = player.getId();

            server.execute(() -> {
                ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.remove(playerId);
                playersReceivedWarnings.remove(player.getUUID());

                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    FriendlyByteBuf serverBuf = PacketByteBufs.create();
                    serverBuf.writeInt(playerId);

                    ServerPlayNetworking.send(target, REMOVE_FROM_STORE_S2C, serverBuf);
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(TWIRL_STATE_C2S, (server, player, handler, buf, responseSender) -> {
            int twirlState = buf.readInt();

            server.execute(() -> {
                Entity entity = player;

                FriendlyByteBuf serverBuf = PacketByteBufs.create();
                serverBuf.writeInt(entity.getId());
                serverBuf.writeInt(twirlState);

                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(target, TWIRL_STATE_S2C, serverBuf);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PLAYER_CONFIG_C2S, (server, player, handler, buf, responseSender) -> {
            CompoundTag configTag = buf.readNbt();

            server.execute(() -> {
                Entity entity = player;

                ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.put(entity.getId(), configTag);

                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    FriendlyByteBuf serverBuf = PacketByteBufs.create();
                    serverBuf.writeInt(entity.getId());
                    serverBuf.writeNbt(configTag);

                    ServerPlayNetworking.send(target, PLAYER_CONFIG_S2C, serverBuf);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LEGACY_PLAYER_CONFIG_C2S, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!playersReceivedWarnings.contains(player.getUUID())) {
                    player.displayClientMessage(
                            Component.literal("§cYou are using an outdated version of Elytra Contrails. To sync with this server, you must update to Elytra Contrails 1.4.0+"),
                            false
                    );
                }

                playersReceivedWarnings.add(player.getUUID());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(GET_ALL_REQUEST_C2S, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                for (Map.Entry<Integer, CompoundTag> configPair : ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.entrySet()) {
                    FriendlyByteBuf serverBuf = PacketByteBufs.create();
                    serverBuf.writeInt(configPair.getKey());
                    serverBuf.writeNbt(configPair.getValue());

                    ServerPlayNetworking.send(player, PLAYER_CONFIG_S2C, serverBuf);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(REMOVE_FROM_STORE_C2S, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ServerPlayerConfigStore.SERVER_PLAYER_CONFIGS.remove(player.getId());

                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    FriendlyByteBuf serverBuf = PacketByteBufs.create();
                    serverBuf.writeInt(player.getId());

                    ServerPlayNetworking.send(target, REMOVE_FROM_STORE_S2C, serverBuf);
                }
            });
        });
    }
}