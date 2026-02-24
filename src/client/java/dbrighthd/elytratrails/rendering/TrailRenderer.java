package dbrighthd.elytratrails.rendering;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.rendering.math.SplineInterpolation;
import dbrighthd.elytratrails.util.ShaderChecksUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.minecraft.util.ARGB.alphaFloat;
import static net.minecraft.util.ARGB.color;

public class TrailRenderer {

    public static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath("elytratrails", "textures/trails/trail.png");

    private final @NotNull TrailManager manager;

    private float accumDist = 0.0f;
    private float endOfTrail = 0.0f;
    private ModConfig modConfig;
    private final PerlinNoise perlinNoise =  PerlinNoise.create(RandomSource.create(), List.of(1));
    private float totalTrailLength;
    private ClientPlayerConfigStore.TrailRenderSettings trailRenderSettings;
    boolean isFirstperson;
    //private static final float FP_CAMERA_FADE_ZERO = 0.3f;
    //private static final float FP_CAMERA_FADE_FULL = 0.5f;
    private static final float CAMERA_FADE_ZERO = 0.3f;
    private static final float CAMERA_FADE_FULL = 0.5f;

    public TrailRenderer(@NotNull TrailManager manager) {
        this.manager = manager;
    }

    @SuppressWarnings("resource")
    public void renderAllTrails(@NotNull WorldRenderContext ctx) {
        PoseStack stack = ctx.matrices();
        stack.pushPose();
        stack.translate(ctx.gameRenderer().getMainCamera().position().scale(-1f));
        for (Trail trail : manager.trails()) {
            List<Trail.Point> points = trail.points();
            if (points.size() < 4) continue; // splines :3

            //float length = trail.length();
            RenderType renderType = getRenderType(trail);


            ctx.commandQueue().order(1).submitCustomGeometry(stack, renderType, (pose, consumer) -> {
                Camera camera = ctx.gameRenderer().getMainCamera();

                totalTrailLength = 0f;
                for (int i = 0; i < points.size() - 1; i++) {
                    Trail.Point p0 = points.get(max(i - 1, 0));
                    Trail.Point p1 = points.get(i);
                    Trail.Point p2 = points.get(i + 1);
                    Trail.Point p3 = points.get(min(i + 2, points.size() - 1));

                    calculateSubdivideLength(p0, p1, p2, p3, 0f, 1f);
                }
                this.accumDist = 0f;
                this.endOfTrail = 0f;
                this.modConfig = getConfig();
                this.trailRenderSettings = ClientPlayerConfigStore.decodeTrailType(trail.config().trailType());

                this.isFirstperson = ((Minecraft.getInstance().player != null) && trail.entityId() == Minecraft.getInstance().player.getId()) && Minecraft.getInstance().options.getCameraType().isFirstPerson();
                for (int i = 0; i < points.size() - 1; i++) {
                    Trail.Point p0 = points.get(max(i - 1, 0));
                    Trail.Point p1 = points.get(i);
                    Trail.Point p2 = points.get(i + 1);
                    Trail.Point p3 = points.get(min(i + 2, points.size() - 1));

                    renderSubdividedSegment(pose, consumer, p0, p1, p2, p3, 0f, 1f, camera, trail, trail.config().color());
                }
            });
        }

        stack.popPose();
    }
    private RenderType getRenderType(Trail trail)
    {
        ClientPlayerConfigStore.TrailRenderSettings trailRenderSettings = ClientPlayerConfigStore.decodeTrailType(trail.config().trailType());
        if(trailRenderSettings.glowing())
        {
            if(ShaderChecksUtil.isUsingShaders())
            {
                return RenderTypes.entityTranslucentEmissive(trail.texture());
            }
            else
            {
                if(trailRenderSettings.translucent())
                {
                    return TrailPipelines.entityTranslucentEmissiveUnlit(trail.texture());
                }
                else
                {
                    return TrailPipelines.entityCutoutEmissiveUnlit(trail.texture());
                }
            }
        }
        else
        {
            if(ShaderChecksUtil.isUsingShaders())
            {
                return RenderTypes.entityTranslucent(trail.texture());
            }
            return TrailPipelines.entityTranslucentCull(trail.texture());
        }
    }
    private void renderSubdividedSegment(
            PoseStack.Pose pose, VertexConsumer consumer,
            Trail.Point point0, Trail.Point point1, Trail.Point point2, Trail.Point point3,
            float tStart, float tEnd,
            Camera camera, Trail trail,  int color
    ) {
        Vec3 p0 = point0.pos();
        Vec3 p1 = point1.pos();
        Vec3 p2 = point2.pos();
        Vec3 p3 = point3.pos();

        Vec3 startPos = SplineInterpolation.catmullRom(p0, p1, p2, p3, tStart);
        Vec3 endPos = SplineInterpolation.catmullRom(p0, p1, p2, p3, tEnd);
        float midT = (tStart + tEnd) / 2f;
        Vec3 midPos = SplineInterpolation.catmullRom(p0, p1, p2, p3, midT);

        Vec3 chord = endPos.subtract(startPos);
        double chordLenSq = chord.length();

        boolean needsSplit = false;
        if (chordLenSq > 0.01) {
            Vec3 toMid = midPos.subtract(startPos);
            double distFromChord = toMid.cross(chord).length() / chordLenSq;

            if (distFromChord > 0.02 && (tEnd - tStart) > 0.05) {
                needsSplit = true;
            }
        }

        if (needsSplit) {
            renderSubdividedSegment(pose, consumer, point0, point1, point2, point3, tStart, midT, camera, trail, color);
            renderSubdividedSegment(pose, consumer, point0, point1, point2, point3, midT, tEnd, camera, trail, color);
        } else {
            Vec3 startTan = SplineInterpolation.catmullRomTangent(p0, p1, p2, p3, tStart).normalize();
            Vec3 endTan = SplineInterpolation.catmullRomTangent(p0, p1, p2, p3, tEnd).normalize();

            Vec3 sideA = startTan.cross(startPos.subtract(camera.position()).normalize()).normalize();
            Vec3 sideB = endTan.cross(endPos.subtract(camera.position()).normalize()).normalize();

            float segmentLength = (float) startPos.distanceTo(endPos) * 2f;
            float v1 = this.accumDist / 2.0f;
            float v2 = (this.accumDist + segmentLength) / 2.0f;

            long epoch0 = point1.epoch();
            long epoch1 = point2.epoch();

            long currentTime = Util.getMillis();
            double start = Mth.lerp(tStart, epoch0, epoch1);
            double end = Mth.lerp(tEnd, epoch0, epoch1);



            float alphaEnd = computeLifetimeFadeout(end, currentTime, (long) (trail.config().trailLifetime() * 1000));
            float alphaStart = trail.config().fadeEnd() || alphaEnd <= 0 ? computeLifetimeFadeout(start, currentTime, (long) (trail.config().trailLifetime() * 1000)) : 1f;
            if(alphaEnd <= 0)
            {
                if (v1 > endOfTrail)
                {
                    endOfTrail = v1;
                }
            }
            else if(!trail.config().fadeEnd())
            {
                alphaEnd = 1f;

            }
            float scaleStart = computeWidthScaling(totalTrailLength- v1, -(endOfTrail - v1), trail.config());
            float scaleEnd = computeWidthScaling(totalTrailLength- v2, -(endOfTrail - v2), trail.config());



            //float scaleStart = 1f;
            // scaleEnd = 1f;

            if(isFirstperson && modConfig.fadeFirstPersonTrail && modConfig.firstPersonFadeTime > 0)
            {
                scaleStart *= firstPersonWidthFadeFactor(start, currentTime);
                scaleEnd *= firstPersonWidthFadeFactor(end, currentTime);
            }


            if(trail.config().enableRandomWidth())
            {
                scaleStart = scaleStart * (float)trail.config().randomWidthVariation() *((float)( perlinNoise.getValue(startPos.x,startPos.y,startPos.z)) + 1);
                scaleEnd = scaleEnd * (float)trail.config().randomWidthVariation() *((float)( perlinNoise.getValue(endPos.x,endPos.y,endPos.z)) + 1);

            }

            if(trail.config().fadeStart() && trailRenderSettings.translucent())
            {
                alphaStart *= computeStartFade(totalTrailLength- v1, trail.config());
                alphaEnd *= computeStartFade(totalTrailLength- v2, trail.config());
            }
            float halfWidthStart = (float) (trail.config().maxWidth() / 2f) * scaleStart;
            float halfWidthEnd = (float) (trail.config().maxWidth() / 2f) * scaleEnd;

            if ((scaleStart != 0 || scaleEnd != 0) && (alphaEnd != 0 || alphaStart != 0)) {
                if(!trailRenderSettings.translucent())
                {
                    alphaEnd = 1;
                    alphaStart = 1;
                }
                quadBetweenPoints(pose, consumer, startPos, endPos, sideA, sideB, halfWidthStart, halfWidthEnd, v1, v2, alphaStart, alphaEnd, trail.flipUv(), color);
            }
            this.accumDist += segmentLength;
        }
    }

