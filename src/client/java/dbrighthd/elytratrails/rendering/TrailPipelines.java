package dbrighthd.elytratrails.rendering;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;
import java.util.function.Function;

//yes yes i know FLAMGOP they aren't actually pipelines but I didnt want to refactor the code
public class TrailPipelines extends RenderStateShard {
    private static final int BUFFER_SIZE = 1024;

    private TrailPipelines(String name, Runnable setupState, Runnable clearState) {
        super(name, setupState, clearState);
    }
    private static final Function<ResourceLocation, RenderType> ENTITY_TRANSLUCENT_CULL = Util.memoize(texture -> {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)                        .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                .createCompositeState(RenderType.OutlineProperty.AFFECTS_OUTLINE);

        return RenderType.create(
                "entity_translucent_cull",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                true,
                true,
                state
        );
    });

    //idk how to do wireframe
    private static final Function<ResourceLocation, RenderType> ENTITY_TRANSLUCENT_CULL_WIREFRAME = Util.memoize(ENTITY_TRANSLUCENT_CULL);
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_UNLIT =
            Util.memoize((texture, outline) -> {
                RenderType.CompositeState state = RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setOverlayState(OVERLAY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                        .createCompositeState(outline ? RenderType.OutlineProperty.AFFECTS_OUTLINE : RenderType.OutlineProperty.NONE);
                return RenderType.create(
                        "entity_translucent_emissive_unlit",
                        DefaultVertexFormat.NEW_ENTITY,
                        VertexFormat.Mode.QUADS,
                        BUFFER_SIZE,
                        true,
                        true,
                        state
                );
            });

    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_UNLIT_WIREFRAME =
            Util.memoize(ENTITY_TRANSLUCENT_EMISSIVE_UNLIT
            );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_EMISSIVE_UNLIT =
            Util.memoize((texture, outline) -> {
                RenderType.CompositeState state = RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setCullState(NO_CULL)
                        .setOverlayState(OVERLAY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)

                        .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                        .createCompositeState(outline
                                ? RenderType.OutlineProperty.AFFECTS_OUTLINE
                                : RenderType.OutlineProperty.NONE);

                return RenderType.create(
                        "entity_cutout_emissive_unlit",
                        DefaultVertexFormat.NEW_ENTITY,
                        VertexFormat.Mode.QUADS,
                        BUFFER_SIZE,
                        true,
                        false,
                        state
                );
            });

    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_EMISSIVE_UNLIT_WIREFRAME =
            Util.memoize(ENTITY_CUTOUT_EMISSIVE_UNLIT
            );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_LIT =
            Util.memoize((texture, outline) -> {
                RenderType.CompositeState state = RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(outline
                                ? RenderType.OutlineProperty.AFFECTS_OUTLINE
                                : RenderType.OutlineProperty.NONE);

                return RenderType.create(
                        "entity_cutout_lit",
                        DefaultVertexFormat.NEW_ENTITY,
                        VertexFormat.Mode.QUADS,
                        BUFFER_SIZE,
                        true,
                        false,
                        state
                );
            });

    public static RenderType entityTranslucentCull(ResourceLocation texture) {
        return ENTITY_TRANSLUCENT_CULL.apply(texture);
    }

    public static RenderType entityTranslucentCullWireFrame(ResourceLocation texture) {
        return ENTITY_TRANSLUCENT_CULL_WIREFRAME.apply(texture);
    }

    public static RenderType entityTranslucentEmissiveUnlit(ResourceLocation texture) {
        return ENTITY_TRANSLUCENT_EMISSIVE_UNLIT.apply(texture, false);
    }

    public static RenderType entityTranslucentEmissiveWireFrame(ResourceLocation texture) {
        return ENTITY_TRANSLUCENT_EMISSIVE_UNLIT_WIREFRAME.apply(texture, false);
    }

    public static RenderType entityCutoutEmissiveUnlit(ResourceLocation texture) {
        return ENTITY_CUTOUT_EMISSIVE_UNLIT.apply(texture, false);
    }

    public static RenderType entityCutoutEmissiveUnlitWireframe(ResourceLocation texture) {
        return ENTITY_CUTOUT_EMISSIVE_UNLIT_WIREFRAME.apply(texture, false);
    }

    public static RenderType entityCutoutLit(ResourceLocation texture) {
        return ENTITY_CUTOUT_LIT.apply(texture, false);
    }

    public static void init() {
    }
}