package dbrighthd.elytratrails.twirling;

import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.network.NetworkTwirl;
import dbrighthd.elytratrails.twirling.types.EaseType;
import dbrighthd.elytratrails.util.ElytraTimeUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

import static dbrighthd.elytratrails.twirling.TwirlData.sendTwirlPacket;

public class TwirlManager {
    public static Map<Integer,TwirlData> twirlMap = new HashMap<>();

    public static void Init()
    {
        LevelRenderEvents.END_MAIN.register(_ -> updateAllTwirls());
    }
    public static float getExtraRollRadians(int entityId)
    {
        if(!twirlMap.containsKey(entityId))
        {
            return 0;
        }
        TwirlData twirlData = twirlMap.get(entityId);
        return (float)(twirlData.getEasedTwirlProgress() * Math.TAU);
    }
    public static float getTwirlProgress(int entityId)
    {
        TwirlData twirlData = twirlMap.get(entityId);
        return (float)twirlData.twirlProgress;
    }
    public static void updateAllTwirls()
    {
        long currentMillis = ElytraTimeUtil.currentMillis();
        for (TwirlData twirlData : twirlMap.values())
        {
            twirlData.updateTwirl(currentMillis);
        }
    }

    public static void receiveTwirlPacket(int entityId, NetworkTwirl networkTwirl)
    {
        Player player = Minecraft.getInstance().player;
        if(player == null)
        {
            return;
        }
        if(entityId == player.getId())
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



    public static void clientTwirlInput(int direction, ModConfig modConfig)
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
        sendEaseInOutTwirl(direction, modConfig, clientData);
    }
    public static void holdTwirlSend(int direction, ModConfig modConfig)
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
            if(direction == clientData.currDirection)
            {
                clientData.addInBetweenLinearTwirl((long)(modConfig.clientPlayerConfig.twirlTime * 1000));
            }
            else if(clientData.twirlQueue.size() < 3)
            {
                    sendEaseInOutTwirl(direction, modConfig, clientData);
            }
        }
        else
        {
            sendEaseInOutTwirl(direction, modConfig, clientData);
        }
    }

    private static void sendEaseInOutTwirl(int direction, ModConfig modConfig, TwirlData clientData)
    {
        EaseType easeType = EaseTypes.get(modConfig.clientPlayerConfig.easeType.name());
        double mult = easeType.easeMult();

        Twirl firstTwirl = new Twirl(easeType, direction, (long)(modConfig.clientPlayerConfig.twirlTime * 1000 * mult/2), EaseTypes.EaseMode.IN);
        clientData.addTwirlToEnd(firstTwirl);
        if(clientData.twirlQueue.size() == 1)
        {
            sendTwirlPacket(firstTwirl);
        }
        clientData.addTwirlToEnd(new Twirl(easeType, direction, (long)(modConfig.clientPlayerConfig.twirlTime * 1000 * mult/2), EaseTypes.EaseMode.OUT));
    }
    public static boolean isRolling(int entityId)
    {
        if(twirlMap.containsKey(entityId))
        {
            return !twirlMap.get(entityId).twirlQueue.isEmpty();
        }
        return false;
    }
}
