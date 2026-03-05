package dbrighthd.elytratrails.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/**
 * Call to this when you need a tick-based time (needed for flashback support to work well). Not sure if there's a better way to do this so if this is bad let me know
 */
public class TimeUtil {

    static long tickCounter = 0;
    public static void init()
    {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!client.isPaused())
            {
                tickCounter++;
            }

        });
    }
    public static long currentMillis()
    {
        return (long) (tickCounter*1000 + (Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false) * 1000))/20;
    }
    public static long currentNanos()
    {
        return (Util.NANOS_PER_MILLI * currentMillis());
    }
}
