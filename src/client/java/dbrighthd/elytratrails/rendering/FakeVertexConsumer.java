package dbrighthd.elytratrails.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;

//only exists to put the first person player after forcing it to "render", so that it wont actually render.
public final class FakeVertexConsumer implements VertexConsumer {

    public FakeVertexConsumer() {}

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        return this;
    }
}