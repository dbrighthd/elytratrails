package dbrighthd.elytratrails.rendering.math;

import net.minecraft.world.phys.Vec3;

public class SplineInterpolation {
    public static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float t2 = t*t;
        float t3 = t2*t;
        return p1.scale(2)
                .add(p2.subtract(p0).scale(t))
                .add(p0.scale(2).subtract(p1.scale(5)).add(p2.scale(4)).subtract(p3).scale(t2))
                .add(p1.scale(3).subtract(p0).subtract(p2.scale(3)).add(p3).scale(t3))
                .scale(0.5f);
    }

    public static Vec3 catmullRomTangent(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float t2 = t*t;
        return p2.subtract(p0)
                .add(p0.scale(2).subtract(p1.scale(5)).add(p2.scale(4)).subtract(p3).scale(2 * t))
                .add(p1.scale(3).subtract(p0).subtract(p2.scale(3)).add(p3).scale(3 * t2))
                .scale(0.5f);
    }
}