    @SuppressWarnings("unused")
    private static float cameraDistanceFade(float cameraDistBlocks) {
        float denom = (CAMERA_FADE_FULL - CAMERA_FADE_ZERO);
        float t = (cameraDistBlocks - CAMERA_FADE_ZERO) / denom;
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private float firstPersonWidthFadeFactor(double epoch, long currentTime) {
        float fadeAmount = ((float)(currentTime - epoch))/1000;

        float fpCameraFadeFull = (float)modConfig.firstPersonFadeTime + 0.2f;
        float fpCameraFadeZero = (float)modConfig.firstPersonFadeTime;
        float fadeRange = fpCameraFadeFull - (float)modConfig.firstPersonFadeTime;
        if (fadeRange <= 1e-6f) {
            return (fadeAmount >= fpCameraFadeFull) ? 1.0f : 0.0f;
        }

        float normalizedFade = (fadeAmount - fpCameraFadeZero) / fadeRange;
        if (normalizedFade < 0f) normalizedFade = 0f;
        if (normalizedFade > 1f) normalizedFade = 1f;
        return normalizedFade;
    }

    private float computeWidthScaling(float distFromStart, float distToEnd, TrailPackConfigManager.ResolvedTrailSettings config) {
        float endRamp = (float) config.endRampDistance();
        float startRamp = (float) config.startRampDistance();
        float up;
        if (startRamp < 1e-6f){
            up = 1f;
        }
        else
        {
            if (distFromStart <= 0f) up = 0f;
            else if (distFromStart >= startRamp) up = 1f;
            else up = (float) Math.sin((distFromStart / startRamp) * (Math.PI / 2.0));
        }
        float down;
        if (distToEnd <= 0f) down = 0f;
        else if (distToEnd >= endRamp) down = 1f;
        else down = (float) Math.sin((distToEnd / endRamp) * (Math.PI / 2.0));

        float mult = min(up, down);
        return (float) Math.pow(mult, 0.9f);
    }

    private float computeStartFade(float distFromStart, TrailPackConfigManager.ResolvedTrailSettings cfg) {
        float startRamp = (float) cfg.fadeStartDistance();
        float up;
        if (startRamp < 1e-6f){
            up = 1f;
        }
        else
        {
            if (distFromStart <= 0f) up = 0f;
            else if (distFromStart >= startRamp) up = 1f;
            else up = (float) Math.sin((distFromStart / startRamp) * (Math.PI / 2.0));
        }
        return (float) Math.pow(up, 0.9f);
    }

    @SuppressWarnings("unused")
    private float computeWidthScalingButGood(float distFromStart, float distToEnd, TrailPackConfigManager.ResolvedTrailSettings config)
    {
        return computeWidthScalingStart(distFromStart, config) * computeWidthScalingEnd(distToEnd, config);
    }
    private float computeWidthScalingStart(float distFromStart, TrailPackConfigManager.ResolvedTrailSettings config)
    {
        if(distFromStart > config.startRampDistance())
        {
            return 1f;
        }
        if(distFromStart <= 0)
        {
            return 0f;
        }
        return (float) Math.sin(distFromStart / ((config.startRampDistance()) * (2 / Math.PI)));
    }
    private float computeWidthScalingEnd(float distToEnd, TrailPackConfigManager.ResolvedTrailSettings config)
    {
        if(distToEnd > config.endRampDistance())
        {
            return 1f;
        }
        if(distToEnd <= 0)
        {
            return 0;
        }
        return (float) Math.sin(distToEnd / ((config.endRampDistance()) * (2 / Math.PI)));
    }
    private float computeLifetimeFadeout(double epoch, long currentTime, long maxLifetime) {
        long age = (long) (currentTime - epoch);
        if (age >= maxLifetime) return 0.0f;
        else return 1.0f - (age / (float) maxLifetime);
    }

    @SuppressWarnings("unused")
    private float computeEnd(double epoch, long currentTime, long maxLifetime) {
        long age = (long) (currentTime - epoch);
        if (age >= maxLifetime) return 0.0f;
        else return 1.0f - (age / (float) maxLifetime);
    }

    private int computeLightTexture(Vec3 pos) { // note: I really hate this method, but I don't feel like managing the state that's required to do this in a better way
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || trailRenderSettings.glowing()) return LightTexture.FULL_BRIGHT;

        BlockPos blockPos = BlockPos.containing(pos);
        return LightTexture.pack(mc.level.getBrightness(LightLayer.BLOCK, blockPos), mc.level.getBrightness(LightLayer.SKY, blockPos));
    }

