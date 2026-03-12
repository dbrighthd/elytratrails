package dbrighthd.elytratrails;

import com.mojang.blaze3d.platform.InputConstants;
import dbrighthd.elytratrails.config.ConfigScreenBuilder;
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
    public static KeyMapping OPEN_SETTINGS;
    public static KeyMapping TOGGLE_TRAILS;

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
        DO_A_LIL_TWIRL_RANDOM = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.elytratrails.twirl_random",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));
        OPEN_SETTINGS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.elytratrails.open_settings",
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

            ModConfig modConfig = getConfig();
            boolean canTwirl = client.player.isFallFlying();
            boolean inputBufferEnabled = modConfig.inputBuffer;

            boolean twirlLPhysicalDown = DO_A_LIL_TWIRL_L.isDown();
            boolean twirlRPhysicalDown = DO_A_LIL_TWIRL_R.isDown();
            boolean twirlRandomPhysicalDown = DO_A_LIL_TWIRL_RANDOM.isDown();

            boolean continuousLPhysicalDown = DO_A_LIL_CONTINUOUS_TWIRL_L.isDown();
            boolean continuousRPhysicalDown = DO_A_LIL_CONTINUOUS_TWIRL_R.isDown();

            boolean twirlLPressed = twirlLPhysicalDown && !prevTwirlLDown;
            boolean twirlRPressed = twirlRPhysicalDown && !prevTwirlRDown;
            boolean twirlRandomPressed = twirlRandomPhysicalDown && !prevTwirlRandomDown;
            boolean continuousLPressed = continuousLPhysicalDown && !prevContinuousLDown;
            boolean continuousRPressed = continuousRPhysicalDown && !prevContinuousRDown;

            prevTwirlLDown = twirlLPhysicalDown;
            prevTwirlRDown = twirlRPhysicalDown;
            prevTwirlRandomDown = twirlRandomPhysicalDown;
            prevContinuousLDown = continuousLPhysicalDown;
            prevContinuousRDown = continuousRPhysicalDown;

            boolean normalActive = TwirlController.isActive();
            boolean contActive = ContinuousTwirlController.isActive();

            boolean normalDown = canTwirl && (twirlLPhysicalDown || twirlRPhysicalDown || twirlRandomPhysicalDown);
            int desiredMode = heldMode(
                    canTwirl && twirlLPhysicalDown,
                    canTwirl && twirlRPhysicalDown,
                    canTwirl && twirlRandomPhysicalDown
            );

            boolean continuousDown = canTwirl && (continuousLPhysicalDown || continuousRPhysicalDown);
            int continuousMode = heldMode(
                    canTwirl && continuousLPhysicalDown,
                    canTwirl && continuousRPhysicalDown,
                    false
            );

            if (inputBufferEnabled) {
                tickBuffers();

                if (canTwirl) {
                    int normalPressedMode = firstPressedMode(twirlLPressed, twirlRPressed, twirlRandomPressed, true);
                    if (normalPressedMode != Integer.MIN_VALUE) {
                        handleNormalPress(normalPressedMode, normalActive);
                    }

                    int continuousPressedMode = firstPressedMode(continuousLPressed, continuousRPressed, false, false);
                    if (continuousPressedMode != Integer.MIN_VALUE) {
                        handleContinuousPress(continuousPressedMode, contActive);
                    }
                }

                boolean bufferedNormalReady = canTwirl && (bufferedNormalTicks > 0 || queuedNormalRestart);
                boolean bufferedContinuousReady = canTwirl && (bufferedContinuousTicks > 0 || queuedContinuousRestart);

                if (normalActive) {
                    TwirlController.tickTwirlKey(normalDown, desiredMode);
                    ContinuousTwirlController.tickContinuousTwirlKey(false, continuousMode);
                } else if (contActive) {
                    ContinuousTwirlController.tickContinuousTwirlKey(continuousDown, continuousMode);
                    TwirlController.tickTwirlKey(false, desiredMode);
                } else if (bufferedNormalReady) {
                    int modeToUse = queuedNormalRestart ? queuedNormalMode : bufferedNormalMode;

                    TwirlController.tickTwirlKey(true, modeToUse);
                    ContinuousTwirlController.tickContinuousTwirlKey(false, continuousMode);

                    bufferedNormalTicks = 0;
                    queuedNormalRestart = false;
                } else if (bufferedContinuousReady) {
                    int modeToUse = queuedContinuousRestart ? queuedContinuousMode : bufferedContinuousMode;

                    ContinuousTwirlController.tickContinuousTwirlKey(true, modeToUse);
                    TwirlController.tickTwirlKey(false, desiredMode);

                    bufferedContinuousTicks = 0;
                    queuedContinuousRestart = false;
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
            } else {
                clearInputBuffers();

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
            }

            while (OPEN_SETTINGS.consumeClick()) {
                client.setScreen(ConfigScreenBuilder.buildConfigScreen(client.screen, modConfig));
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

    private static void handleNormalPress(int mode, boolean normalActive) {
        if (mode != 0 && TwirlController.canBufferBackReverse(mode)) {
            if (TwirlController.canStillReverseFromBufferedBackInput(mode)) {
                TwirlController.bufferReverseRequest(mode, INPUT_BUFFER_TICKS);
            } else {
                queueNormalRestart(mode);
            }
            return;
        }

        if (normalActive) {
            queueNormalRestart(mode);
        } else {
            setNormalBuffer(mode);
        }
    }

    private static void handleContinuousPress(int mode, boolean contActive) {
        if (ContinuousTwirlController.canBufferBackReverse(mode)) {
            if (ContinuousTwirlController.canStillReverseFromBufferedBackInput(mode)) {
                ContinuousTwirlController.bufferReverseRequest(mode, INPUT_BUFFER_TICKS);
            } else {
                queueContinuousRestart(mode);
            }
            return;
        }

        if (!contActive || ContinuousTwirlController.getCurrentDir() != mode) {
            setContinuousBuffer(mode);
        }
    }

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