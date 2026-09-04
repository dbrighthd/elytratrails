package dbrighthd.elytratrails.twirling.types;

public class SinEase implements EaseType{

    @Override
    public String id() {
        return "Sine";
    }

    @Override
    public double easeIn(double t) {
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return (1.0 - Math.cos((Math.PI * 0.5) * t));
    }

    @Override
    public double easeOut(double t) {
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return Math.sin((Math.PI * 0.5) * t);
    }

    @Override
    public double easeInOut(double t) {
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return 0.5 - 0.5 * Math.cos(Math.PI * t);
    }

    @Override
    public double easeMult()
    {
        return 1;
    }
}
