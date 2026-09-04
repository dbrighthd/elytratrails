package dbrighthd.elytratrails;

import com.mojang.blaze3d.platform.InputConstants;
import dbrighthd.elytratrails.config.ConfigScreenBuilder;
import dbrighthd.elytratrails.config.FallbackConfigMessageScreen;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.twirling.TwirlManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.ElytraTrailsClient.setConfig;
import static dbrighthd.elytratrails.compat.ModStatuses.CLOTH_LOADED;

public final class ElytraTrailsKeybind {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elytratrails", "skibidi"));

    public static KeyMapping DO_A_LIL_ALTERNATING_TWIRL_ONE;
    public static KeyMapping DO_A_LIL_ALTERNATING_TWIRL_TWO;
    public static KeyMapping DO_A_LIL_TWIRL_L_ONE;
    public static KeyMapping DO_A_LIL_TWIRL_R_ONE;

    public static KeyMapping DO_A_LIL_TWIRL_L_TWO;
    public static KeyMapping DO_A_LIL_TWIRL_R_TWO;
    public static KeyMapping OPEN_SETTINGS;
    public static KeyMapping TOGGLE_TRAILS;

    public static int LEFT = -1;
    public static int RIGHT = 1;

    public static void init() {
        DO_A_LIL_ALTERNATING_TWIRL_ONE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.twirl_random",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));
        DO_A_LIL_ALTERNATING_TWIRL_TWO = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.twirl_random_two",
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

        DO_A_LIL_TWIRL_L_ONE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.continuous_twirl_l",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_TWIRL_R_ONE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.continuous_twirl_r",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_TWIRL_L_TWO = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.continuous_twirl_l_two",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_TWIRL_R_TWO = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytratrails.continuous_twirl_r_two",
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
            boolean can_twirl = client.player.isFallFlying() && modConfig.enableTwirls;
            if (DO_A_LIL_ALTERNATING_TWIRL_ONE.isDown())
            {
                if(can_twirl)
                {
                    TwirlManager.alternatingTwirlInput(modConfig, 1);
                }
            }
            if (DO_A_LIL_ALTERNATING_TWIRL_TWO.isDown())
            {
                if(can_twirl)
                {
                    TwirlManager.alternatingTwirlInput(modConfig, 2);
                }
            }
            handleTwirlKey(DO_A_LIL_TWIRL_L_ONE,LEFT,modConfig,1,can_twirl);
            handleTwirlKey(DO_A_LIL_TWIRL_R_ONE,RIGHT,modConfig,1,can_twirl);
            handleTwirlKey(DO_A_LIL_TWIRL_L_TWO,LEFT,modConfig,2,can_twirl);
            handleTwirlKey(DO_A_LIL_TWIRL_R_TWO,RIGHT,modConfig,2,can_twirl);

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
                Component status = Component.literal(modConfig.enableAllTrails ? "ON" : "OFF").withStyle(modConfig.enableAllTrails ? ChatFormatting.GREEN : ChatFormatting.RED);
                Component message = Component.empty().append(Component.literal("Toggled Elytra Contrails ")).append(status);
                Minecraft.getInstance().gui.hud.setOverlayMessage(message,false);
                setConfig(modConfig);
            }
        });
    }

    private static void handleTwirlKey(KeyMapping key, int direction, ModConfig modConfig, int twirlIndex, boolean canTwirl)
    {
        if (!canTwirl) {
            return;
        }
        if (key.isDown())
        {
            TwirlManager.holdTwirlSend(direction, modConfig, twirlIndex);
        }
    }
}