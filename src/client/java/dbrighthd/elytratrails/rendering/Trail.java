package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.config.pack.ResolvedTrailSettings;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import dbrighthd.elytratrails.util.TimeUtil;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record Trail(Identifier texture, List<Point> points, ResolvedTrailSettings config, boolean isLeftWing, int entityId, int emitterIndex, Long trailId, RenderType renderType) {

    public static Trail fromPlayerConfig(int playerId, Emitter emitter, int index, long trailId) {
        PlayerConfig config = ClientPlayerConfigStore.getOrDefault(playerId);
        ResolvedTrailSettings resolvedTrailSettings = TrailPackConfigManager.resolve(emitter.modelName(), emitter.boneName(), config, emitter.isLeftWing());
        Identifier texture = TrailTextureRegistry.resolveTextureOrNull(resolvedTrailSettings.prideTrail());
        if (texture == null) texture = TrailRenderer.DEFAULT_TEXTURE;
        return new Trail(texture, new ArrayList<>(), resolvedTrailSettings, emitter.isLeftWing(), playerId, index, trailId, getRenderType(texture,resolvedTrailSettings));
    }
    /**
     * @param pos   position of trail point
     * @param epoch time of creation, in milliseconds
     */
    public record Point(Vec3 pos, long epoch, double speedAtEmission, boolean visible) {
        public Point(Vec3 pos, double speed, boolean visible) {
            this(pos, TimeUtil.currentMillis(), speed, visible);
        }
    }

    private static RenderType getRenderType(Identifier texture, ResolvedTrailSettings trailSettings) {
        if (trailSettings.glowingTrails()) {
            if (trailSettings.translucentTrails()) {
                if (trailSettings.wireframeTrails()) {
                    return TrailPipelines.entityTranslucentEmissiveWireFrame(texture);
                }
                return TrailPipelines.entityTranslucentEmissiveUnlit(texture);
            } else {
                if (trailSettings.wireframeTrails()) {
                    return TrailPipelines.entityCutoutEmissiveUnlitWireframe(texture);
                } else {
                    return TrailPipelines.entityCutoutEmissiveUnlit(texture);

                }
            }
        } else {
            if (trailSettings.wireframeTrails()) {
                return TrailPipelines.entityTranslucentCullWireFrame(texture);
            } else {
                if (trailSettings.translucentTrails()) {
                    return TrailPipelines.entityTranslucentCull(texture);
                } else {
                    return TrailPipelines.entityCutoutLit(texture);

                }
            }
        }
    }
}