    private void quadBetweenPoints(
            PoseStack.Pose pose, VertexConsumer consumer,
            Vec3 a, Vec3 b, Vec3 sideA, Vec3 sideB,
            float halfWidthStart, float halfWidthEnd, float v1, float v2, float alphaStart, float alphaEnd, boolean flipUv, int color
    ) {
        Vector3f p1 = a.add(sideA.scale(halfWidthStart)).toVector3f();
        Vector3f p2 = b.add(sideB.scale(halfWidthEnd)).toVector3f();
        Vector3f p3 = b.subtract(sideB.scale(halfWidthEnd)).toVector3f();
        Vector3f p4 = a.subtract(sideA.scale(halfWidthStart)).toVector3f();

        int overlay = OverlayTexture.NO_OVERLAY;
        int lightStart = computeLightTexture(a);
        int lightEnd = computeLightTexture(b);

        int colorStart = multiplyAlpha(color, alphaStart);
        int colorEnd = multiplyAlpha(color, alphaEnd);

        float normalX = 0, normalY = -1, normalZ = 0;

        consumer.addVertex(pose, p1)
                .setNormal(normalX, normalY, normalZ)
                .setOverlay(overlay)
                .setLight(lightStart)
                .setColor(colorStart)
                .setUv(v1, 0);
        consumer.addVertex(pose, p2)
                .setNormal(normalX, normalY, normalZ)
                .setOverlay(overlay)
                .setLight(lightEnd)
                .setColor(colorEnd)
                .setUv(v2, 0);
        consumer.addVertex(pose, p3)
                .setNormal(normalX, normalY, normalZ)
                .setOverlay(overlay)
                .setLight(lightEnd)
                .setColor(colorEnd)
                .setUv(v2, flipUv ? 1 : -1);
        consumer.addVertex(pose, p4)
                .setNormal(normalX, normalY, normalZ)
                .setOverlay(overlay)
                .setLight(lightStart)
                .setColor(colorStart)
                .setUv(v1, flipUv ? 1 : -1);
    }

