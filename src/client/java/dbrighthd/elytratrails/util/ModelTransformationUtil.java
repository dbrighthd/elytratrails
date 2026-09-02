package dbrighthd.elytratrails.util;

import dbrighthd.elytratrails.twirling.EaseTypes;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static dbrighthd.elytratrails.twirling.TwirlManager.getTwirlAOAProgress;


public class ModelTransformationUtil {

    public static final Vec3 VANILLA_LEFT_WING_TIP = new Vec3(-11.0 / 16.0, 21.0 / 16.0, 3.0 / 16.0);
    public static final Vec3 VANILLA_RIGHT_WING_TIP = new Vec3(11.0 / 16.0, 21.0 / 16.0, 3.0 / 16.0);

    public static Vec3 transformPoint(Matrix4f matrix, Vec3 localPoint) {
        Vector4f homogeneous = new Vector4f((float) localPoint.x, (float) localPoint.y, (float) localPoint.z, 1.0f);
        homogeneous.mul(matrix);
        return new Vec3(homogeneous.x(), homogeneous.y(), homogeneous.z());
    }

    public static float computeWingOpenness(ModelPart wingPart) {
        float wingRollAbs = Math.abs(wingPart.zRot);
        float fullyClosed = 0.28766277f;
        //float fullyOpen = 1.5707302f;
        float fullyOpen = 1.54f;
        float openness = (wingRollAbs - fullyClosed) / (fullyOpen - fullyClosed);
        return (float)EaseTypes.SINE.easeIn(openness);
        //return Mth.clamp(openness, 0.0f, 1.0f);
    }

    public static float getUnsignedAOA(LivingEntity entity)
    {
        float AOARaw = getUnsignedAOARaw(entity);

        //I want the twirl to affect the AOA of the emitters but I no no wanna do the math so here's an approximation
        return AOARaw + getTwirlAOAProgress(entity.getId())/1.5f;
    }


    public static float getUnsignedAOARaw(LivingEntity entity)
    {
        if (!entity.isFallFlying())
        {
            return 0.0f;
        }
        Vec3 lookVector = entity.getLookAngle();
        Vec3 motion = entity.getDeltaMovement();

        return (float) Math.acos(lookVector.dot(motion)/((lookVector.length() * motion.length())));
    }

    public static float getSignedElytraAoACalculation(LivingEntity entity) {
        if (!entity.isFallFlying()) {
            return 1.0f;
        }

        Vec3 vel = entity.getDeltaMovement();
        double vx = vel.x;
        double vy = vel.y;
        double vz = vel.z;

        double speedSqr = vel.lengthSqr();
        if (speedSqr < 1.0e-12) {
            return 1.0f;
        }

        double invSpeed = Mth.invSqrt((float) speedSqr);
        double velHoriz = Math.sqrt(vx * vx + vz * vz) * invSpeed;
        double velVert = vy * invSpeed;
        float pitch = -entity.getXRot() * Mth.DEG_TO_RAD;
        float lookHoriz = Mth.cos(pitch);
        float lookVert = Mth.sin(pitch);
        float cosAoA = (float) (lookHoriz * velHoriz + lookVert * velVert);
        float sinAoA = (float) (lookVert * velHoriz - lookHoriz * velVert);
        return Math.abs(sinAoA < 0.0f ? cosAoA : 1.0f);
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setupAnimUnchecked(EntityModel<?> model, EntityRenderState state) {
        ((EntityModel) model).setupAnim(state);
    }

    public static void setupAnyModelAnim(Object modelObj, Object stateObj) {
        if (!(modelObj instanceof EntityModel<?> model)) return;
        if (!(stateObj instanceof EntityRenderState state)) return;
        setupAnimUnchecked(model, state);
    }


}
