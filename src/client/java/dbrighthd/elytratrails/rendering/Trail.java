package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.config.pack.ResolvedTrailSettings;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import dbrighthd.elytratrails.util.TimeUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record Trail(Identifier texture, List<Point> points, ResolvedTrailSettings config, boolean isLeftWing,
                    int entityId, int emitterIndex, Long trailId) {

    public static Trail fromPlayerConfig(int playerId, Emitter emitter, int index, long trailId) {
        PlayerConfig config = ClientPlayerConfigStore.getOrDefault(playerId);
        ResolvedTrailSettings resolvedTrailSettings = TrailPackConfigManager.resolve(emitter.modelName(), emitter.boneName(), config, emitter.isLeftWing());
        Identifier texture = TrailTextureRegistry.resolveTextureOrNull(resolvedTrailSettings.prideTrail());
        if (texture == null) texture = TrailRenderer.DEFAULT_TEXTURE;
        return new Trail(texture, new ArrayList<>(), resolvedTrailSettings, emitter.isLeftWing(), playerId, index, trailId);
    }

    @SuppressWarnings("unused")
    public float length() {
        float length = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            length += (float) points.get(i).pos().distanceTo(points.get(i + 1).pos());
        }
        return length;
    }

    /**
     * @param pos   position of trail point
     * @param epoch time of creation, in milliseconds
     */
    public record Point(Vec3 pos, long epoch, double speedAtEmission) {
        public Point(Vec3 pos, double speed) {
            this(pos, TimeUtil.currentMillis(), speed);
        }
    }
}
