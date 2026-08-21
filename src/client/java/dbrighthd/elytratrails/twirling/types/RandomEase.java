package dbrighthd.elytratrails.twirling.types;

import java.util.Random;

public class RandomEase implements EaseType{
    static Random random = new Random();
    @Override
    public String id() {
        return "Random";
    }

    @Override
    public double easeIn(double t) {
        return random.nextDouble();
    }

    @Override
    public double easeOut(double t) {
        return random.nextDouble();
    }

    @Override
    public double easeInOut(double t) {
        return random.nextDouble();
    }

    @Override
    public double easeMult() {
        return 1;
    }
}
