package dbrighthd.elytratrails.compat;

import dbrighthd.elytratrails.rendering.TrailPipelines;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

public class IrisCompat {
    static{
        if(!FabricLoader.getInstance().isModLoaded("iris"))
        {
            throw new RuntimeException("iris isn't loaded.");
        }
    }

    public static void registerPipelines() {
        IrisApi.getInstance().assignPipeline(TrailPipelines.PIPELINE_ENTITY_TRANSLUCENT_CULL, IrisProgram.ENTITIES_TRANSLUCENT);
        IrisApi.getInstance().assignPipeline(TrailPipelines.PIPELINE_ENTITY_CUTOUT_EMISSIVE_UNLIT, IrisProgram.EMISSIVE_ENTITIES);
        IrisApi.getInstance().assignPipeline(TrailPipelines.PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT, IrisProgram.EMISSIVE_ENTITIES);
        IrisApi.getInstance().assignPipeline(TrailPipelines.PIPELINE_ENTITY_TRANSLUCENT_CULL_WIREFRAME, IrisProgram.ENTITIES_TRANSLUCENT);
        IrisApi.getInstance().assignPipeline(TrailPipelines.PIPELINE_ENTITY_CUTOUT_EMISSIVE_UNLIT_WIREFRAME, IrisProgram.EMISSIVE_ENTITIES);
        IrisApi.getInstance().assignPipeline(TrailPipelines.PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT_WIREFRAME, IrisProgram.EMISSIVE_ENTITIES);
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
