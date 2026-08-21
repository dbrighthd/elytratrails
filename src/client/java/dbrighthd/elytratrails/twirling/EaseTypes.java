package dbrighthd.elytratrails.twirling;

import dbrighthd.elytratrails.twirling.types.*;

import java.util.HashMap;
import java.util.Map;

public class EaseTypes {
    private static final Map<String, EaseType> EASE_TYPES = new HashMap<>();
    public static void registerTypes()
    {
        register(new BackEase());
        register(new CubicEase());
        register(new ElasticEase());
        register(new ExpoEase());
        register(new LinearEase());
        register(new RandomEase());
        register(new SinEase());
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
