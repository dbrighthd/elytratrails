package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import dbrighthd.elytratrails.trailrendering.TrailTextureRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record Trail(Identifier texture, List<Point> points) {

    public static Trail fromPlayerConfig(int playerId) {
        PlayerConfig config = ClientPlayerConfigStore.getOrDefault(playerId);

        // add other fields to this record and populate them from the config here

        Identifier texture = TrailTextureRegistry.resolveTextureOrNull(config.prideTrail());
        if (texture == null) texture = TrailRenderer.DEFAULT_TEXTURE;
        return new Trail(texture, new ArrayList<>());
    }

    public float length() {
        float length = 0;
        for (int i = 0; i < points.size() - 1; i++) {
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
