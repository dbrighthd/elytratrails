package dbrighthd.elytratrails.trailrendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.BiFunction;

public class TrailRenderType {
    public static void init()
    {
        RenderPipelines.register(PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT);
    }
    public static final RenderPipeline PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT =
            RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                    .withLocation(Identifier.parse("elytratrails:pipeline/entity_translucent_emissive_unlit"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withSampler("Sampler1")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withDepthWrite(false)
                    .build();

    private static final BiFunction<Identifier, Boolean, RenderType> RENDER_TYPE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT =
            Util.memoize((identifier, boolean_) -> {
                RenderSetup renderSetup = RenderSetup.builder(PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT)
                        .withTexture("Sampler0", identifier)
                        .useOverlay()
                        .affectsCrumbling()
                        .sortOnUpload()
                        .setOutline(boolean_ ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
                        .createRenderSetup();
                return RenderType.create("entity_translucent_emissive_unlit", renderSetup);
            });

    public static RenderType entityTranslucentEmissiveUnlit(Identifier texture) {
        return RENDER_TYPE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT.apply(texture, false);
    }
}
