package dbrighthd.elytratrails.trailrendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dbrighthd.elytratrails.config.ModConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

public class TrailRenderer implements SubmitNodeCollector.CustomGeometryRenderer {

    public static final Identifier TRAIL_TEX = Identifier.parse("elytratrails:textures/misc/trail.png");
    private final Int2ObjectMap<Deque<TrailStore.TrailPoint>> trails;

    private static final float SEAM_OVERLAP = 1.02f;
    private static final float THICKNESS_POWER = 0.9f;

    private static final float CAMERA_FADE_ZERO = 0.3f;
    private static final float CAMERA_FADE_FULL = 0.5f;

    public TrailRenderer(Int2ObjectMap<Deque<TrailStore.TrailPoint>> trails) {
        this.trails = trails;
    }

    @Override
    public void render(PoseStack.@NotNull Pose pose, @NotNull VertexConsumer vertexConsumer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        long nowNanos = Util.getNanos();
        TrailStore.cleanup(nowNanos);

        float maxWidthBlocks = (float) AutoConfig.getConfigHolder(ModConfig.class).getConfig().maxWidth;
        Vec3 cameraWorldPos = minecraft.gameRenderer.getMainCamera().position();

        int packedOverlay = OverlayTexture.NO_OVERLAY;
        int packedLight = LightTexture.pack(15, 15);

        VertexBuilder vertexBuilder = new VertexBuilder(vertexConsumer, pose, packedLight, packedOverlay);

        boolean isFirstPerson = minecraft.options.getCameraType().isFirstPerson();
        int localId = (minecraft.player == null) ? Integer.MIN_VALUE : minecraft.player.getId();

        renderTrailStore(vertexBuilder, cameraWorldPos, nowNanos, maxWidthBlocks, isFirstPerson, localId);
    }

    private void renderTrailStore(VertexBuilder vertexBuilder,
                                  Vec3 cameraWorldPos,
                                  long nowNanos,
                                  float maxWidthBlocks,
                                  boolean isFirstPerson,
                                  int localPlayerId) {
        for (Int2ObjectMap.Entry<Deque<TrailStore.TrailPoint>> entityTrailEntry : trails.int2ObjectEntrySet()) {
            int entityId = entityTrailEntry.getIntKey();
            Deque<TrailStore.TrailPoint> trailPoints = entityTrailEntry.getValue();
            if (trailPoints.size() < 2) continue;

            boolean applyFirstPersonNearFade = isFirstPerson && entityId == localPlayerId;

            java.util.ArrayList<TrailStore.TrailPoint> run = new java.util.ArrayList<>();

            for (TrailStore.TrailPoint p : trailPoints) {
                if (p.breakHere()) {
                    renderRun(vertexBuilder, run, cameraWorldPos, nowNanos, maxWidthBlocks, applyFirstPersonNearFade);
                    run.clear();
                    continue;
                }
                run.add(p);
            }

            renderRun(vertexBuilder, run, cameraWorldPos, nowNanos, maxWidthBlocks, applyFirstPersonNearFade);
        }
    }

    private void renderRun(VertexBuilder vertexBuilder,
                           java.util.ArrayList<TrailStore.TrailPoint> run,
                           Vec3 cameraWorldPos,
                           long nowNanos,
                           float maxWidthBlocks,
                           boolean applyFirstPersonNearFade) {
        if (run.size() < 2) return;

        if (getConfig().useSplineTrail) {
            renderRunSplineAuto(vertexBuilder, run, cameraWorldPos, nowNanos, maxWidthBlocks, applyFirstPersonNearFade);
        } else {
            renderRunLinear(vertexBuilder, run, cameraWorldPos, nowNanos, maxWidthBlocks, applyFirstPersonNearFade);
        }
    }

