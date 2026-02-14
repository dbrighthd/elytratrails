package dbrighthd.elytratrails.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
public class ModelTransformationUtil {

    //for now I have the tips hardcoded, eventually I want to give support to EMF remodels.
    public static Vec3 HARDCODED_LEFT_WING_TIP = new Vec3(-10.0 / 16.0, 21.0 / 16.0, 2.0 / 16.0);
    public static Vec3 HARDCODED_RIGHT_WING_TIP = new Vec3( 10.0 / 16.0, 21.0 / 16.0, 2.0 / 16.0);
    public static Vec3 transformLocalPointThroughPart(PoseStack basePoseStack, ModelPart part, Vec3 localPoint) {
        basePoseStack.pushPose();
        part.translateAndRotate(basePoseStack);
        Vec3 transformed = transformPoint(basePoseStack.last().pose(), localPoint);
        basePoseStack.popPose();
        return transformed;
    }

    public static Vec3 transformPoint(Matrix4f matrix, Vec3 localPoint) {
        Vector4f homogeneous = new Vector4f((float) localPoint.x, (float) localPoint.y, (float) localPoint.z, 1.0f);
        homogeneous.mul(matrix);
        return new Vec3(homogeneous.x(), homogeneous.y(), homogeneous.z());
    }


    public static Vec3 computeWingTipLocal(ModelPart wingPart, boolean isLeftWing) {
        return isLeftWing
                ? HARDCODED_LEFT_WING_TIP
                : HARDCODED_RIGHT_WING_TIP;

        //the code below tried to get the actual value, but its the same every time unless EMF is loaded, but if EMF is loaded then these dont work and it goes to fallback anyway, so it's faster to just send the vectors as they were.
//        float minX = Float.POSITIVE_INFINITY;
//        float minY = Float.POSITIVE_INFINITY;
//        float minZ = Float.POSITIVE_INFINITY;
//
//        float maxX = Float.NEGATIVE_INFINITY;
//        float maxY = Float.NEGATIVE_INFINITY;
//        float maxZ = Float.NEGATIVE_INFINITY;
//
//        for (Object cubeObject : cubes) {
//            ModelPartCubeAccessor cube = (ModelPartCubeAccessor) cubeObject;
//
//            minX = Math.min(minX, cube.flyingparticles$getMinX());
//            minY = Math.min(minY, cube.flyingparticles$getMinY());
//            minZ = Math.min(minZ, cube.flyingparticles$getMinZ());
//
//            maxX = Math.max(maxX, cube.flyingparticles$getMaxX());
//            maxY = Math.max(maxY, cube.flyingparticles$getMaxY());
//            maxZ = Math.max(maxZ, cube.flyingparticles$getMaxZ());
//        }
//
//        // Keep your existing "tip = min + max" behavior
//        Vec3 localTipPixels = new Vec3((minX + maxX), (minY + maxY), (minZ + maxZ));
//
//        // Convert model pixels -> world units
//        return localTipPixels.scale(1.0 / 16.0);
    }

    public static float computeWingOpenness(ModelPart wingPart) {
        float wingRollAbs = Math.abs(wingPart.zRot);

        float fullyClosed = 0.28766277f;
        float fullyOpen = 1.5707302f;

        float openness = (wingRollAbs - fullyClosed) / (fullyOpen - fullyClosed);
        return Mth.clamp(openness, 0.0f, 1.0f);
    }
}
