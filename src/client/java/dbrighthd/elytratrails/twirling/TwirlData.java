package dbrighthd.elytratrails.twirling;

import dbrighthd.elytratrails.network.NetworkTwirlC2SPayload;
import dbrighthd.elytratrails.twirling.types.EaseType;
import dbrighthd.elytratrails.util.ElytraTimeUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class TwirlData {
    boolean isClient;
    boolean hasSent;
    double twirlProgress;
    double twirlOffset;
    EaseType lastNonLinearEase;
    double easedTwirlProgress;
    long twirlStartTimeMillis;
    double packetSendProgressTime = 0.75;
    int currDirection;
    List<Twirl> twirlQueue = new ArrayList<>();

    public TwirlData()
    {
        twirlStartTimeMillis = ElytraTimeUtil.currentMillis();
    }

    public void updateTwirl(long currentMillis)
    {
        if(twirlQueue.isEmpty())
        {
            easedTwirlProgress = 0;
            twirlStartTimeMillis = ElytraTimeUtil.currentMillis();
            return;
        }
        Twirl currTwirl = twirlQueue.getFirst();
        double packetGraceTime = (twirlQueue.size() > 1 && currTwirl.easeMode() == EaseTypes.EaseMode.OUT && currTwirl.easeType().flipTime() > 0 && currTwirl.easeType().flipTime() < 1) ? currTwirl.easeType().flipTime() : packetSendProgressTime;
        twirlProgress = twirlOffset + (double) (currentMillis - twirlStartTimeMillis) /currTwirl.twirlTime();
        if(twirlProgress > 1)
        {
            nextTwirl(currentMillis,0);
        }
        else if(isClient && (twirlProgress > packetGraceTime) && !hasSent && twirlQueue.size() > 1)
        {
            sendTwirlPacket(twirlQueue.get(1));
            hasSent = true;
        }
        else if(twirlQueue.size() > 1 && currTwirl.easeMode() == EaseTypes.EaseMode.OUT)
        {
            double flipTime = currTwirl.easeType().flipTime();
            Twirl nextTwirl = twirlQueue.get(1);
            if(currTwirl.direction() != nextTwirl.direction())
            {
                if(twirlProgress > flipTime && twirlProgress < flipTime + 0.05)
                {
                    nextTwirl(currentMillis, nextTwirl.easeType().flipStart());
                }
            }
        }
        currDirection = twirlQueue.isEmpty()? 1 : twirlQueue.getFirst().direction();
        easedTwirlProgress = doEase();
    }
    public void nextTwirl(long currentMillis, double offset)
    {
        if(twirlQueue.getFirst().easeType() != EaseTypes.LINEAR)
        {
            lastNonLinearEase = twirlQueue.getLast().easeType();
        }
        twirlQueue.removeFirst();
        twirlProgress = offset;
        twirlOffset = offset;
        hasSent = false;
        twirlStartTimeMillis = currentMillis;
    }
    public void addInBetweenLinearTwirl(long twirlTime)
    {
        if(twirlQueue.size() != 2 || hasSent || twirlProgress < 0.6)
        {
            return;
        }
        EaseType easeType = EaseTypes.LINEAR;
        if(twirlQueue.getFirst().easeType() == EaseTypes.RANDOM)
        {
            easeType = EaseTypes.RANDOM;
        }
        addTwirl(twirlQueue.size()-1,new Twirl(easeType,twirlQueue.getFirst().direction(),twirlTime/2, EaseTypes.EaseMode.BOTH, 0.5));
    }

    public double doEase()
    {
        if(twirlQueue.isEmpty())
        {
            return 0;
        }
        Twirl currTwirl = twirlQueue.getFirst();
        switch(currTwirl.easeMode())
        {
            case IN -> {return currTwirl.easeType().easeIn(twirlProgress)/2;}
            case OUT -> {return 0.5 + currTwirl.easeType().easeOut(twirlProgress)/2;}
            case BOTH -> {return currTwirl.easeType().easeInOut(twirlProgress);}
            default -> {return 0;}
        }
    }

    public double getEasedTwirlProgress()
    {
        return (easedTwirlProgress + (twirlQueue.isEmpty() ? 0 : twirlQueue.getFirst().offset())) * currDirection;
    }

    public void addTwirl(int index, Twirl twirl)
    {
        twirlQueue.add(index,twirl);
    }

    public void addTwirlToEnd(Twirl twirl)
    {
        twirlQueue.addLast(twirl);
    }

    public static void sendTwirlPacket(Twirl twirl) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return;
        NetworkTwirlC2SPayload payload = new NetworkTwirlC2SPayload(twirl.toNetworkTwirl());
        if (!ClientPlayNetworking.canSend(payload.type())) return;
        ClientPlayNetworking.send(payload);
    }
}
