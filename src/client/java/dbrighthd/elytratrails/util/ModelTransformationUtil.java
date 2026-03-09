package dbrighthd.elytratrails.util;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;


public class ModelTransformationUtil {

    public static Vec3 VANILLA_LEFT_WING_TIP = new Vec3(-11.0 / 16.0, 21.0 / 16.0, 3.0 / 16.0);
    public static Vec3 VANILLA_RIGHT_WING_TIP = new Vec3( 11.0 / 16.0, 21.0 / 16.0, 3.0 / 16.0);
    public static Vec3 ZERO_WINGTIP = new Vec3(0.0 / 16.0, 0.0 / 16.0, 0.0 / 16.0);
    public static Vec3 VANILLA_ARROW_WINGTIP = new Vec3(-8.0 / 16.0, 0 / 16.0, 0.0 / 16.0);

    public static Vec3 transformPoint(Matrix4f matrix, Vec3 localPoint) {
        Vector4f homogeneous = new Vector4f((float) localPoint.x, (float) localPoint.y, (float) localPoint.z, 1.0f);
        homogeneous.mul(matrix);
        return new Vec3(homogeneous.x(), homogeneous.y(), homogeneous.z());
    }

    public static float computeWingOpenness(ModelPart wingPart) {
        float wingRollAbs = Math.abs(wingPart.zRot);

        float fullyClosed = 0.28766277f;
        //float fullyOpen = 1.5707302f;
        float fullyOpen = 1.57f;
        float openness = (wingRollAbs - fullyClosed) / (fullyOpen - fullyClosed);
        return Mth.clamp(openness, 0.0f, 1.0f);
    }

    public static float getSignedElytraAoARadiansFast(LivingEntity entity) {
        if (!entity.isFallFlying()) {
            return 1.0f;
        }

        Vec3 vel = entity.getDeltaMovement();
        double vx = vel.x;
        double vy = vel.y;
        double vz = vel.z;

        double speedSqr = vx * vx + vy * vy + vz * vz;
        if (speedSqr < 1.0e-12) {
            return 1.0f;
        }

        double invSpeed = Mth.invSqrt((float) speedSqr);
        double velHoriz = Math.sqrt(vx * vx + vz * vz) * invSpeed; // cos(velPitch)
        double velVert  = vy * invSpeed;                           // sin(velPitch)

        // Minecraft XRot is positive when looking downward.
        // Convert to standard math pitch where up is positive.
        float pitch = -entity.getXRot() * Mth.DEG_TO_RAD;

        float lookHoriz = Mth.cos(pitch); // cos(lookPitch)
        float lookVert  = Mth.sin(pitch); // sin(lookPitch)

        // cos(lookPitch - velPitch)
        float cosAoA = (float) (lookHoriz * velHoriz + lookVert * velVert);

        // sin(lookPitch - velPitch)
        float sinAoA = (float) (lookVert * velHoriz - lookHoriz * velVert);

        // AoA < 0  <=>  sin(AoA) < 0   (assuming normal small flight-angle ranges, which this is)
        return Math.abs(sinAoA < 0.0f ? cosAoA : 1.0f);
    }
}
