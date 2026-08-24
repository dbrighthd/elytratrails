package dbrighthd.elytratrails.twirling;

import com.mojang.math.Axis;
import dbrighthd.elytratrails.network.NetworkTwirl;
import dbrighthd.elytratrails.twirling.types.EaseType;

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
        return new Twirl(EaseTypes.get(networkTwirl.twirlType()), networkTwirl.direction(), networkTwirl.twirlTime(), EaseTypes.EaseMode.values()[networkTwirl.easeMode()], EaseTypes.AxisType.values()[networkTwirl.axis()].axis(), networkTwirl.offset());
    }
}
