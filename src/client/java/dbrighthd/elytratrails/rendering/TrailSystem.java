package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.ElytraTrailsClient;
import dbrighthd.elytratrails.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class TrailSystem {

    private static final WingTipSampler sampler = new WingTipSampler();
    private static final TrailManager manager = new TrailManager(sampler);
    private static final TrailRenderer renderer = new TrailRenderer(manager);

    public static void init() {
        WorldRenderEvents.END.register(ctx -> {
            ModConfig config = ElytraTrailsClient.getConfig();
            if (!config.enableAllTrails) return;
            renderer.renderAllTrails(ctx, sampler.gatherdTrailsThisFrameSnapCache);
            if (ctx.consumers() instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource) {
                bufferSource.endLastBatch();
            }
        });
    }

    public static TrailManager getTrailManager() {
        return manager;
    }

    public static WingTipSampler getWingtipSampler() {
        return sampler;
    }
}
