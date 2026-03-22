package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.ElytraTrailsClient;
import dbrighthd.elytratrails.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
public class TrailSystem {

    private static final WingTipSampler sampler = new WingTipSampler();
    private static final TrailManager manager = new TrailManager(sampler);
    private static final TrailRenderer renderer = new TrailRenderer(manager);

    public static void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(ctx -> {
            ModConfig config = ElytraTrailsClient.getConfig();
            if (!config.enableAllTrails) return;
            renderer.renderAllTrails(ctx, sampler.gatheredTrailsThisFrame);
        });
    }

    public static TrailManager getTrailManager() {
        return manager;
    }

    public static WingTipSampler getWingtipSampler() {
        return sampler;
    }
}
