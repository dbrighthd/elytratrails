package dbrighthd.elytratrails.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.BiFunction;
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

    public static final RenderPipeline PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT =
            RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                    .withLocation(Identifier.parse("elytratrails:pipeline/entity_translucent_emissive_unlit"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withSampler("Sampler1")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withDepthWrite(false)
                    .build());

    public static final RenderPipeline PIPELINE_ENTITY_CUTOUT_EMISSIVE_UNLIT =
            RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                    .withLocation(Identifier.parse("elytratrails:pipeline/entity_cutout_emissive_unlit"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withSampler("Sampler1")
                    .withCull(false)
                    .withDepthWrite(true)
                    .build());

    public static final RenderPipeline PIPELINE_ENTITY_CUTOUT_LIT =
            RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.parse("elytratrails:pipeline/entity_cutout_lit"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withSampler("Sampler1")
                    .withCull(false)
                    .withDepthWrite(true)
                    // Intentionally no blend (cutout)
                    .build());

    private static final BiFunction<Identifier, Boolean, RenderType> RENDER_TYPE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT =
            Util.memoize((identifier, outline) -> {
                RenderSetup renderSetup = RenderSetup.builder(PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT)
                        .withTexture("Sampler0", identifier)
                        .useOverlay()
                        .affectsCrumbling()
                        .sortOnUpload()
                        .setOutline(outline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
                        .createRenderSetup();
                return RenderType.create("elytratrails_entity_translucent_emissive_unlit", renderSetup);
            });

    private static final BiFunction<Identifier, Boolean, RenderType> RENDER_TYPE_ENTITY_CUTOUT_EMISSIVE_UNLIT =
            Util.memoize((identifier, outline) -> {
                RenderSetup renderSetup = RenderSetup.builder(PIPELINE_ENTITY_CUTOUT_EMISSIVE_UNLIT)
                        .withTexture("Sampler0", identifier)
                        .useOverlay()
                        .affectsCrumbling()
                        .sortOnUpload()
                        .setOutline(outline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
                        .createRenderSetup();
                return RenderType.create("elytratrails_entity_cutout_emissive_unlit", renderSetup);
            });

    private static final BiFunction<Identifier, Boolean, RenderType> RENDER_TYPE_ENTITY_CUTOUT_LIT =
            Util.memoize((identifier, outline) -> {
                RenderSetup renderSetup = RenderSetup.builder(PIPELINE_ENTITY_CUTOUT_LIT)
                        .withTexture("Sampler0", identifier)
                        .useOverlay()
                        .affectsCrumbling()
                        .sortOnUpload()
                        .setOutline(outline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
                        .createRenderSetup();
                return RenderType.create("elytratrails_entity_cutout_lit", renderSetup);
            });

    public static RenderType entityTranslucentEmissiveUnlit(Identifier texture) {
        return RENDER_TYPE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT.apply(texture, false);
    }

    public static RenderType entityCutoutEmissiveUnlit(Identifier texture) {
        return RENDER_TYPE_ENTITY_CUTOUT_EMISSIVE_UNLIT.apply(texture, false);
    }

    @SuppressWarnings("unused")
    public static RenderType entityCutoutLit(Identifier texture) {
        return RENDER_TYPE_ENTITY_CUTOUT_LIT.apply(texture, false);
    }

    public static void init() {}

    public static RenderType entityTranslucentCull(Identifier texture) {
        return ENTITY_TRANSLUCENT_CULL.apply(texture);
    }
}
