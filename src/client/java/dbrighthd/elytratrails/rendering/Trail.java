package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import dbrighthd.elytratrails.trailrendering.TrailTextureRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record Trail(Identifier texture, List<Point> points, TrailPackConfigManager.ResolvedTrailSettings config, boolean flipUv) {

    public static Trail fromPlayerConfig(int playerId, boolean flipUv) {
        PlayerConfig config = ClientPlayerConfigStore.getOrDefault(playerId);

        // add other fields to this record and populate them from the config here

        Identifier texture = TrailTextureRegistry.resolveTextureOrNull(config.prideTrail());
        if (texture == null) texture = TrailRenderer.DEFAULT_TEXTURE;
        return new Trail(texture, new ArrayList<>(), TrailPackConfigManager.resolveFromPlayerConfig(config), flipUv);
    }

    public float length() {
        if(points.size() < 4)
        {
            return 0;
        }
        float length = 0;
        for (int i = 1; i < points.size() - 2; i++) {
            length += (float) points.get(i).pos().distanceTo(points.get(i + 1).pos());
        }
        return length;
    }

    /**
     * @param pos position of trail point
     * @param epoch time of creation, in milliseconds
     */
    public record Point(Vec3 pos, long epoch) {
        public Point(Vec3 pos) {
            this(pos, Util.getMillis());
        }
    }
}
