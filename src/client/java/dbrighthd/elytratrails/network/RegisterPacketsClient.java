package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.controller.EntityTwirlManager;
import dbrighthd.elytratrails.rendering.TrailSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.CLIENT_PLAYER_CONFIGS;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.getLocalPlayerConfigToSend;

public class RegisterPacketsClient {
    @Environment(EnvType.CLIENT)
    public static boolean hasRecievedThisSession = false;

    private static int pendingConfigRequestTicks = -1;

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(RegisterPackets.TWIRL_STATE_S2C, (client, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            int twirlState = buf.readInt();

            client.execute(() -> {
                EntityTwirlManager.setEntityTwirlState(entityId, twirlState);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RegisterPackets.PLAYER_CONFIG_S2C, (client, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            CompoundTag configTag = buf.readNbt();

            client.execute(() -> {
                TrailSystem.getTrailManager().removeTrail(entityId);
                ClientPlayerConfigStore.putSafeInitial(entityId, configTag);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RegisterPackets.REMOVE_FROM_STORE_S2C, (client, handler, buf, responseSender) -> {
            int entityId = buf.readInt();

            client.execute(() -> {
                CLIENT_PLAYER_CONFIGS.remove(entityId);
//                TrailSystem.getWingtipSampler().removeFromEmfCache(entityId);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RegisterPackets.LEGACY_PLAYER_CONFIG_S2C, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                if (!hasRecievedThisSession) {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("§cThe server is using an outdated ElytraTrails mod or plugin. Please ask the server owner to update it to at least version 1.4.0 to work with your client."),
                                false
                        );
                    }
                }

                hasRecievedThisSession = true;
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (getConfig().shareTrail || !getConfig().showTrailToOtherPlayers) {
                ClientPackets.sendPlayerConfig(getLocalPlayerConfigToSend().toTag());
            }

            pendingConfigRequestTicks = 10;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingConfigRequestTicks > 0) {
                pendingConfigRequestTicks--;

                if (pendingConfigRequestTicks == 0) {
                    if (Minecraft.getInstance().level != null) {
                        ClientPackets.sendGetAllRequest();
                    }
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, sender) -> {
//            TrailSystem.getWingtipSampler().removeAllEmfCache();
            hasRecievedThisSession = false;
            CLIENT_PLAYER_CONFIGS.clear();
            ClientPackets.sendRemoveFromStore();
            pendingConfigRequestTicks = -1;
        });
    }
}