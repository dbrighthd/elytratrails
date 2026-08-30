package dbrighthd.elytratrails.twirling;

import com.mojang.math.Axis;
import dbrighthd.elytratrails.network.NetworkTwirl;
import dbrighthd.elytratrails.twirling.types.EaseType;

/**
 *  A twirl! instead of the old state system that derived from the playerConfig, each twirl is now an independent unit.
 *  This tells the other clients *exactly* how to render their twirl, without any ambiguity and without needing to fetch other info
 * @param easeType how to ease the twirl
 * @param direction what direction to twirl in
 * @param twirlTime how long should a twirl take
 * @param easeMode easing in, out, or both?
 * @param axis what axis to rotate along
 * @param offset what progress to start the twirl at (in between twirls need to be offset by 0.5, (pi radians, or 180 degrees))
 */
public record Twirl(EaseType easeType, int direction, long twirlTime, EaseTypes.EaseMode easeMode, Axis axis, double offset) {

    public Twirl(EaseType easeType, int direction, long twirlTime, EaseTypes.EaseMode easeMode, Axis axis)
    {
        this(easeType, direction, twirlTime, easeMode, axis, 0);
    }
    public NetworkTwirl toNetworkTwirl()
    {
        return new NetworkTwirl(easeType.id(),direction,twirlTime,easeMode.ordinal(), EaseTypes.AxisType.fromAxis(axis).ordinal(), offset);
    }

    public static Twirl fromNetworkTwirl(NetworkTwirl networkTwirl)
    {
        return new Twirl(EaseTypes.get(networkTwirl.twirlType()), (int)Math.signum(networkTwirl.direction()), networkTwirl.twirlTime(), EaseTypes.EaseMode.values()[networkTwirl.easeMode()], EaseTypes.AxisType.values()[networkTwirl.axis()].axis(), networkTwirl.offset());
    }
}
