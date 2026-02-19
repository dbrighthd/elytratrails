package dbrighthd.elytratrails.handler;

import dbrighthd.elytratrails.trailrendering.TrailRenderer;
import dbrighthd.elytratrails.trailrendering.TrailStore;
import dbrighthd.elytratrails.trailrendering.TrailTextureRegistry;
import dbrighthd.elytratrails.util.ShaderChecksUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.trailrendering.TrailRenderType.*;
import static dbrighthd.elytratrails.trailrendering.TrailRenderer.TRAIL_TEX;

public final class TrailRenderHandler {

    private static final TrailRenderer normalRenderer = new TrailRenderer(TrailStore.TRAILS);

    /**
     * Cache pride renderers so we don't allocate a new TrailRenderer every frame.
     */
    private static final Map<Identifier, TrailRenderer> PRIDE_RENDERERS = new Object2ObjectOpenHashMap<>();

    // ---- RenderType factories initialized in init() ----
    private static Function<Identifier, RenderType> NORMAL_TYPE_PICKER;
    private static Function<Identifier, RenderType> PRIDE_TYPE_PICKER;

    public static void init() {
        // Build render-type pickers ONCE. They do not allocate per-frame; they just choose from memoized/vanilla cached RenderTypes.
        NORMAL_TYPE_PICKER = texture -> pickRenderType(texture);
        PRIDE_TYPE_PICKER = texture -> pickRenderType(texture);

        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            if (!getConfig().enableAllTrails) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            TrailStore.cleanup(Util.getNanos());

            // Only compute state + queue commands here
            ctx.commandQueue().order(0).submitCustomGeometry(
                    ctx.matrices(),
                    NORMAL_TYPE_PICKER.apply(TRAIL_TEX),
                    normalRenderer
            );

            Set<Identifier> prideTextures = collectActivePrideTextures();
            for (Identifier prideTex : prideTextures) {
                TrailRenderer prideRenderer = PRIDE_RENDERERS.computeIfAbsent(
                        prideTex,
                        tex -> new TrailRenderer(TrailStore.TRAILS)
                );

                ctx.commandQueue().order(1).submitCustomGeometry(
                        ctx.matrices(),
                        PRIDE_TYPE_PICKER.apply(prideTex),
                        prideRenderer
                );
            }

            if (!PRIDE_RENDERERS.isEmpty()) {
                PRIDE_RENDERERS.keySet().removeIf(tex -> !prideTextures.contains(tex));
            }
        });
    }

    /**
     * Centralized RenderType decision:
     * - translucentTrails controls translucent vs cutout
     * - glowingTrails controls emissive/unlit vs normal lit
     * - shaders controls whether we use vanilla emissive path or our unlit custom pipelines
     */
    private static RenderType pickRenderType(Identifier texture) {
        boolean shaders = ShaderChecksUtil.isUsingShaders();
        boolean translucent = getConfig().translucentTrails;
        boolean glowing = getConfig().glowingTrails;

        if (glowing) {
            if (shaders) {
                // Shaderpacks: use vanilla emissive (translucent emissive).
                // Note: vanilla doesn't provide a cutout-emissive RenderType here.
                return RenderTypes.entityTranslucentEmissive(texture);
            } else {
                // No shaders: use our custom "unlit emissive" pipelines, and respect translucentTrails with cutout.
                return translucent
                        ? entityTranslucentEmissiveUnlit(texture)
                        : entityCutoutEmissiveUnlit(texture);
            }
        } else {
            // Not glowing: use normal lit types, shader mode doesn't matter.
            return RenderTypes.entityTranslucent(texture);
        }
    }

    private static Set<Identifier> collectActivePrideTextures() {
        Set<Identifier> out = new ObjectOpenHashSet<>();

        dbrighthd.elytratrails.network.ClientPlayerConfigStore.CLIENT_PLAYER_CONFIGS.values().forEach(pc -> {
            Identifier id = TrailTextureRegistry.resolveTextureOrNull(pc.prideTrail());
            if (id != null) out.add(id);
        });

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            var local = dbrighthd.elytratrails.network.ClientPlayerConfigStore.getOrDefault(mc.player.getId());
            Identifier id = TrailTextureRegistry.resolveTextureOrNull(local.prideTrail());
            if (id != null) out.add(id);
        }

        return out;
    }
}
