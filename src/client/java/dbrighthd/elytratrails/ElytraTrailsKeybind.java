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
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elytratrails", "skibidi"));

    public static KeyMapping DO_A_LIL_TWIRL_RANDOM;
    public static KeyMapping DO_A_LIL_TWIRL_L;
    public static KeyMapping DO_A_LIL_TWIRL_R;

    public static KeyMapping DO_A_LIL_CONTINUOUS_TWIRL_L;
    public static KeyMapping DO_A_LIL_CONTINUOUS_TWIRL_R;
    public static KeyMapping OPEN_SETTINGS;
    public static KeyMapping TOGGLE_TRAILS;

    public static int LEFT = -1;
    public static int RIGHT = 1;
    private static final int INPUT_BUFFER_TICKS = 4;

    private static boolean prevTwirlLDown;
    private static boolean prevTwirlRDown;
    private static boolean prevTwirlRandomDown;
    private static boolean prevContinuousLDown;
    private static boolean prevContinuousRDown;

    private static int bufferedNormalTicks;
    private static int bufferedNormalMode = 1;

    private static int bufferedContinuousTicks;
    private static int bufferedContinuousMode = 1;

    private static boolean queuedNormalRestart;
    private static int queuedNormalMode = 1;

    private static boolean queuedContinuousRestart;
    private static int queuedContinuousMode = 1;

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
            boolean canTwirl = client.player.isFallFlying();
            boolean inputBufferEnabled = modConfig.inputBuffer;

            while (DO_A_LIL_TWIRL_L.consumeClick())
            {
                TwirlManager.clientTwirlInput(LEFT, modConfig);
            }
            while (DO_A_LIL_TWIRL_R.consumeClick())
            {
                TwirlManager.clientTwirlInput(RIGHT,modConfig);
            }
            while (DO_A_LIL_CONTINUOUS_TWIRL_L.consumeClick())
            {
                TwirlManager.holdTwirlSend(LEFT, modConfig);
            }
            while (DO_A_LIL_CONTINUOUS_TWIRL_R.consumeClick())
            {
                TwirlManager.holdTwirlSend(RIGHT,modConfig);
            }
            boolean twirlLPhysicalDown = DO_A_LIL_TWIRL_L.isDown();
            boolean twirlRPhysicalDown = DO_A_LIL_TWIRL_R.isDown();
            boolean twirlRandomPhysicalDown = DO_A_LIL_TWIRL_RANDOM.isDown();

            boolean continuousLPhysicalDown = DO_A_LIL_CONTINUOUS_TWIRL_L.isDown();
            boolean continuousRPhysicalDown = DO_A_LIL_CONTINUOUS_TWIRL_R.isDown();
            prevTwirlLDown = twirlLPhysicalDown;
            prevTwirlRDown = twirlRPhysicalDown;
            prevTwirlRandomDown = twirlRandomPhysicalDown;
            prevContinuousLDown = continuousLPhysicalDown;
            prevContinuousRDown = continuousRPhysicalDown;



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

    private static void clearInputBuffers() {
        bufferedNormalTicks = 0;
        bufferedNormalMode = 1;

        bufferedContinuousTicks = 0;
        bufferedContinuousMode = 1;

        queuedNormalRestart = false;
        queuedNormalMode = 1;

        queuedContinuousRestart = false;
        queuedContinuousMode = 1;
    }

    private static void tickBuffers() {
        if (bufferedNormalTicks > 0) bufferedNormalTicks--;
        if (bufferedContinuousTicks > 0) bufferedContinuousTicks--;
    }

    private static void setNormalBuffer(int mode) {
        bufferedNormalTicks = INPUT_BUFFER_TICKS;
        bufferedNormalMode = mode;
    }

    private static void setContinuousBuffer(int mode) {
        bufferedContinuousTicks = INPUT_BUFFER_TICKS;
        bufferedContinuousMode = mode;
    }

    private static void queueNormalRestart(int mode) {
        queuedNormalRestart = true;
        queuedNormalMode = mode;
    }

    private static void queueContinuousRestart(int mode) {
        queuedContinuousRestart = true;
        queuedContinuousMode = mode;
    }

//    private static void handleNormalPress(int mode, boolean normalActive) {
//        if (mode != 0 && TwirlController.canBufferBackReverse(mode)) {
//            if (TwirlController.canStillReverseFromBufferedBackInput(mode)) {
//                TwirlController.bufferReverseRequest(mode, INPUT_BUFFER_TICKS);
//            } else {
//                queueNormalRestart(mode);
//            }
//            return;
//        }
//
//        if (normalActive) {
//            queueNormalRestart(mode);
//        } else {
//            setNormalBuffer(mode);
//        }
//    }
//
//    private static void handleContinuousPress(int mode, boolean contActive) {
//        if (ContinuousTwirlController.canBufferBackReverse(mode)) {
//            if (ContinuousTwirlController.canStillReverseFromBufferedBackInput(mode)) {
//                ContinuousTwirlController.bufferReverseRequest(mode, INPUT_BUFFER_TICKS);
//            } else {
//                queueContinuousRestart(mode);
//            }
//            return;
//        }
//
//        if (!contActive || ContinuousTwirlController.getCurrentDir() != mode) {
//            setContinuousBuffer(mode);
//        }
//    }

    /**
     * -1: for left
     * 1: for right
     * 0: for alernating
     * else Integer.MIN_VALUE
     */
    private static int firstPressedMode(boolean leftPressed, boolean rightPressed, boolean randomPressed, boolean allowRandom) {
        if (leftPressed) return -1;
        if (rightPressed) return 1;
        if (allowRandom && randomPressed) return 0;
        return Integer.MIN_VALUE;
    }

    private static int heldMode(boolean leftDown, boolean rightDown, boolean randomDown) {
        if (leftDown) return -1;
        if (rightDown) return 1;
        if (randomDown) return 0;
        return 1;
    }

    private ElytraTrailsKeybind() {
    }
}