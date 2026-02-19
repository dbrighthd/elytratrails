package dbrighthd.elytratrails.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

public class TrailPipelines {
    public static final RenderPipeline PIPELINE_ENTITY_TRANSLUCENT_CULL = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "pipeline/entity_translucent_cull"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler1")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build());

    private static final Function<Identifier, RenderType> ENTITY_TRANSLUCENT_CULL = Util.memoize(
            (identifier) -> {
                RenderSetup renderSetup = RenderSetup.builder(PIPELINE_ENTITY_TRANSLUCENT_CULL)
                        .withTexture("Sampler0", identifier)
                        .useLightmap()
                        .useOverlay()
                        .affectsCrumbling()
                        .sortOnUpload()
                        .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                        .createRenderSetup();
                return RenderType.create("entity_translucent_cull", renderSetup);
            }
    );

    public static void init() {}

    public static RenderType entityTranslucentCull(Identifier texture) {
        return ENTITY_TRANSLUCENT_CULL.apply(texture);
    }
}
