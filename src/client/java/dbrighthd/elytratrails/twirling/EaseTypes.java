package dbrighthd.elytratrails.twirling;

import dbrighthd.elytratrails.twirling.types.*;

import java.util.HashMap;
import java.util.Map;

public class EaseTypes {
    private static final Map<String, EaseType> EASE_TYPES = new HashMap<>();

    public static EaseType BACK;
    public static EaseType CUBIC;
    public static EaseType ELASTIC;
    public static EaseType LINEAR;
    public static EaseType RANDOM;
    public static EaseType EXPO;
    public static EaseType SINE;


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
    public static EaseType get(String id) {
        return EASE_TYPES.get(id);
    }

    public enum EaseMode {
        IN,
        OUT,
        BOTH
    }
}
