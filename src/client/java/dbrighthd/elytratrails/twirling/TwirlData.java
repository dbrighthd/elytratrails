package dbrighthd.elytratrails.twirling;

import com.mojang.math.Axis;
import dbrighthd.elytratrails.network.NetworkTwirlC2SPayload;
import dbrighthd.elytratrails.twirling.types.EaseType;
import dbrighthd.elytratrails.util.ElytraTimeUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 *  Each player has a TwirlData corresponding to them when they twirl. this holds information about the twirls they are doing, have done in the recent past, and will do soon.
 **/
public class TwirlData {

    boolean stagnant = false;
    boolean isClient;
    boolean hasSent;
    double twirlProgress;
    double twirlOffset;
    double prevTickBakedTwirlProgress;
    double bakedTwirlProgress;
    double prePacketTwirlProgress;
    double prevTwirlProgress;
    int noTwirlGracePeriodTicks;
    double easedTwirlProgress;
    long twirlStartTimeMillis;
    double packetSendProgressTime = 0.75;
    int currDirection;
    Axis lastKnownAxis;
    List<Twirl> twirlQueue = new ArrayList<>();

    /**
     * If this is the first time a player is doing a twirl, then the twirlStartTimeMillis needs to be set accordingly. TwirlData only gets created when a twirl is done.
     */
    public TwirlData()
    {
        twirlStartTimeMillis = ElytraTimeUtil.currentMillis();
    }

    /**
     * Run on every entity that has recently twirled
     * @param currentMillis current time in millis
     */
    public void updateTwirl(long currentMillis)
    {
        if(twirlQueue.isEmpty())
        {
            handleEmptyQueue(currentMillis);
            stagnant = noTwirlGracePeriodTicks <= 0;
            return;
        }

        prevTickBakedTwirlProgress = bakedTwirlProgress;
        prePacketTwirlProgress = prevTwirlProgress;
        prevTwirlProgress = twirlProgress;
        Twirl currTwirl = twirlQueue.getFirst();
        lastKnownAxis = currTwirl.axis();
        double packetGraceTime = (twirlQueue.size() > 1 && currTwirl.easeMode() == EaseTypes.EaseMode.OUT && currTwirl.easeType().flipTime() > 0 && currTwirl.easeType().flipTime() < 1) ? currTwirl.easeType().flipTime() : packetSendProgressTime;
        twirlProgress = twirlOffset + (double) (currentMillis - twirlStartTimeMillis) /currTwirl.twirlTime();
        if(twirlProgress > 1)
        {
            double millisToEnd = (1.0 - twirlOffset) * currTwirl.twirlTime();
            double overshootMillis = (currentMillis - twirlStartTimeMillis) - millisToEnd;
            if(twirlQueue.size() == 1)
            {
                twirlProgress = 1.0;
                currDirection = currTwirl.direction();
                easedTwirlProgress = doEase();
                bakedTwirlProgress = getFallbackBakedTwirlProgress(currTwirl);
                nextTwirl(currentMillis, 0, 0);
                return;
            }
            nextTwirl(currentMillis,0, overshootMillis);
        }
        else if(isClient && (twirlProgress > packetGraceTime) && !hasSent && twirlQueue.size() > 1)
        {
            sendProtectedTwirlPacket(twirlQueue.get(1));
            hasSent = true;
        }
        else if(twirlQueue.size() > 1 && currTwirl.easeMode() == EaseTypes.EaseMode.OUT)
        {
            double flipTime = currTwirl.easeType().flipTime();
            Twirl nextTwirl = twirlQueue.get(1);
            if(currTwirl.direction() != nextTwirl.direction() && currTwirl.axis().equals(nextTwirl.axis()) && currTwirl.easeType() == nextTwirl.easeType() && prePacketTwirlProgress <= flipTime && getApproxNextHalfTickTwirlProgress(currentMillis,currTwirl) >= flipTime)
            {
                nextTwirl(currentMillis, nextTwirl.easeType().flipStart(),0);
            }
        }
        currDirection = twirlQueue.isEmpty()? 1 : twirlQueue.getFirst().direction();
        easedTwirlProgress = doEase();
        bakedTwirlProgress = getBakedTwirlProgress();
    }

    /**
     * Go to the next twirl
     * @param currentMillis current time in millis
     * @param offset progress offset; needed for the in between twirls that start at 180 degrees!
     * @param overshootMillis we don't want to linger at 0 for a tick when the time indicates we should be past it!!
     */
    public void nextTwirl(long currentMillis, double offset, double overshootMillis)
    {
        twirlQueue.removeFirst();
        twirlProgress = offset;
        twirlOffset = offset;
        if(!twirlQueue.isEmpty())
        {
            twirlStartTimeMillis = currentMillis - (long) overshootMillis;
            Twirl nextTwirl = twirlQueue.getFirst();
            if(isClient && !hasSent)
            {
                sendProtectedTwirlPacket(nextTwirl);
            }
            twirlProgress = twirlOffset + (double)(currentMillis - twirlStartTimeMillis) / nextTwirl.twirlTime();
        }
        else
        {
            twirlStartTimeMillis = currentMillis;
            noTwirlGracePeriodTicks = 10;
        }
        hasSent = false;
    }

