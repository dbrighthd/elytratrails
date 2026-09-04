package dbrighthd.elytratrails.twirling.types;

public class BackEase implements EaseType{

    @Override
    public String id() {
        return "Back";
    }

    @Override
    public double easeIn(double t) {
        double s = 2.5949095;
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return t * t * (t * (s + 1) - s);
    }

    @Override
    public double easeOut(double t) {
        double s = 2.5949095;
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return Math.pow((t - 1), 2) * ((t - 1) * (s + 1) + s) + 1;
    }

    @Override
    public double easeInOut(double t) {
        double s1 = 2.5949095;
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        if (t <= 0.5) {
            return 2 * t * t * (2 * t * (s1 + 1) - s1);
        }
        return 0.5 * (Math.pow(((2 * t) - 2), 2)) * ((2 * t - 2) * (s1 + 1) + s1) + 1;
    }

    @Override
    public double easeMult() {
        return 2.9931;
    }

    @Override
    public double flipStart() {
        return 0.481219;
    }

    @Override
    public double flipTime() {
        return 0.518781;
    }
}
