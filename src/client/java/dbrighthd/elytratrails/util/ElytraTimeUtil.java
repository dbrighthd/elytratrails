package dbrighthd.elytratrails.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.TimeUtil;

public class ElytraTimeUtil {

    private static long tickCounter = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.isPaused()) {
                return;
            }
            tickCounter++;
        });
    }

    public static long currentMillis() {
        return (long) (tickCounter * 1000 + (Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false) * 1000)) / 20;
    }

    public static long currentNanos() {
        return TimeUtil.NANOSECONDS_PER_MILLISECOND * currentMillis();
    }
}