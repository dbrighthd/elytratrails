package dbrighthd.elytratrails.twirling;

import com.mojang.math.Axis;
import dbrighthd.elytratrails.twirling.types.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EaseTypes {
    private static final Map<String, EaseType> EASE_TYPES = new HashMap<>();

    public static EaseType BACK;
    public static EaseType CUBIC;
    public static EaseType ELASTIC;
    public static EaseType LINEAR;
    public static EaseType RANDOM;
    public static EaseType EXPO;
    public static EaseType SINE;
    static Random random = new Random();


    public static void registerTypes()
    {
        BACK = register(new BackEase());
        CUBIC = register(new CubicEase());
        ELASTIC = register(new ElasticEase());
        EXPO = register(new ExpoEase());
        LINEAR = register(new LinearEase());
        RANDOM = register(new RandomEase());
        SINE = register(new SinEase());
    }

    public static EaseType register(EaseType type)
    {
        EASE_TYPES.put(type.id(),type);
        return type;
    }

    public static List<String> getTypes()
    {
        return EASE_TYPES.keySet().stream().toList();
    }
    public static EaseType get(String id) {
        return EASE_TYPES.get(id);
    }

    public enum EaseMode {
        IN,
        OUT,
        BOTH
    }

    public enum AxisType {
        YP,
        XP,
        ZP,
        RANDOM;

        public Axis axis() {
            return switch (this) {
                case XP -> Axis.XP;
                case YP -> Axis.YP;
                case ZP -> Axis.ZP;
                case RANDOM -> randomAxis();
            };
        }

        public static Axis randomAxis()
        {
            return switch(random.nextInt(3)) {
                case 0 -> Axis.XP;
                case 1 -> Axis.ZP;
                default -> Axis.YP;
            };
        }

        public static AxisType fromAxis(Axis axis) {
            if (axis == Axis.XP) return XP;
            if (axis == Axis.YP) return YP;
            if (axis == Axis.ZP) return ZP;
            return YP;
        }
    }
}
