package dbrighthd.elytratrails.twirling.types;

public class ExpoEase implements EaseType{

    @Override
    public String id() {
        return "Expo";
    }

    @Override
    public double easeIn(double t) {
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return Math.pow(2, 10 * t - 10) - 0.001;
    }

    @Override
    public double easeOut(double t) {
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return 1.001 * -Math.pow(2, -10 * t) + 1;
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
            return 0.5 * Math.pow(2, 20 * t - 10) - 0.0005;
        }
        return 0.50025 * -Math.pow(2, -20 * t + 10) + 1;
    }

    @Override
    public double easeMult() {
        return 4.4171;
    }
}