    private void calculateSubdivideLength( Trail.Point point0, Trail.Point point1, Trail.Point point2, Trail.Point point3,
            float tStart, float tEnd
    ) {
        Vec3 p0 = point0.pos();
        Vec3 p1 = point1.pos();
        Vec3 p2 = point2.pos();
        Vec3 p3 = point3.pos();

        Vec3 startPos = SplineInterpolation.catmullRom(p0, p1, p2, p3, tStart);
        Vec3 endPos = SplineInterpolation.catmullRom(p0, p1, p2, p3, tEnd);
        float midT = (tStart + tEnd) / 2f;
        Vec3 midPos = SplineInterpolation.catmullRom(p0, p1, p2, p3, midT);

        Vec3 chord = endPos.subtract(startPos);
        double chordLenSq = chord.length();

        boolean needsSplit = false;
        if (chordLenSq > 0.01) {
            Vec3 toMid = midPos.subtract(startPos);
            double distFromChord = toMid.cross(chord).length() / chordLenSq;

            if (distFromChord > 0.02 && (tEnd - tStart) > 0.05) {
                needsSplit = true;
            }
        }

        if (needsSplit) {
            calculateSubdivideLength(point0, point1, point2, point3, tStart, midT);
            calculateSubdivideLength(point0, point1, point2, point3, midT, tEnd);
        } else {
            this.totalTrailLength += (float) startPos.distanceTo(endPos);
        }
    }

    // minecraft's alpha multiply function sets color to 0 when alpha is 0, which breaks with our interpolation. this function does the same but just sets alpha to be min 0
    private int multiplyAlpha(int color, float alpha)
    {
        return alpha >= 1.0F ? color : color(alphaFloat(color) * max(0,alpha), color);
    }
}
