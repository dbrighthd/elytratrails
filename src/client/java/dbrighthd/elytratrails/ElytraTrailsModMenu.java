package dbrighthd.elytratrails;

import dbrighthd.elytratrails.config.ModConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dbrighthd.elytratrails.network.GetAllRequestC2SPayload;
import dbrighthd.elytratrails.network.PlayerConfigC2SPayload;
import dbrighthd.elytratrails.network.RemoveFromStoreC2SPayload;
import dbrighthd.elytratrails.network.RemoveFromStoreS2CPayload;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.network.ClientPlayerConfigStore.*;

public class ElytraTrailsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            Screen screen = AutoConfigClient.getConfigScreen(ModConfig.class, parent).get();

            if (screen instanceof AbstractConfigScreen cloth) {
                cloth.setSavingRunnable(() -> {
                    AutoConfig.getConfigHolder(ModConfig.class).save();

                    var mc = Minecraft.getInstance();
                    if (mc.getConnection() != null && mc.player != null && mc.level != null) {
                        //if showTrailToOtherPlayers is turned on, we need to share that to other clients or else they will see the default.
                        if(getConfig().shareTrail || !getConfig().showTrailToOtherPlayers)
                        {
                            ClientPlayNetworking.send(new PlayerConfigC2SPayload(getLocalPlayerConfigToSend()));
                        }
                        else
                        {
                            ClientPlayNetworking.send(new RemoveFromStoreC2SPayload());
                        }
                        if (!getConfig().syncWithServer) {
                            CLIENT_PLAYER_CONFIGS.clear();
                        } else if (CLIENT_PLAYER_CONFIGS.isEmpty()) {
                            ClientPlayNetworking.send(new GetAllRequestC2SPayload());
                        }
                    }
                });
            }

            return screen;
        };
    }

}