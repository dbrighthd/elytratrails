package dbrighthd.elytratrails.handler;

import dbrighthd.elytratrails.trailrendering.TrailRenderer;
import dbrighthd.elytratrails.trailrendering.TrailStore;
import dbrighthd.elytratrails.util.ShaderChecksUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import static dbrighthd.elytratrails.trailrendering.TrailRenderType.entityTranslucentEmissiveUnlit;
import static dbrighthd.elytratrails.trailrendering.TrailRenderer.TRAIL_TEX;

public final class TrailRenderHandler {
    private static final TrailRenderer trailRenderer = new TrailRenderer(TrailStore.TRAILS);

    public static void init()
    {
        WorldRenderEvents.AFTER_ENTITIES.register(worldRenderContext -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            RenderType elytraTrailRenderType = entityTranslucentEmissiveUnlit(TRAIL_TEX);
            if (ShaderChecksUtil.isUsingShaders()) {
                elytraTrailRenderType = RenderTypes.entityTranslucentEmissive(TRAIL_TEX);
            }

            worldRenderContext.commandQueue().order(1)
                    .submitCustomGeometry(worldRenderContext.matrices(),
                            elytraTrailRenderType,
                            trailRenderer);
        });
    }
}
