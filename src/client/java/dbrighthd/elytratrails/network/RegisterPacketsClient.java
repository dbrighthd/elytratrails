package dbrighthd.elytratrails.network;

import dbrighthd.elytratrails.controller.EntityTwirlManager;
import dbrighthd.elytratrails.rendering.TrailSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.CLIENT_PLAYER_CONFIGS;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.getLocalPlayerConfigToSend;

public class RegisterPacketsClient {
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(TwirlStateS2CPayload.ID, (payload, context) ->
        {
            EntityTwirlManager.setEntityTwirlState(payload.entityId(), payload.twirlState());
        });
        ClientPlayNetworking.registerGlobalReceiver(PlayerConfigS2CPayload.ID, (payload, context) ->
        {
            TrailSystem.getTrailManager().removeTrail(payload.entityId());
            ClientPlayerConfigStore.putSafe(payload.entityId(),payload.playerConfig());
        });
        ClientPlayNetworking.registerGlobalReceiver(RemoveFromStoreS2CPayload.ID, (payload, context) ->
        {
            if(CLIENT_PLAYER_CONFIGS.containsKey(payload.entityId()))
            {
                ClientPlayerConfigStore.CLIENT_PLAYER_CONFIGS.remove(payload.entityId());
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            //if showTrailToOtherPlayers is turned on, we need to share that to other clients or else they will see the default.
            if(getConfig().shareTrail || !getConfig().showTrailToOtherPlayers)
            {
                ClientPlayNetworking.send(new PlayerConfigC2SPayload(getLocalPlayerConfigToSend()));
            }
            ClientPlayNetworking.send(new GetAllRequestC2SPayload());
        });
    }
}
