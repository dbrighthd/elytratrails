package dbrighthd.elytratrails.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.config.pack.ResolvedTrailSettings;
import dbrighthd.elytratrails.rendering.math.SplineInterpolation;
import dbrighthd.elytratrails.util.TimeUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.minecraft.util.ARGB.*;

/**
 * Handles trail rendering
 */
public class TrailRenderer {

    public static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath("elytratrails", "textures/trails/trail.png");

    private final @NotNull TrailManager manager;

    private float accumDist = 0.0f;
    private ModConfig modConfig;
    private final PerlinNoise perlinNoise =  PerlinNoise.create(RandomSource.create(), List.of(1));
    private float totalTrailLength;
    boolean isFirstPerson;
    boolean atEnd;
    Minecraft minecraft;
    private static final Logger LOGGER = LoggerFactory.getLogger(TrailRenderer.class);
    private static final float CAMERA_FADE_ZERO = 0.5f;
    private static final float CAMERA_FADE_FULL = 0.7f;
    private static float endCorrection = 0f;
    private Vec3 cameraPosition;
    long currentTime;
    public TrailRenderer(@NotNull TrailManager manager) {
        this.manager = manager;
    }

    @SuppressWarnings("resource")
    public void renderAllTrails(@NotNull WorldRenderContext ctx, Map<Integer, List<Emitter>> gatheredThisFrame) {
        PoseStack stack = ctx.matrices();
        minecraft = Minecraft.getInstance();
        stack.pushPose();

        Camera camera = ctx.gameRenderer().getMainCamera();

        modConfig = getConfig();
        cameraPosition = camera.position();
        stack.translate(cameraPosition.scale(-1f));
        for (Trail trail : manager.trails()) {
            List<Trail.Point> points = trail.points();
            int size = points.size();
            if (size < 4) continue;

            ResolvedTrailSettings trailSettings = trail.config();
            RenderType renderType = getRenderType(trail, trailSettings);

            final int last = size - 1;

            Trail.Point snappedLastPoint = null;
            if (modConfig.alwaysSnapTrail && size > 4) {
                List<Emitter> emitters = gatheredThisFrame.get(trail.entityId());
                if (emitters != null && manager.isActiveTrail(trail)) {
                    snappedLastPoint = copyTrailPointNewPos(
                            points.get(last),
                            emitters.get(trail.emitterIndex()).position()
                    );
                }
            }

            final Trail.Point effectiveLastPoint = snappedLastPoint != null ? snappedLastPoint : points.get(last);

            ctx.commandQueue().order(1).submitCustomGeometry(stack, renderType, (pose, consumer) -> {
                totalTrailLength = 0f;
                currentTime = TimeUtil.currentMillis();
                for (int i = 0; i < last; i++) {
                    int i0 = (i > 0) ? i - 1 : 0;
                    int i2 = i + 1;
                    int i3 = (i + 2 < size) ? i + 2 : last;

                    Trail.Point p0 = points.get(i0);
                    Trail.Point p1 = points.get(i);
                    Trail.Point p2 = (i2 == last) ? effectiveLastPoint : points.get(i2);
                    Trail.Point p3 = (i3 == last) ? effectiveLastPoint : points.get(i3);

                    calculateSubdivideLength(p0, p1, p2, p3, 0f, 1f);
                }

                totalTrailLength -= (float) trailSettings.distanceTillTrailStart();
                totalTrailLength = max(totalTrailLength, 0);
                endCorrection = 0f;
                this.accumDist = 0f;

                this.atEnd = false;
                this.isFirstPerson =
                        ((minecraft.player != null)
                                && trail.entityId() == minecraft.player.getId())
                                && minecraft.options.getCameraType().isFirstPerson()
                                && minecraft.getCameraEntity() == minecraft.player;

                for (int i = 0; i < last; i++) {
                    int i0 = (i > 0) ? i - 1 : 0;
                    int i2 = i + 1;
                    int i3 = (i + 2 < size) ? i + 2 : last;

                    Trail.Point p0 = points.get(i0);
                    Trail.Point p1 = points.get(i);
                    Trail.Point p2 = (i2 == last) ? effectiveLastPoint : points.get(i2);
                    Trail.Point p3 = (i3 == last) ? effectiveLastPoint : points.get(i3);

                    renderSubdividedSegment(pose, consumer, p0, p1, p2, p3, 0f, 1f, trail, getTrailColor(trailSettings,trail.isLeftWing()), trailSettings);
                }
            });
        }

        stack.popPose();
    }
    private int getTrailColor(ResolvedTrailSettings trailSettings, boolean isLeftWing)
    {
        return trailSettings.useColorBoth() ? trailSettings.color() : (isLeftWing ? trailSettings.color() : trailSettings.colorRight());
    }
    private Trail.Point copyTrailPointNewPos(Trail.Point point, Vec3 newPos)
    {
        return new Trail.Point(newPos, point.epoch());
    }
    private RenderType getRenderType(Trail trail, ResolvedTrailSettings trailSettings)
    {
        if(trailSettings.glowingTrails())
        {
            if(trailSettings.translucentTrails())
            {
                if(trailSettings.wireframeTrails())
                {
                    return TrailPipelines.entityTranslucentEmissiveWireFrame(trail.texture());
                }
                return TrailPipelines.entityTranslucentEmissiveUnlit(trail.texture());
            }
            else
            {
                if(trailSettings.wireframeTrails())
                {
                    return TrailPipelines.entityCutoutEmissiveUnlitWireframe(trail.texture());
                }
                else
                {
                    return TrailPipelines.entityCutoutEmissiveUnlit(trail.texture());

                }
            }
        }
        else
        {
            if(trailSettings.wireframeTrails())
            {
                return TrailPipelines.entityTranslucentCullWireFrame(trail.texture());
            }
            else
            {
                return TrailPipelines.entityTranslucentCull(trail.texture());
            }
        }
    }
    private void renderSubdividedSegment(
            PoseStack.Pose pose, VertexConsumer consumer,
            Trail.Point point0, Trail.Point point1, Trail.Point point2, Trail.Point point3,
            float tStart, float tEnd,
            Trail trail, int color, ResolvedTrailSettings trailSettings
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


        double chordLenSq = chord.lengthSqr();
        if(chordLenSq > 400)
        {
            if(modConfig.logTrails)
            {
                LOGGER.info("Trail removed for entity {}, trail segment was too long and discarded as invalid.", trail.entityId());
            }
            manager.removeTrail(trail.entityId());
            return;
        }
        boolean needsSplit = false;
        if (chordLenSq > 0.0001) {
            Vec3 toMid = midPos.subtract(startPos);
            double distFromChord = toMid.cross(chord).lengthSqr() / chordLenSq;

            if (distFromChord > 0.0004 && (tEnd - tStart) > 0.05) {
                needsSplit = true;
            }
        }

        if (needsSplit) {
            renderSubdividedSegment(pose, consumer, point0, point1, point2, point3, tStart, midT, trail, color, trailSettings);
            renderSubdividedSegment(pose, consumer, point0, point1, point2, point3, midT, tEnd, trail, color, trailSettings);
        } else {

            float segmentLength = (float) startPos.distanceTo(endPos) * 2f;
            float v1 = this.accumDist / 2.0f;
            float v2 = (this.accumDist + segmentLength) / 2.0f;

            long epoch0 = point1.epoch();
            long epoch1 = point2.epoch();


            double start = Mth.lerp(tStart, epoch0, epoch1);
            double end = Mth.lerp(tEnd, epoch0, epoch1);

            double trailLifetimeMillis = trailSettings.enableTrail() ? trailSettings.trailLifetime()* 1000 : 0;
            float alphaEnd = trailSettings.fadeEnd() ?  computeLifetimeFadeout(end, currentTime, (long) (trailLifetimeMillis)) : 1f;
            float alphaStart = trailSettings.fadeEnd() ? computeLifetimeFadeout(start, currentTime, (long) (trailLifetimeMillis)) : 1f;
            if((currentTime - end) >= trailLifetimeMillis)
            {
                endCorrection = v2;
            }
            float scaleStart = computeWidthScaling(totalTrailLength- v1, v1-endCorrection, trailSettings);
            float scaleEnd = computeWidthScaling(totalTrailLength- v2, v2-endCorrection, trailSettings);
            if(trailSettings.startRampDistance() == 0)
            {
                if(scaleEnd == 0)
                {
                    scaleStart =0;
                }
            }
            if(trailSettings.endRampDistance() == 0)
            {
                if(scaleStart == 0)
                {
                    scaleEnd  =0;
                }
            }
            if(isFirstPerson && modConfig.fadeFirstPersonTrail && modConfig.firstPersonFadeTime > 0)
            {
                scaleStart *= firstPersonWidthFadeFactor(start, currentTime);
                scaleEnd *= firstPersonWidthFadeFactor(end, currentTime);
            }


            if(trailSettings.enableRandomWidth())
            {
                scaleStart = scaleStart * (float)trailSettings.randomWidthVariation() *((float)( perlinNoise.getValue(startPos.x,startPos.y,startPos.z)) + 1);
                scaleEnd = scaleEnd * (float)trailSettings.randomWidthVariation() *((float)( perlinNoise.getValue(endPos.x,endPos.y,endPos.z)) + 1);

            }
            if(trailSettings.increaseWidthOverTime())
            {
                scaleStart *= getWidthOverTimeScale(start, currentTime, (long) (trailLifetimeMillis), trailSettings);
                scaleEnd *= getWidthOverTimeScale(end, currentTime, (long) (trailLifetimeMillis), trailSettings);
            }
            if(trailSettings.fadeStart() && trailSettings.translucentTrails())
            {
                alphaStart *= computeStartFade(totalTrailLength- v1, trailSettings);
                alphaEnd *= computeStartFade(totalTrailLength- v2, trailSettings);
            }
            if(trailSettings.endDistanceFade() && trailSettings.translucentTrails())
            {
                alphaStart *= computeEndFade(v1-endCorrection, trailSettings);
                alphaEnd *= computeEndFade(v2-endCorrection, trailSettings);
            }
            if(trailSettings.speedBasedAlpha())
            {
                alphaStart *= inverseLerpTwoVals(point0.speedAtEmission(),trailSettings.minAlphaSpeed(),trailSettings.maxAlphaSpeed());
                alphaEnd *= inverseLerpTwoVals(point1.speedAtEmission(),trailSettings.minAlphaSpeed(),trailSettings.maxAlphaSpeed());
            }
            if(trailSettings.speedBasedWidth())
            {
                scaleStart *= inverseLerpTwoVals(point0.speedAtEmission(),trailSettings.minWidthSpeed(),trailSettings.maxWidthSpeed());
                scaleEnd *= inverseLerpTwoVals(point1.speedAtEmission(),trailSettings.minWidthSpeed(),trailSettings.maxWidthSpeed());
            }
            if(modConfig.tryNearTrailFade)
            {
                alphaStart *= cameraDistanceFade((float)startPos.distanceTo(cameraPosition));
                alphaEnd *= cameraDistanceFade((float)endPos.distanceTo(cameraPosition));
            }
            float halfWidthStart = (float) (trailSettings.maxWidth() / 2f) * scaleStart;
            float halfWidthEnd = (float) (trailSettings.maxWidth() / 2f) * scaleEnd;

            if ((scaleStart != 0 || scaleEnd != 0) && (alphaEnd != 0 || alphaStart != 0)) {
                if(!trailSettings.translucentTrails())
                {
                    alphaEnd = 1;
                    alphaStart = 1;
                }
                Vec3 startTan = SplineInterpolation.catmullRomTangent(p0, p1, p2, p3, tStart).normalize();
                Vec3 endTan = SplineInterpolation.catmullRomTangent(p0, p1, p2, p3, tEnd).normalize();

                Vec3 sideA = startTan.cross(startPos.subtract(cameraPosition).normalize()).normalize();
                Vec3 sideB = endTan.cross(endPos.subtract(cameraPosition).normalize()).normalize();

                float removeDist = manager.deadPointDistance.getOrDefault(trail.trailId(),0f);
                v1 += removeDist;
                v2 += removeDist;
                v1 /=(float) trailSettings.maxWidth();
                v2 /= (float) trailSettings.maxWidth();
                quadBetweenPoints(pose, consumer, startPos, endPos, sideA, sideB, halfWidthStart, halfWidthEnd, v1, v2, alphaStart, alphaEnd, trail.isLeftWing(), color,trailSettings);
            }
            this.accumDist += segmentLength;
        }
    }

