package dbrighthd.elytratrails.network;

import net.minecraft.nbt.CompoundTag;

public class CompoundTagOrDefaults {
    public static boolean getBooleanOr(CompoundTag tag, String key, boolean defaultVal)
    {
        if(tag.contains(key))
        {
            return tag.getBoolean(key);
        }
        return defaultVal;
    }
    public static String getStringOr(CompoundTag tag, String key, String defaultVal)
    {
        if(tag.contains(key))
        {
            return tag.getString(key);
        }
        return defaultVal;
    }
    public static double getDoubleOr(CompoundTag tag, String key, double defaultVal)
    {
        if(tag.contains(key))
        {
            return tag.getDouble(key);
        }
        return defaultVal;
    }
    public static int getIntOr(CompoundTag tag, String key, int defaultVal)
    {
        if(tag.contains(key))
        {
            return tag.getInt(key);
        }
        return defaultVal;
    }
}
