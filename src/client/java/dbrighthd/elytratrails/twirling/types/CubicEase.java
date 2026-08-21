package dbrighthd.elytratrails.twirling.types;

public class CubicEase implements EaseType{
    @Override
    public String id() {
        return "Cubic";
    }

    @Override
    public double easeIn(double t) {
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return t * t * t;
    }

    @Override
    public double easeOut(double t) {
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return Math.pow(t - 1, 3) + 1;
    }

    @Override
    public double easeInOut(double t) {
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        if (t <= 0.5) {
            return 4 * Math.pow(t, 3);
        }
        return 4 * Math.pow(t - 1, 3) + 1;
    }

    @Override
    public double easeMult()
    {
        return 1.9099;
    }
}
