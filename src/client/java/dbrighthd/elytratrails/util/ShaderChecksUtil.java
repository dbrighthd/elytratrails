package dbrighthd.elytratrails.util;

import dbrighthd.elytratrails.compat.IrisCompat;
import net.fabricmc.loader.api.FabricLoader;

public final class ShaderChecksUtil {
    private ShaderChecksUtil() {}

    private static final boolean IRIS_LOADED  = FabricLoader.getInstance().isModLoaded("iris");

    public static boolean isShadowPass() {
        if (!IRIS_LOADED) return false;
        return IrisCompat.isShadowPassing();
    }
    @SuppressWarnings("unused")
    public static boolean isUsingShaders() {
        if (!IRIS_LOADED) return false;
        return IrisCompat.isUsingShaders();
    }
}
