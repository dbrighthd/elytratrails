package dbrighthd.elytratrails.trailrendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

public final class VertexBuilder {

    private final VertexConsumer vertexConsumer;
    private final PoseStack.Pose currentPose;

    private final int packedLight;
    private final int packedOverlay;

    public VertexBuilder(VertexConsumer vertexConsumer, PoseStack.Pose currentPose, int packedLight, int packedOverlay) {
        this.vertexConsumer = vertexConsumer;
        this.currentPose = currentPose;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
    }

    public void vert(
            Vec3 position,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha,
            float normalX,
            float normalY,
            float normalZ
    ) {
        vertexConsumer.addVertex(currentPose.pose(), (float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(currentPose, normalX, normalY, normalZ);
    }

    public void quadQuad(
            Vec3 startOuterCorner,
            Vec3 startInnerCorner,
            Vec3 endInnerCorner,
            Vec3 endOuterCorner,
            int alphaAtStart,
            int alphaAtEnd,
            float normalX,
            float normalY,
            float normalZ
    ) {
        vert(startOuterCorner, 0f, 0f, 255, 255, 255, alphaAtStart, normalX, normalY, normalZ);
        vert(startInnerCorner, 0f, 1f, 255, 255, 255, alphaAtStart, normalX, normalY, normalZ);
        vert(endInnerCorner,   1f, 1f, 255, 255, 255, alphaAtEnd,   normalX, normalY, normalZ);
        vert(endOuterCorner,   1f, 0f, 255, 255, 255, alphaAtEnd,   normalX, normalY, normalZ);
    }
}
