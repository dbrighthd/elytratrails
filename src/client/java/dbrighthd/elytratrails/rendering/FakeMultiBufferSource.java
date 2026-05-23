package dbrighthd.elytratrails.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

//only exists to put the first person player after forcing it to "render", so that it wont actually render.

public final class FakeMultiBufferSource implements MultiBufferSource {
    public static final FakeMultiBufferSource INSTANCE = new FakeMultiBufferSource();

    private FakeMultiBufferSource() {}

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return new FakeVertexConsumer();
    }
}