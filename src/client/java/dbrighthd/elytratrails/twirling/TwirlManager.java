package dbrighthd.elytratrails.twirling;

import com.mojang.math.Axis;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.network.NetworkTwirl;
import dbrighthd.elytratrails.twirling.types.EaseType;
import dbrighthd.elytratrails.util.ElytraTimeUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static dbrighthd.elytratrails.twirling.TwirlData.sendTwirlPacket;

public class TwirlManager {
    public static Map<Integer,TwirlData> twirlMap = new HashMap<>();

    public static void Init()
    {
        ClientTickEvents.END_CLIENT_TICK.register(TwirlManager::updateAllTwirls);
    }
    public static float getExtraRollRadians(int entityId, float partialTick)
    {
        if(!twirlMap.containsKey(entityId))
        {
            return 0;
        }
        TwirlData twirlData = twirlMap.get(entityId);
        return (float)(twirlData.getEasedTwirlAngleRadians(partialTick));
    }
    public static float getExtraRollRadiansFromUUID(UUID uuid)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if(level == null)
        {
            return  0;
        }
        Entity e = Minecraft.getInstance().level.getEntity(uuid);
        if(e == null)
        {
            return  0;
        }
        int entityId = e.getId();
        return getExtraRollRadians(entityId, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
    public static Axis getAxis(int entityId)
    {
        if(!twirlMap.containsKey(entityId))
        {
            return Axis.YP;
        }
        TwirlData twirlData = twirlMap.get(entityId);
        if(twirlData.twirlQueue.isEmpty())
        {
            return Objects.requireNonNullElse(twirlData.lastKnownAxis, Axis.YP);
        }
        return twirlData.twirlQueue.getFirst().axis();
    }

    public static Vector3f getCameraAxisVector(int entityId)
    {
        switch (EaseTypes.AxisType.fromAxis(getAxis(entityId)))
        {
            case EaseTypes.AxisType.X -> {return new Vector3f(-1,0,0);}
            case EaseTypes.AxisType.Y -> {return new Vector3f(0,0,1);}
            case EaseTypes.AxisType.Z -> {return new Vector3f(0,-1,0);}
        }
        return new Vector3f(1,0,0);
    }

    @SuppressWarnings("unused")
    public static float getTwirlProgress(int entityId)
    {
        if(!twirlMap.containsKey(entityId))
        {
            return 0;
        }
        TwirlData twirlData = twirlMap.get(entityId);
        return (float)twirlData.twirlProgress;
    }
    public static float getTwirlAOAProgress(int entityId)
    {
        if(!twirlMap.containsKey(entityId))
        {
            return 0;
        }
        TwirlData twirlData = twirlMap.get(entityId);
        return twirlData.getTwirlAOAProgress();
    }
    public static void updateAllTwirls(Minecraft minecraft)
    {
        if(minecraft.isPaused())
        {
            return;
        }
        long currentMillis = ElytraTimeUtil.currentMillis();
        for (TwirlData twirlData : twirlMap.values())
        {
            twirlData.updateTwirl(currentMillis);
        }
        ClientLevel level = Minecraft.getInstance().level;
        twirlMap.entrySet().removeIf(s -> removeTwirlCheck(s,level));
    }

    public static boolean removeTwirlCheck(Map.Entry<Integer,TwirlData> twirlDataEntry, ClientLevel level)
    {
        if(twirlDataEntry.getValue().stagnant)
        {
            return true;
        }
        if (level != null)
        {
            Entity e = level.getEntity(twirlDataEntry.getKey());
            if(e instanceof Player player)
            {
                return !player.isFallFlying();
            }
        }
        return false;
    }

    public static void receiveTwirlPacket(int entityId, NetworkTwirl networkTwirl)
    {
        Player player = Minecraft.getInstance().player;
        if(player == null || entityId == player.getId())
        {
            return;
        }
        if(twirlMap.containsKey(entityId))
        {
            twirlMap.get(entityId).addTwirlToEnd(Twirl.fromNetworkTwirl(networkTwirl));
        }
        else
        {
            twirlMap.put(entityId, new TwirlData());
            twirlMap.get(entityId).addTwirlToEnd(Twirl.fromNetworkTwirl(networkTwirl));
        }

    }

    public static void alternatingTwirlInput(ModConfig modConfig, int twirlIndex)
    {
        Player player = Minecraft.getInstance().player;
        if(player == null)
        {
            return;
        }
        int clientEid = player.getId();

        if(!twirlMap.containsKey(clientEid))
        {
            twirlMap.put(clientEid,new TwirlData());
            twirlMap.get(clientEid).isClient=true;
        }
        TwirlData clientData = twirlMap.get(clientEid);
        if(clientData.twirlQueue.size() > 1)
        {
            return;
        }
        int direction = 1;
        if(!clientData.twirlQueue.isEmpty())
        {
            direction = -1*clientData.currDirection;
        }
        sendEaseInOutTwirl(direction, modConfig, clientData, twirlIndex);
    }


    @SuppressWarnings("unused")
    public static void sendSingleTwirl(int direction, ModConfig modConfig, int twirlIndex)
    {
        Player player = Minecraft.getInstance().player;
        if(player == null)
        {
            return;
        }
        int clientEid = player.getId();

        if(!twirlMap.containsKey(clientEid))
        {
            twirlMap.put(clientEid,new TwirlData());
            twirlMap.get(clientEid).isClient=true;
        }
        TwirlData clientData = twirlMap.get(clientEid);
        if(clientData.twirlQueue.size() > 1)
        {
            return;
        }
        sendEaseInOutTwirl(direction, modConfig, clientData, twirlIndex);
    }

    public static void holdTwirlSend(int direction, ModConfig modConfig, int twirlIndex)
    {
        Player player = Minecraft.getInstance().player;
        if(player == null)
        {
            return;
        }
        int clientEid = player.getId();
        if(!twirlMap.containsKey(clientEid))
        {
            twirlMap.put(clientEid,new TwirlData());
            twirlMap.get(clientEid).isClient=true;
        }
        TwirlData clientData = twirlMap.get(clientEid);
        if(clientData.twirlQueue.size()>1)
        {
            if(direction == clientData.currDirection && getAxis(clientEid).equals(twirlAxisFromIndex(twirlIndex,modConfig)))
            {
                double twirlTime = twirlTimeFromIndex(twirlIndex, modConfig);
                clientData.addInBetweenLinearTwirl((long)(twirlTime * 1000));
            }
            else if(clientData.twirlQueue.size() < 3)
            {
                    sendEaseInOutTwirl(direction, modConfig, clientData, twirlIndex);
            }
        }
        else
        {
            sendEaseInOutTwirl(direction, modConfig, clientData, twirlIndex);
        }
    }

    private static void sendEaseInOutTwirl(int direction, ModConfig modConfig, TwirlData clientData, int twirlIndex)
    {
        double twirlTime = twirlTimeFromIndex(twirlIndex, modConfig);
        EaseType easeType = twirlEaseFromIndex(twirlIndex,modConfig);
        Axis axis = twirlAxisFromIndex(twirlIndex,modConfig);
        double mult = easeType.easeMult();

        Twirl firstTwirl = new Twirl(easeType, direction, (long)(twirlTime * 1000 * mult/2), EaseTypes.EaseMode.IN, axis);
        clientData.addTwirlToEnd(firstTwirl);
        if(clientData.twirlQueue.size() == 1)
        {
            sendTwirlPacket(firstTwirl);
        }
        clientData.addTwirlToEnd(new Twirl(easeType, direction, (long)(twirlTime * 1000 * mult/2), EaseTypes.EaseMode.OUT, axis));
    }
    public static boolean isRolling(int entityId)
    {
        if(twirlMap.containsKey(entityId))
        {
            return !twirlMap.get(entityId).twirlQueue.isEmpty();
        }
        return false;
    }

    public static double twirlTimeFromIndex(int index, ModConfig modConfig)
    {
        if (index == 1)
        {
            return modConfig.twirlOneTime;
        }
        return modConfig.twirlTwoTime;
    }
    public static Axis twirlAxisFromIndex(int index, ModConfig modConfig)
    {
        if (index == 1)
        {
            return modConfig.twirlOneAxis.axis();
        }
        return modConfig.twirlTwoAxis.axis();
    }
    public static EaseType twirlEaseFromIndex(int index, ModConfig modConfig)
    {
        if(index == 1)
        {
            return EaseTypes.get(modConfig.twirlOneEaseType);
        }
        return EaseTypes.get(modConfig.twirlTwoEaseType);
    }
}