    private float getWidthOverTimeScale(double epoch, long currentTime, long maxLifetime, ResolvedTrailSettings trailSettings) {
        long age = (long) (currentTime - epoch);
        return (float)Mth.lerp((double) age /maxLifetime, trailSettings.startingWidthMultiplier(), trailSettings.endingWidthMultiplier());
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

    private float inverseLerpTwoVals(double t, double a, double b)
    {
        if (a == b) return 1.0f;
        return (float) Math.clamp(((t - a) / (b - a)),0,1);
    }
    private float computeWidthScaling(float distFromStart, float distToEnd, ResolvedTrailSettings config) {
        if(distFromStart <= 0)
        {
            return 0.0f;
        }
        float endRamp = (float) config.endRampDistance();
        float startRamp = (float) config.startRampDistance();
        float up;
        if (startRamp < 1e-6f){
            up = 1f;
        }
        else
        {
            if (distFromStart >= startRamp) up = 1f;
            else up = (float) Math.sin((distFromStart / startRamp) * (Math.PI / 2.0));
        }
        float down;
        if (distToEnd <= 0f) down = 0f;
        else if (distToEnd >= endRamp) down = 1f;
        else down = (float) Math.sin((distToEnd / endRamp) * (Math.PI / 2.0));

        return min(up, down);
    }

    private float computeStartFade(float distFromStart, ResolvedTrailSettings cfg) {
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
        return up;
    }
    private float computeEndFade(float distToEnd, ResolvedTrailSettings cfg) {
        float endRamp = (float) cfg.endDistanceFadeAmount();
        float down;
        if (distToEnd <= 0f) down = 0f;
        else if (distToEnd >= endRamp) down = 1f;
        else down = (float) Math.sin((distToEnd / endRamp) * (Math.PI / 2.0));
        return down;
    }

    @SuppressWarnings("unused")
    private float computeWidthScalingButGood(float distFromStart, float distToEnd, ResolvedTrailSettings config)
    {
        return computeWidthScalingStart(distFromStart, config) * computeWidthScalingEnd(distToEnd, config);
    }
    private float computeWidthScalingStart(float distFromStart, ResolvedTrailSettings config)
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
    private float computeWidthScalingEnd(float distToEnd, ResolvedTrailSettings config)
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

    private int computeLightTexture(Vec3 pos, ResolvedTrailSettings trailSettings) { // note: I really hate this method, but I don't feel like managing the state that's required to do this in a better way
        if (minecraft.level == null || trailSettings.glowingTrails()) return LightTexture.FULL_BRIGHT;

        BlockPos blockPos = BlockPos.containing(pos);
        return LightTexture.pack(minecraft.level.getBrightness(LightLayer.BLOCK, blockPos), minecraft.level.getBrightness(LightLayer.SKY, blockPos));
    }

    private void quadBetweenPoints(
            PoseStack.Pose pose, VertexConsumer consumer,
            Vec3 a, Vec3 b, Vec3 sideA, Vec3 sideB,
            float halfWidthStart, float halfWidthEnd, float v1, float v2, float alphaStart, float alphaEnd, boolean flipUv, int color, ResolvedTrailSettings trailSettings
    ) {
        Vector3f p1 = a.add(sideA.scale(halfWidthStart)).toVector3f();
        Vector3f p2 = b.add(sideB.scale(halfWidthEnd)).toVector3f();
        Vector3f p3 = b.subtract(sideB.scale(halfWidthEnd)).toVector3f();
        Vector3f p4 = a.subtract(sideA.scale(halfWidthStart)).toVector3f();

        int overlay = OverlayTexture.NO_OVERLAY;
        int lightStart = computeLightTexture(a,trailSettings);
        int lightEnd = computeLightTexture(b,trailSettings);

        int colorStart = multiplyAlpha(color, alphaStart);
        int colorEnd = multiplyAlpha(color, alphaEnd);

        float normalX = 0, normalY = -1, normalZ = 0;

        float widthStart = halfWidthStart <= 0 ? 0.5f : 1f;
        float widthEnd = halfWidthEnd <= 0 ? 0.5f : 1f;


        consumer.addVertex(pose, p1)
                .setNormal(normalX, normalY, normalZ)
                .setOverlay(overlay)
                .setLight(lightStart)
                .setColor(colorStart)
                .setUv(v1, flipUv ? 1f - widthStart : -(1f - widthStart));
        consumer.addVertex(pose, p2)
                .setNormal(normalX, normalY, normalZ)
                .setOverlay(overlay)
                .setLight(lightEnd)
                .setColor(colorEnd)
                .setUv(v2, 0f);
        consumer.addVertex(pose, p3)
                .setNormal(normalX, normalY, normalZ)
                .setOverlay(overlay)
                .setLight(lightEnd)
                .setColor(colorEnd)
                .setUv(v2, flipUv ? widthEnd : -widthEnd);
        consumer.addVertex(pose, p4)
                .setNormal(normalX, normalY, normalZ)
                .setOverlay(overlay)
                .setLight(lightStart)
                .setColor(colorStart)
                .setUv(v1, flipUv ? widthStart : -widthStart);
    }

    private void calculateSubdivideLength(
            Trail.Point point0, Trail.Point point1, Trail.Point point2, Trail.Point point3,
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

        double chordLenSq = chord.lengthSqr();
        boolean needsSplit = false;
        if (chordLenSq > 0.0001) {
            Vec3 toMid = midPos.subtract(startPos);
            double distFromChord = toMid.cross(chord).lengthSqr() / chordLenSq;

            if (distFromChord > 0.0004 && (tEnd - tStart) > 0.05) {
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
    private float cameraDistanceFade(float cameraDistBlocks) {
        float denom = (CAMERA_FADE_FULL - CAMERA_FADE_ZERO);
        float t = (cameraDistBlocks - CAMERA_FADE_ZERO) / denom;
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }
    // minecraft's alpha multiply function sets color to 0 when alpha is 0, which breaks with our interpolation. this function does the same but just sets alpha to be min 0
    private int multiplyAlpha(int color, float alpha)
    {
        return alpha >= 1.0F ? color : color(alphaFloat(color) * max(0,alpha), color);
    }
}
