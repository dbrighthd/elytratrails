package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.controller.EntityTwirlManager;
import dbrighthd.elytratrails.rendering.TrailSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.CLIENT_PLAYER_CONFIGS;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.getLocalPlayerConfigToSend;

public class RegisterPacketsClient {
    @Environment(EnvType.CLIENT)
    public static boolean hasRecievedThisSession = false;
    private static int pendingConfigRequestTicks = -1;

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(TwirlStateS2CPayload.ID, (payload, context) ->
                EntityTwirlManager.setEntityTwirlState(payload.entityId(), payload.twirlState()));
        ClientPlayNetworking.registerGlobalReceiver(PlayerConfigS2CPayload.ID, (payload, context) ->
        {
            TrailSystem.getTrailManager().removeTrail(payload.entityId());
            ClientPlayerConfigStore.putSafeInitial(payload.entityId(), payload.configTag());
        });
        ClientPlayNetworking.registerGlobalReceiver(RemoveFromStoreS2CPayload.ID, (payload, context) ->
        {
            if (CLIENT_PLAYER_CONFIGS.containsKey(payload.entityId())) {
                ClientPlayerConfigStore.CLIENT_PLAYER_CONFIGS.remove(payload.entityId());
            }
            TrailSystem.getWingtipSampler().removeFromEmfCache(payload.entityId());
        });
        ClientPlayNetworking.registerGlobalReceiver(LegacyPlayerConfigS2CPayload.ID, (payload, context) ->
        {
            if (!hasRecievedThisSession) {
                assert Minecraft.getInstance().player != null;
                Minecraft.getInstance().player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cThe server is using an outdated ElytraTrails mod or plugin. Please ask the server owner to update it to at least version 1.4.0 to work with your client."),
                        false
                );
            }
            hasRecievedThisSession = true;
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (getConfig().shareTrail || !getConfig().showTrailToOtherPlayers) {
                ClientPlayNetworking.send(new PlayerConfigC2SPayload(getLocalPlayerConfigToSend().toTag()));
            }

            pendingConfigRequestTicks = 10;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingConfigRequestTicks > 0) {
                pendingConfigRequestTicks--;
                if (pendingConfigRequestTicks == 0) {
                    ClientPlayNetworking.send(new GetAllRequestC2SPayload());
                }
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, sender) -> {
            TrailSystem.getWingtipSampler().removeAllEmfCache();
            hasRecievedThisSession = false;
            CLIENT_PLAYER_CONFIGS.clear();
        });
    }
}