    /**
     * This is for the stitching between back twirls (or whichever ease type has stitching if a mod defines their own).
     * Getting a full tick in advance was too much and getting no tick in advance was too little so like. yea
     * @param currentMillis current time in millis
     * @param currTwirl current twirl
     * @return twirl progress next half tick
     */
    public double getApproxNextHalfTickTwirlProgress(long currentMillis, Twirl currTwirl)
    {
        return twirlOffset + (double) ((currentMillis + 25) - twirlStartTimeMillis) /currTwirl.twirlTime();
    }

    /**
     * Determined whether you can put in a standard in between 360 degree twirl, and does so.
     * @param twirlTime how long is the twirl?
     */
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
        Twirl twirlToSend = new Twirl(easeType,twirlQueue.getFirst().direction(),twirlTime/2, EaseTypes.EaseMode.BOTH, twirlQueue.getFirst().axis(), 0.5);
        sendTwirlPacket(twirlToSend);
        addTwirl(twirlQueue.size()-1,twirlToSend);
    }

    /**
     * Handles empty queue. This could mean that we are waiting for a packet from the other client, or it could mean
     * the client just hasn't done a twirl in a while. we don't know! If we are waiting for a twirl from the client, linger
     * at the current angle for a couple ticks. This might look weird, but with the way that twirl packets are overlapped with
     * their start time, the current system already accounts for lag, so this shouldn't happen often in normal play.
     * BUT in the event that it DOES happen, we don't want the player to snap to 0 angle if they are in the middle of a continuous
     * or otherwise offset twirl! this still looks odd, but looks significantly less odd, especially with the trails showing the exact
     * trajectory. Also, this fixes oddities that exist in flashback for some reason.
     * @param currentMillis current time in millis
     */
    public void handleEmptyQueue(long currentMillis)
    {
        twirlStartTimeMillis = currentMillis;
        if(noTwirlGracePeriodTicks > 0)
        {
            noTwirlGracePeriodTicks--;
            prevTickBakedTwirlProgress = bakedTwirlProgress;
            return;
        }
        easedTwirlProgress = 0;
        bakedTwirlProgress = 0;
        prevTickBakedTwirlProgress = 0;
    }

    /**
     * Does the easing and returns accordingly
     * @return eased twirl progress this tick
     */
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

    /**
     * Does the interpolation between angles and sends back what angle should be rendered this frame for the given player
     * @param partialTick progress in tick
     * @return angle to render at (radians)
     */
    public double getEasedTwirlAngleRadians(float partialTick)
    {
        return Mth.rotLerpRad(partialTick, (float)(prevTickBakedTwirlProgress * Math.TAU), (float)(bakedTwirlProgress * Math.TAU));
    }

    /**
     * Get the twirl progress, or 0. "baked" to mean that the match for direction and empty is already inputted
     * @return directional twirl progress
     */
    public double getBakedTwirlProgress()
    {
        return (easedTwirlProgress + (twirlQueue.isEmpty() ? 0 : twirlQueue.getFirst().offset())) * currDirection;
    }

    /**
     * get the fallback progress from the last known twirl (currTwirl)
     * @param currTwirl last known twirl
     * @return directional twirl progress
     */
    public double getFallbackBakedTwirlProgress(Twirl currTwirl)
    {
        return (easedTwirlProgress + currTwirl.offset()) * currDirection;
    }

    /**
     * add a twirl at a certain index
     * @param index of queue
     * @param twirl twirl to add
     */
    public void addTwirl(int index, Twirl twirl)
    {
        twirlQueue.add(index,twirl);
    }

    /**
     * add a twirl to end of twirl queue. all packets being received should do this!
     * @param twirl twirl to add to end of queue
     */
    public void addTwirlToEnd(Twirl twirl)
    {
        twirlQueue.addLast(twirl);
    }

    /**
     * pretty much get the estimated speed of rotation for the AOA approximation. not perfect but sells it okay I think
     * @return fraction of full twirl speed approximation
     */
    public float getTwirlAOAProgress()
    {
        if(twirlQueue.isEmpty())
        {
            return 0;
        }
        Twirl twirl = twirlQueue.getFirst();
        switch(twirl.easeMode()) {
            case EaseTypes.EaseMode.BOTH -> {
                return 1;
            }
            case EaseTypes.EaseMode.IN -> {
                return (float) twirlProgress;
            }
            default -> {
                return 1 - (float) twirlProgress;
            }
        }
    }

    /**
     * send a twirl packet to server
     * @param twirl twirl to send
     */
    public static void sendTwirlPacket(Twirl twirl) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return;
        NetworkTwirlC2SPayload payload = new NetworkTwirlC2SPayload(twirl.toNetworkTwirl());
        if (!ClientPlayNetworking.canSend(payload.type())) return;
        ClientPlayNetworking.send(payload);
    }

    /**
     * send a twirl packet to server
     * @param twirl twirl to send
     */
    public static void sendProtectedTwirlPacket(Twirl twirl) {
        if(twirl.easeMode() == EaseTypes.EaseMode.BOTH)
        {
            return;
        }
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return;
        NetworkTwirlC2SPayload payload = new NetworkTwirlC2SPayload(twirl.toNetworkTwirl());
        if (!ClientPlayNetworking.canSend(payload.type())) return;
        ClientPlayNetworking.send(payload);
    }


}
