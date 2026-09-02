package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.config.pack.ResolvedTrailSettings;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import dbrighthd.elytratrails.util.ElytraTimeUtil;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record Trail(Identifier texture, List<Point> points, ResolvedTrailSettings config, boolean isLeftWing, int entityId, int emitterIndex, Long trailId, RenderType renderType, @Nullable ColorOverride colorOverride) {

    public static Trail createTrail(int playerId, Emitter emitter, int index, long trailId, ColorOverride colorOverride, ResolvedTrailSettings trailSettingOverrides) {
        PlayerConfig config = ClientPlayerConfigStore.getOrDefault(playerId);
        ResolvedTrailSettings resolvedTrailSettings = trailSettingOverrides != null? trailSettingOverrides : TrailPackConfigManager.resolve(emitter.modelName(), emitter.boneName(), config, emitter.isLeftWing());
        Identifier texture = TrailTextureRegistry.resolveTextureOrNull(resolvedTrailSettings.prideTrail());
        if (texture == null) texture = TrailRenderer.DEFAULT_TEXTURE;
        return new Trail(texture, new ArrayList<>(), resolvedTrailSettings, emitter.isLeftWing(), playerId, index, trailId, getRenderType(texture,resolvedTrailSettings), colorOverride);
    }

    /**
     *
     * @param pos position of trail point
     * @param epoch time of creation, in milliseconds
     * @param speedData AOA/Speed of player at this point
     * @param visible Should trail be visible at this point
     * @param posAtEmission Position at emission (will change from position if there is wind)
     * @param light Light level, if simplified light calc is on.
     */
    public record Point(Vec3 pos, long epoch, PlayerSpeedData speedData, boolean visible, Vec3 posAtEmission, int light) {
        public Point(Vec3 pos, PlayerSpeedData speed, boolean visible, int light) {
            this(pos, ElytraTimeUtil.currentMillis(), speed, visible, pos, light);
        }
        public Point addPositionOffset(Vec3 offset)
        {
            return new Point(pos.add(offset), epoch, speedData, visible, posAtEmission, light);
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
