package dbrighthd.elytratrails.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dbrighthd.elytratrails.ElytraTrailsClient;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.rendering.TrailSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererTrailPassMixin {

    @Shadow
    @Final
    private GameRenderer gameRenderer;

    @Shadow
    @Final
    private FeatureRenderDispatcher featureRenderDispatcher;

    @Unique
    private final SubmitNodeStorage elytraTrails$submitNodeStorage = new SubmitNodeStorage();

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;close()V",
                    shift = At.Shift.AFTER
            )
    )
    private void elytraTrails$renderAfterVanillaFrame(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci
    ) {
        if(TrailSystem.shouldUseLegacyRender())
        {
            return;
        }
        ModConfig config = ElytraTrailsClient.getConfig();
        if (!config.enableAllTrails) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Camera camera = this.gameRenderer.mainCamera();
        this.elytraTrails$submitNodeStorage.getSubmitsPerOrder().clear();
        PoseStack poseStack = new PoseStack();

        //the posestack is already popped at this point
        elytraTrails$applyCameraCorrection(poseStack, camera);
        TrailSystem.getTrailRenderer().submitAllTrails(poseStack, this.elytraTrails$submitNodeStorage, camera, TrailSystem.getWingtipSampler().gatheredTrailsThisFrame);

        if (this.elytraTrails$submitNodeStorage.getSubmitsPerOrder().isEmpty())
        {
            return;
        }

        FeatureRenderDispatcher.PreparedFrame trailFrame = this.featureRenderDispatcher.prepareFrame(this.elytraTrails$submitNodeStorage);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();

        try {
            modelViewStack.pushMatrix();
            modelViewStack.identity();
            RenderSystem.setShaderFog(terrainFog);
            RenderSystem.outputColorTextureOverride = this.gameRenderer.mainRenderTarget().getColorTextureView();
            RenderSystem.outputDepthTextureOverride = this.gameRenderer.mainRenderTarget().getDepthTextureView();

            trailFrame.executeSolid();
            trailFrame.executeTranslucent();
            trailFrame.executeTranslucentAfterTerrain();
        } finally {
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            modelViewStack.popMatrix();
            trailFrame.close();
        }
    }

    @Unique
    private static void elytraTrails$applyCameraCorrection(PoseStack poseStack, Camera camera) {
        poseStack.mulPose(camera.rotation().invert());

    }
}