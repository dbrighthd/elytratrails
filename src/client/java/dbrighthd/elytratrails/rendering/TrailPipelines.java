package dbrighthd.elytratrails.rendering;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.PolygonMode;
import dbrighthd.elytratrails.ElytraTrails;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.BiFunction;
import java.util.function.Function;

public class TrailPipelines {
    public static final ColorTargetState TRANSLUCENT_COLOR_STATE = new ColorTargetState(BlendFunction.TRANSLUCENT);
    public static final DepthStencilState DEFAULT_STENCIL_WITH_FALSE_DEPTHWRITE = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true);
    public static final BindGroupLayout EXAMPLE_LAYOUT = BindGroupLayout.builder()
            // Specifies that the shaders have a 'Sampler0' sampler
            .withSampler("Sampler1")
            // Specifies that the shaders have access to the 'Globals' uniform
            .build();
    public static final RenderPipeline PIPELINE_ENTITY_TRANSLUCENT_CULL = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "pipeline/entity_translucent_cull"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            //.withBlend(BlendFunction.TRANSLUCENT)
            .withColorTargetState(TRANSLUCENT_COLOR_STATE)
            .withDepthStencilState(DEFAULT_STENCIL_WITH_FALSE_DEPTHWRITE)
            .withCull(false)
            .withBindGroupLayout(EXAMPLE_LAYOUT)
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

    public static final RenderPipeline PIPELINE_ENTITY_TRANSLUCENT_CULL_WIREFRAME = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ElytraTrails.MOD_ID, "pipeline/entity_translucent_cull"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withPolygonMode(PolygonMode.WIREFRAME)
            .withColorTargetState(TRANSLUCENT_COLOR_STATE)
            .withDepthStencilState(DEFAULT_STENCIL_WITH_FALSE_DEPTHWRITE)
            .withCull(false)
            .withBindGroupLayout(EXAMPLE_LAYOUT)
            .build());

    private static final Function<Identifier, RenderType> ENTITY_TRANSLUCENT_CULL_WIREFRAME = Util.memoize(
            (identifier) -> {
                RenderSetup renderSetup = RenderSetup.builder(PIPELINE_ENTITY_TRANSLUCENT_CULL_WIREFRAME)
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
                    .withColorTargetState(TRANSLUCENT_COLOR_STATE)
                    .withCull(false)
                    .withBindGroupLayout(EXAMPLE_LAYOUT)
                    .withDepthStencilState(DEFAULT_STENCIL_WITH_FALSE_DEPTHWRITE)
                    .build());

    public static final RenderPipeline PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT_WIREFRAME =
            RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                    .withLocation(Identifier.parse("elytratrails:pipeline/entity_translucent_emissive_unlit"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withPolygonMode(PolygonMode.WIREFRAME)
                    .withColorTargetState(TRANSLUCENT_COLOR_STATE)
                    .withCull(false)
                    .withBindGroupLayout(EXAMPLE_LAYOUT)
                    .withDepthStencilState(DEFAULT_STENCIL_WITH_FALSE_DEPTHWRITE)
                    .build());

    public static final RenderPipeline PIPELINE_ENTITY_CUTOUT_EMISSIVE_UNLIT =
            RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                    .withLocation(Identifier.parse("elytratrails:pipeline/entity_cutout_emissive_unlit"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withCull(false)
                    .withBindGroupLayout(EXAMPLE_LAYOUT)
                    .build());

    public static final RenderPipeline PIPELINE_ENTITY_CUTOUT_EMISSIVE_UNLIT_WIREFRAME =
            RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                    .withLocation(Identifier.parse("elytratrails:pipeline/entity_cutout_emissive_unlit"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withCull(false)
                    .withBindGroupLayout(EXAMPLE_LAYOUT)
                    .build());


    public static final RenderPipeline PIPELINE_ENTITY_CUTOUT_LIT =
            RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.parse("elytratrails:pipeline/entity_cutout_lit"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withCull(false)
                    .withBindGroupLayout(EXAMPLE_LAYOUT)
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

    private static final BiFunction<Identifier, Boolean, RenderType> RENDER_TYPE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT_WIREFRAME =
            Util.memoize((identifier, outline) -> {
                RenderSetup renderSetup = RenderSetup.builder(PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT_WIREFRAME)
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

    private static final BiFunction<Identifier, Boolean, RenderType> RENDER_TYPE_ENTITY_CUTOUT_EMISSIVE_UNLIT_WIREFRAME =
            Util.memoize((identifier, outline) -> {
                RenderSetup renderSetup = RenderSetup.builder(PIPELINE_ENTITY_CUTOUT_EMISSIVE_UNLIT_WIREFRAME)
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
                        .useLightmap()
                        .affectsCrumbling()
                        .sortOnUpload()
                        .setOutline(outline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
                        .createRenderSetup();
                return RenderType.create("elytratrails_entity_cutout_lit", renderSetup);
            });

    public static RenderType entityTranslucentEmissiveUnlit(Identifier texture) {
        return RENDER_TYPE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT.apply(texture, false);
    }

    public static RenderType entityTranslucentEmissiveWireFrame(Identifier texture) {
        return RENDER_TYPE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT_WIREFRAME.apply(texture, false);
    }

    public static RenderType entityCutoutEmissiveUnlit(Identifier texture) {
        return RENDER_TYPE_ENTITY_CUTOUT_EMISSIVE_UNLIT.apply(texture, false);
    }

    public static RenderType entityCutoutEmissiveUnlitWireframe(Identifier texture) {
        return RENDER_TYPE_ENTITY_CUTOUT_EMISSIVE_UNLIT_WIREFRAME.apply(texture, false);
    }

    @SuppressWarnings("unused")
    public static RenderType entityCutoutLit(Identifier texture) {
        return RENDER_TYPE_ENTITY_CUTOUT_LIT.apply(texture, false);
    }

    public static void init() {
    }

    public static RenderType entityTranslucentCull(Identifier texture) {
        return ENTITY_TRANSLUCENT_CULL.apply(texture);
    }

    public static RenderType entityTranslucentCullWireFrame(Identifier texture) {
        return ENTITY_TRANSLUCENT_CULL_WIREFRAME.apply(texture);
    }

}
