package dbrighthd.elytratrails.twirling.types;

public class ElasticEase implements EaseType {
    @Override
    public String id() {
        return "Elastic";
    }

    @Override
    public double easeIn(double t) {
        double p = 0.45;
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return -Math.pow(2, 10 * (t - 1)) * Math.sin((2 * Math.PI * (t - 1 - (p / 4))) / p);
    }

    @Override
    public double easeOut(double t) {
        double p = 0.45;
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return Math.pow(2, -10 * t) * Math.sin((2 * Math.PI * (t - (p / 4))) / p) + 1;
    }

    @Override
    public double easeInOut(double t) {
        double p1 = 0.45;
        if (t < 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        double sin = Math.sin((2 * Math.PI * (2 * t - 1.1125) / p1));
        if (t <= 0.5) {
            return -0.5 * Math.pow(2, (20 * t - 10)) * sin;
        }
        return 0.5 * Math.pow(2, (-20 * t + 10)) * sin;
    }

    @Override
    public double easeMult() {
        return 4.4127;
    }
}