    private void renderRunLinear(VertexBuilder vertexBuilder,
                                 java.util.ArrayList<TrailStore.TrailPoint> run,
                                 Vec3 cameraWorldPos,
                                 long nowNanos,
                                 float maxWidthBlocks,
                                 boolean applyFirstPersonNearFade) {
        int count = run.size();

        double[] dist = new double[count];
        dist[0] = 0.0;
        for (int i = 1; i < count; i++) {
            Vec3 p0 = run.get(i - 1).pos();
            Vec3 p1 = run.get(i).pos();
            dist[i] = dist[i - 1] + p1.distanceTo(p0);
        }
        double totalLen = dist[count - 1];
        if (totalLen < 1e-6) return;

        Vec3 stitchedScaledSideAtStart = null;

        for (int i = 1; i < count; i++) {
            TrailStore.TrailPoint a = run.get(i - 1);
            TrailStore.TrailPoint b = run.get(i);

            float dFromStartA = (float) dist[i - 1];
            float dFromStartB = (float) dist[i];

            float dToEndA = (float) (totalLen - dist[i - 1]);
            float dToEndB = (float) (totalLen - dist[i]);

            stitchedScaledSideAtStart = drawSegment(
                    vertexBuilder,
                    a, b,
                    cameraWorldPos,
                    nowNanos,
                    stitchedScaledSideAtStart,
                    maxWidthBlocks,
                    dFromStartA, dToEndA,
                    dFromStartB, dToEndB,
                    applyFirstPersonNearFade
            );
        }
    }

    private void renderRunSplineAuto(VertexBuilder vertexBuilder,
                                     java.util.ArrayList<TrailStore.TrailPoint> run,
                                     Vec3 cameraWorldPos,
                                     long nowNanos,
                                     float maxWidthBlocks,
                                     boolean applyFirstPersonNearFade) {

        int n = run.size();

        java.util.ArrayList<Vec3> pts = new java.util.ArrayList<>();
        java.util.ArrayList<Long> times = new java.util.ArrayList<>();

        pts.add(run.getFirst().pos());
        times.add(run.getFirst().timeNanos());

        for (int i = 0; i < n - 1; i++) {
            Vec3 p0 = run.get(Math.max(i - 1, 0)).pos();
            Vec3 p1 = run.get(i).pos();
            Vec3 p2 = run.get(i + 1).pos();
            Vec3 p3 = run.get(Math.min(i + 2, n - 1)).pos();

            long t1 = run.get(i).timeNanos();
            long t2 = run.get(i + 1).timeNanos();

            int steps = stepsFor(p1, p2);

            for (int s = 1; s <= steps; s++) {
                double tt = s / (double) steps;
                pts.add(catmullRom(p0, p1, p2, p3, tt));

                long interpTime = (long) (t1 + (t2 - t1) * tt);
                times.add(interpTime);
            }
        }

        if (pts.size() < 2) return;

        int count = pts.size();

        double[] dist = new double[count];
        dist[0] = 0.0;
        for (int i = 1; i < count; i++) {
            dist[i] = dist[i - 1] + pts.get(i).distanceTo(pts.get(i - 1));
        }
        double totalLen = dist[count - 1];
        if (totalLen < 1e-6) return;

        Vec3 stitchedScaledSideAtStart = null;

        for (int i = 1; i < count; i++) {
            TrailStore.TrailPoint a = new TrailStore.TrailPoint(pts.get(i - 1), times.get(i - 1), false);
            TrailStore.TrailPoint b = new TrailStore.TrailPoint(pts.get(i),     times.get(i),     false);

            float dFromStartA = (float) dist[i - 1];
            float dFromStartB = (float) dist[i];

            float dToEndA = (float) (totalLen - dist[i - 1]);
            float dToEndB = (float) (totalLen - dist[i]);

            stitchedScaledSideAtStart = drawSegment(
                    vertexBuilder,
                    a, b,
                    cameraWorldPos,
                    nowNanos,
                    stitchedScaledSideAtStart,
                    maxWidthBlocks,
                    dFromStartA, dToEndA,
                    dFromStartB, dToEndB,
                    applyFirstPersonNearFade
            );
        }
    }

    private static int stepsFor(Vec3 p1, Vec3 p2) {
        double d = p2.distanceTo(p1);
        return Mth.clamp((int) Math.ceil(d * 4.0), 1, 16);
    }

    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;

        double x = 0.5 * (2 * p1.x + (-p0.x + p2.x) * t
                + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2
                + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);

