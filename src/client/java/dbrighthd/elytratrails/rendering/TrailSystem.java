package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.ElytraTrailsClient;
import dbrighthd.elytratrails.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class TrailSystem {

    private static final WingTipSampler sampler = new WingTipSampler();
    private static final TrailManager manager = new TrailManager(sampler);
    private static final TrailRenderer renderer = new TrailRenderer(manager);

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            ModConfig config = ElytraTrailsClient.getConfig();
            if (!config.enableAllTrails) return;
            renderer.renderAllTrails(config, ctx);
        });
    }
}
