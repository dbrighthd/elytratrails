package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.ElytraTrailsClient;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.util.ShaderChecksUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public class TrailSystem {

    private static final WingTipSampler sampler = new WingTipSampler();
    private static final TrailManager manager = new TrailManager(sampler);
    private static final TrailRenderer renderer = new TrailRenderer(manager);

    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
            if(shouldUseLegacyRender())
            {
                ModConfig config = ElytraTrailsClient.getConfig();
                if (!config.enableAllTrails) {
                    return;
                }

                renderer.renderAllTrails(ctx, sampler.gatheredTrailsThisFrame);
            }
        });
    }


    public static boolean shouldUseLegacyRender() {
        ModConfig config = ElytraTrailsClient.getConfig();
        switch(config.useOldRenderer)
        {
            case ALWAYS -> {return true;}
            case NEVER -> {return false;}
            default -> {return ShaderChecksUtil.isUsingShaders();}
        }
    }

    public static TrailManager getTrailManager() {
        return manager;
    }

    public static WingTipSampler getWingtipSampler() {
        return sampler;
    }

    public static TrailRenderer getTrailRenderer() {
        return renderer;
    }
}