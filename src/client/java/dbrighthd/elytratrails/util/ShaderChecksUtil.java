package dbrighthd.elytratrails.util;

import dbrighthd.elytratrails.compat.IrisCompat;
import net.fabricmc.loader.api.FabricLoader;

public final class ShaderChecksUtil {
    private ShaderChecksUtil() {}

    public static boolean isShadowPass() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return false;
        return IrisCompat.isShadowPassing();
    }
    @SuppressWarnings("unused")
    public static boolean isUsingShaders() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return false;
        return IrisCompat.isUsingShaders();
    }
}
