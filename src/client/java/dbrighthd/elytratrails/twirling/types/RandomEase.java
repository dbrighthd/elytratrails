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
        return random.nextDouble()  * 3;
    }

    @Override
    public double easeOut(double t) {
        if(t >= 1)
        {
            return 1;
        }
        return random.nextDouble() * 3;
    }

    @Override
    public double easeInOut(double t) {
        return random.nextDouble()  * 3;
    }

    @Override
    public double easeMult() {
        return 1;
    }
}
