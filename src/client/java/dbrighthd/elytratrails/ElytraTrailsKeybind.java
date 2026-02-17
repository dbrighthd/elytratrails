// ElytraTrailsKeybind.java
package dbrighthd.elytratrails;

import com.mojang.blaze3d.platform.InputConstants;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.controller.ContinuousTwirlController;
import dbrighthd.elytratrails.controller.TwirlController;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.ElytraTrailsClient.setConfig;

public final class ElytraTrailsKeybind {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elytratrails", "skibidi"));

    public static KeyMapping DO_A_LIL_TWIRL_RANDOM;
    public static KeyMapping DO_A_LIL_TWIRL_L;
    public static KeyMapping DO_A_LIL_TWIRL_R;

    public static KeyMapping DO_A_LIL_CONTINUOUS_TWIRL_L;
    public static KeyMapping DO_A_LIL_CONTINUOUS_TWIRL_R;

    public static KeyMapping TOGGLE_TRAILS;

    public static void init() {
        DO_A_LIL_TWIRL_RANDOM = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.elytratrails.twirl_random",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_TWIRL_L = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.elytratrails.twirl_l",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_TWIRL_R = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.elytratrails.twirl_r",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_CONTINUOUS_TWIRL_L = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.elytratrails.continuous_twirl_l",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        DO_A_LIL_CONTINUOUS_TWIRL_R = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.elytratrails.continuous_twirl_r",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        TOGGLE_TRAILS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.elytratrails.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.isPaused()) return;

            boolean canTwirl = client.player.isFallFlying();

            // Normal twirl keys
            boolean lDown = canTwirl && DO_A_LIL_TWIRL_L.isDown();
            boolean rDown = canTwirl && DO_A_LIL_TWIRL_R.isDown();
            boolean randDown = canTwirl && DO_A_LIL_TWIRL_RANDOM.isDown();
            boolean normalDown = lDown || rDown || randDown;

            int desiredMode;
            if (lDown) desiredMode = -1;
            else if (rDown) desiredMode = +1;
            else if (randDown) desiredMode = 0;
            else desiredMode = +1;

            // Continuous twirl keys
            boolean cLDown = canTwirl && DO_A_LIL_CONTINUOUS_TWIRL_L.isDown();
            boolean cRDown = canTwirl && DO_A_LIL_CONTINUOUS_TWIRL_R.isDown();
            boolean continuousDown = cLDown || cRDown;

            int continuousMode;
            if (cLDown) continuousMode = -1;
            else if (cRDown) continuousMode = +1;
            else continuousMode = +1;

            boolean normalActive = TwirlController.isActive();
            boolean contActive = ContinuousTwirlController.isActive();

            if (normalActive) {
                TwirlController.tickTwirlKey(normalDown, desiredMode);
                ContinuousTwirlController.tickContinuousTwirlKey(false, continuousMode);
            } else if (contActive) {
                ContinuousTwirlController.tickContinuousTwirlKey(continuousDown, continuousMode);
                TwirlController.tickTwirlKey(false, desiredMode);
            } else if (normalDown) {
                TwirlController.tickTwirlKey(true, desiredMode);
                ContinuousTwirlController.tickContinuousTwirlKey(false, continuousMode);
            } else if (continuousDown) {
                ContinuousTwirlController.tickContinuousTwirlKey(true, continuousMode);
                TwirlController.tickTwirlKey(false, desiredMode);
            } else {
                TwirlController.tickTwirlKey(false, desiredMode);
                ContinuousTwirlController.tickContinuousTwirlKey(false, continuousMode);
            }

            while (TOGGLE_TRAILS.consumeClick()) {
                ModConfig modConfig = getConfig();
                modConfig.enableTrail = !modConfig.enableTrail;
                setConfig(modConfig);
            }
        });
    }

    private ElytraTrailsKeybind() {}
}
