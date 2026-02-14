package dbrighthd.elytratrails.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;

public class IrisCompat {
    static{
        if(!FabricLoader.getInstance().isModLoaded("iris"))
        {
            throw new RuntimeException("iris isn't loaded.");
        }
    }
    public static boolean isShadowPassing()
    {
        return IrisApi.getInstance().isRenderingShadowPass();
    }

    public static boolean isUsingShaders()
    {
        return IrisApi.getInstance().isShaderPackInUse();
    }

}
