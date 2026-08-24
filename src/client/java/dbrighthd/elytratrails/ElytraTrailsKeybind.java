package dbrighthd.elytratrails;

import com.mojang.blaze3d.platform.InputConstants;
import dbrighthd.elytratrails.config.ConfigScreenBuilder;
import dbrighthd.elytratrails.config.FallbackConfigMessageScreen;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.twirling.TwirlManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.ElytraTrailsClient.setConfig;
import static dbrighthd.elytratrails.compat.ModStatuses.CLOTH_LOADED;

public final class ElytraTrailsKeybind {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elytratrails", "skibidi"));

    public static KeyMapping DO_A_LIL_TWIRL_RANDOM;
    public static KeyMapping DO_A_LIL_TWIRL_L;
    public static KeyMapping DO_A_LIL_TWIRL_R;

    public static KeyMapping DO_A_LIL_CONTINUOUS_TWIRL_L;
    public static KeyMapping DO_A_LIL_CONTINUOUS_TWIRL_R;
    public static KeyMapping OPEN_SETTINGS;
    public static KeyMapping TOGGLE_TRAILS;

    public static int LEFT = -1;
    public static int RIGHT = 1;

    public static void init() {
        DO_A_LIL_TWIRL_RANDOM = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.twirl_random",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));
        OPEN_SETTINGS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.open_settings",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_TWIRL_L = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.twirl_l",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_TWIRL_R = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.twirl_r",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_CONTINUOUS_TWIRL_L = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.continuous_twirl_l",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_CONTINUOUS_TWIRL_R = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.continuous_twirl_r",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        TOGGLE_TRAILS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.isPaused()) return;

            ModConfig modConfig = getConfig();

            while (DO_A_LIL_TWIRL_L.consumeClick())
            {
                TwirlManager.clientTwirlInput(LEFT, modConfig);
            }
            while (DO_A_LIL_TWIRL_R.consumeClick())
            {
                TwirlManager.clientTwirlInput(RIGHT,modConfig);
            }
            if (DO_A_LIL_CONTINUOUS_TWIRL_L.isDown())
            {
                TwirlManager.holdTwirlSend(LEFT, modConfig);
            }
            if (DO_A_LIL_CONTINUOUS_TWIRL_R.isDown())
            {
                TwirlManager.holdTwirlSend(RIGHT,modConfig);
            }
            while (OPEN_SETTINGS.consumeClick()) {
                if(CLOTH_LOADED)
                {
                    client.setScreenAndShow(ConfigScreenBuilder.buildConfigScreen(client.gui.screen(), modConfig));
                }
                else
                {
                    client.setScreenAndShow(new FallbackConfigMessageScreen(client.gui.screen()));
                }
            }

            while (TOGGLE_TRAILS.consumeClick()) {
                modConfig.enableAllTrails = !modConfig.enableAllTrails;
                setConfig(modConfig);
            }
        });
    }

    private ElytraTrailsKeybind() {
    }
}