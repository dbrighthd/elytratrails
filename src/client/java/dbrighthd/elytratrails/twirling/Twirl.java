package dbrighthd.elytratrails.twirling;

import dbrighthd.elytratrails.network.NetworkTwirl;
import dbrighthd.elytratrails.twirling.types.EaseType;

public record Twirl(EaseType easeType, int direction, long twirlTime, EaseTypes.EaseMode easeMode, double offset) {

    public Twirl(EaseType easeType, int direction, long twirlTime, EaseTypes.EaseMode easeMode)
    {
        this(easeType, direction, twirlTime, easeMode, 0);
    }
    public NetworkTwirl toNetworkTwirl()
    {
        return new NetworkTwirl(easeType.id(),direction,twirlTime,easeMode.ordinal(),offset);
    }

    public static Twirl fromNetworkTwirl(NetworkTwirl networkTwirl)
    {
        return new Twirl(EaseTypes.get(networkTwirl.twirlType()), networkTwirl.direction(), networkTwirl.twirlTime(), EaseTypes.EaseMode.values()[networkTwirl.easeMode()]);
    }
}