        double y = 0.5 * (2 * p1.y + (-p0.y + p2.y) * t
                + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2
                + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);

        double z = 0.5 * (2 * p1.z + (-p0.z + p2.z) * t
                + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2
                + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);

        return new Vec3(x, y, z);
    }

    // NEW: camera-distance fade factor (0..1)
    private static float cameraDistanceFade(float cameraDistBlocks) {
        float denom = (CAMERA_FADE_FULL - CAMERA_FADE_ZERO);
        if (denom <= 1e-6f) return cameraDistBlocks >= CAMERA_FADE_FULL ? 1f : 0f;

        float t = (cameraDistBlocks - CAMERA_FADE_ZERO) / denom;
        t = Mth.clamp(t, 0f, 1f);

        // smoothstep for nicer transition
        return t * t * (3f - 2f * t);
    }

    private Vec3 drawSegment(
            VertexBuilder vertexBuilder,
            TrailStore.TrailPoint startTrailPoint,
            TrailStore.TrailPoint endTrailPoint,
            Vec3 cameraWorldPos,
            long nowNanos,
            Vec3 stitchedScaledSideAtStart,
            float maxWidthBlocks,
            float distFromStartA, float distToEndA,
            float distFromStartB, float distToEndB,
            boolean applyFirstPersonNearFade
    ) {
        Vec3 startPosCamSpace = startTrailPoint.pos().subtract(cameraWorldPos);
        Vec3 endPosCamSpace   = endTrailPoint.pos().subtract(cameraWorldPos);

        Vec3 segmentDelta = endPosCamSpace.subtract(startPosCamSpace);
        double segmentLength = segmentDelta.length();
        if (segmentLength < 1e-6) return stitchedScaledSideAtStart;

        Vec3 segmentDirection = segmentDelta.scale(1.0 / segmentLength);

        float randStart = 1f;
        float randEnd = 1f;
        if (getConfig().enableRandomWidth) {
            randStart = randFromPoint(startTrailPoint);
            randEnd = randFromPoint(endTrailPoint);
        }

        float thicknessStart = thicknessAtDistance(distFromStartA, distToEndA);
        float thicknessEnd   = thicknessAtDistance(distFromStartB, distToEndB);

        float widthAtStart = maxWidthBlocks * randStart * thicknessStart * SEAM_OVERLAP;
        float widthAtEnd   = maxWidthBlocks * randEnd   * thicknessEnd   * SEAM_OVERLAP;

        Vec3 segmentMidpointCamSpace = startPosCamSpace.add(endPosCamSpace).scale(0.5);
        Vec3 directionTowardCamera = segmentMidpointCamSpace.scale(-1.0);

        double toCameraLength = directionTowardCamera.length();
        if (toCameraLength > 1e-6) directionTowardCamera = directionTowardCamera.scale(1.0 / toCameraLength);
        else directionTowardCamera = new Vec3(0, 0, 1);

        Vec3 sideDirection = segmentDirection.cross(directionTowardCamera);
        double sideDirectionLength = sideDirection.length();

        if (sideDirectionLength < 1e-6) {
            Vec3 worldUp = new Vec3(0, 1, 0);
            sideDirection = segmentDirection.cross(worldUp);
            sideDirectionLength = sideDirection.length();
            if (sideDirectionLength < 1e-6) return stitchedScaledSideAtStart;
        }

        sideDirection = sideDirection.scale(1.0 / sideDirectionLength);

        if (stitchedScaledSideAtStart != null) {
            double previousSideLength = stitchedScaledSideAtStart.length();
            if (previousSideLength > 1e-6) {
                Vec3 previousSideDirection = stitchedScaledSideAtStart.scale(1.0 / previousSideLength);
                if (sideDirection.dot(previousSideDirection) < 0) sideDirection = sideDirection.scale(-1);
            }
        }

        Vec3 scaledSideAtStart = (stitchedScaledSideAtStart != null) ? stitchedScaledSideAtStart : sideDirection.scale(widthAtStart);
        Vec3 scaledSideAtEnd = sideDirection.scale(widthAtEnd);

        Vec3 startOuterCorner = startPosCamSpace.add(scaledSideAtStart);
        Vec3 startInnerCorner = startPosCamSpace.subtract(scaledSideAtStart);
        Vec3 endOuterCorner   = endPosCamSpace.add(scaledSideAtEnd);
        Vec3 endInnerCorner   = endPosCamSpace.subtract(scaledSideAtEnd);

        float ageAtStart = (float) ((nowNanos - startTrailPoint.timeNanos()) / (double) TrailStore.getLifetime());
        float ageAtEnd   = (float) ((nowNanos - endTrailPoint.timeNanos())   / (double) TrailStore.getLifetime());

        float fadeMulStart = alphaMultiplier(startTrailPoint, nowNanos, applyFirstPersonNearFade, thicknessStart);
        float fadeMulEnd   = alphaMultiplier(endTrailPoint,   nowNanos, applyFirstPersonNearFade, thicknessEnd);

        if(getConfig().cameraDistanceFade)
        {
            fadeMulStart *= cameraDistanceFade((float) startPosCamSpace.length());
            fadeMulEnd   *= cameraDistanceFade((float) endPosCamSpace.length());
        }


        int alphaAtStart = (int) (clamp255((int) (255 * (1.0f - ageAtStart))) * fadeMulStart);
        int alphaAtEnd   = (int) (clamp255((int) (255 * (1.0f - ageAtEnd)))   * fadeMulEnd);

        float normalX = 0.0f, normalY = 1.0f, normalZ = 0.0f;

        vertexBuilder.quadQuad(
                startOuterCorner,
                startInnerCorner,
                endInnerCorner,
                endOuterCorner,
                alphaAtStart,
                alphaAtEnd,
                normalX, normalY, normalZ
        );

        return scaledSideAtEnd;
    }

    private static float alphaMultiplier(TrailStore.TrailPoint trailPoint,
                                         long nowNanos,
                                         boolean applyFirstPersonNearFade,
                                         float thicknessMult) {
        if (!applyFirstPersonNearFade) return 1f;
        if (thicknessMult >= 0.999f) return 1f;
        if (trailPoint == null || trailPoint.pos() == null) return 0f;

        double hideSeconds = getConfig().fadeTime;
        double fadeSeconds = 0.1;

        if (hideSeconds < 0.0) hideSeconds = 0.0;

        double ageSeconds = (nowNanos - trailPoint.timeNanos()) / 1_000_000_000.0;

        if (ageSeconds < hideSeconds) return 0f;

        float t = (float) ((ageSeconds - hideSeconds) / fadeSeconds);
        return Mth.clamp(t, 0f, 1f);
    }

    private static float thicknessAtDistance(float distFromStart, float distToEnd) {
        var cfg = getConfig();
        float startRamp = (float) cfg.startRampDistance;
        float endRamp   = (float) cfg.endRampDistance;

        if (startRamp < 1e-6f) startRamp = 1e-6f;
        if (endRamp   < 1e-6f) endRamp   = 1e-6f;

        float up;
        if (distFromStart <= 0f) up = 0f;
        else if (distFromStart >= startRamp) up = 1f;
        else up = (float) Math.sin((distFromStart / startRamp) * (Math.PI / 2.0));

        float down;
        if (distToEnd <= 0f) down = 0f;
        else if (distToEnd >= endRamp) down = 1f;
        else down = (float) Math.sin((distToEnd / endRamp) * (Math.PI / 2.0));

        float mult = Math.min(up, down);

        mult = (float) Math.pow(mult, THICKNESS_POWER);
        return mult;
    }

    private static float randFromPoint(TrailStore.TrailPoint trailPoint) {
        float randpoint = rand01FromPos(
                (int) (trailPoint.pos().x * 10),
                (int) (trailPoint.pos().y * 10),
                (int) (trailPoint.pos().z * 10)
        );
        return 1f + randpoint * (float) getConfig().randomWidthVariation;
    }

    private static float rand01FromPos(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= (h >>> 16);
        return (h & 0x7fffffff) / (float) 0x80000000;
    }

    private static int clamp255(int a) {
        if (a < 0) return 0;
        return Math.min(a, 255);
    }
}
