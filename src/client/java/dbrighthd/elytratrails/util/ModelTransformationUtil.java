package dbrighthd.elytratrails.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
public class ModelTransformationUtil {

    public static Vec3 VANILLA_LEFT_WING_TIP = new Vec3(-10.0 / 16.0, 21.0 / 16.0, 2.0 / 16.0);
    public static Vec3 VANILLA_RIGHT_WING_TIP = new Vec3( 10.0 / 16.0, 21.0 / 16.0, 2.0 / 16.0);
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

    public static Vec3 computeWingTipLocal(boolean isLeftWing) {
        return isLeftWing
                ? VANILLA_LEFT_WING_TIP
                : VANILLA_RIGHT_WING_TIP;
    }

    public static float computeWingOpenness(ModelPart wingPart) {
        float wingRollAbs = Math.abs(wingPart.zRot);

        float fullyClosed = 0.28766277f;
        float fullyOpen = 1.5707302f;

        float openness = (wingRollAbs - fullyClosed) / (fullyOpen - fullyClosed);
        return Mth.clamp(openness, 0.0f, 1.0f);
    }
}
