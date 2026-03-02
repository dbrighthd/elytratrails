package dbrighthd.elytratrails.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

public class TimeUtil {

    static int tickCounter = 0;
    public static void init()
    {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;
        });
    }
    public static float currentTickPlusPartial()
    {
        return tickCounter + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
    public static long currentMillis()
    {
        return (long)tickToMillis(currentTickPlusPartial());
    }
    public static long currentNanos()
    {
        return tickToNanos(currentTickPlusPartial());
    }
    public static float tickToMillis(float tickTime)
    {
        return (tickTime/20)*1000;
    }
    public static long tickToNanos(float tickTime)
    {
        return (long)(Util.NANOS_PER_MILLI * tickToMillis(tickTime));
    }
}
