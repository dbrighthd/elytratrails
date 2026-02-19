package dbrighthd.elytratrails.trailrendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager.ResolvedTrailSettings;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Deque;
import java.util.Iterator;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.config.pack.TrailPackConfigManager.resolveFromPlayerConfig;

public class TrailRenderer implements SubmitNodeCollector.CustomGeometryRenderer {

    public static final Identifier TRAIL_TEX = Identifier.parse("elytratrails:textures/misc/trail.png");
    private final Long2ObjectMap<Deque<TrailStore.TrailPoint>> trails;


    private static final float SEAM_OVERLAP = 1.02f;
    private static final float THICKNESS_POWER = 0.9f;

    private static final float CAMERA_FADE_ZERO = 0.3f;
    private static final float CAMERA_FADE_FULL = 0.5f;


    private static final ThreadLocal<java.util.ArrayList<TrailStore.TrailPoint>> RUN_BUF =
            ThreadLocal.withInitial(() -> new java.util.ArrayList<>(256));
    private static final ThreadLocal<java.util.ArrayList<Vec3>> SPLINE_PTS_BUF =
            ThreadLocal.withInitial(() -> new java.util.ArrayList<>(256));
    private static final ThreadLocal<it.unimi.dsi.fastutil.longs.LongArrayList> SPLINE_TIMES_BUF =
            ThreadLocal.withInitial(() -> new it.unimi.dsi.fastutil.longs.LongArrayList(256));
    private static final ThreadLocal<double[]> DIST_BUF =
            ThreadLocal.withInitial(() -> new double[256]);

    /**
     * Cache last resolved settings per (entityId + trailIndex) packedKey.
     * This prevents PLAYER trails from reverting to defaults after the player entity unloads.
     */
    private static final Long2ObjectOpenHashMap<ResolvedTrailSettings> SETTINGS_CACHE = new Long2ObjectOpenHashMap<>();

    public TrailRenderer(Long2ObjectMap<Deque<TrailStore.TrailPoint>> trails) {
        this.trails = trails;
    }

    @Override
    public void render(PoseStack.@NotNull Pose pose, @NotNull VertexConsumer vertexConsumer) {
        if (!getConfig().enableAllTrails) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        long nowNanos = Util.getNanos();

        ModConfig cfg = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        boolean fadeOverLifetime = cfg.translucentTrails;

        var camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraWorldPos = camera.position();

        // Stable camera axes in world space.
        Quaternionf camRot = new Quaternionf(camera.rotation());
        Vector3f fwd = new Vector3f(0, 0, 1);
        Vector3f up = new Vector3f(0, 1, 0);
        camRot.transform(fwd);
        camRot.transform(up);
        Vec3 cameraForwardWorld = new Vec3(fwd.x, fwd.y, fwd.z);
        Vec3 cameraUpWorld = new Vec3(up.x, up.y, up.z);

        int packedOverlay = OverlayTexture.NO_OVERLAY;
        int packedLight = LightTexture.pack(15, 15);

        VertexBuilder vertexBuilder = new VertexBuilder(vertexConsumer, pose, packedLight, packedOverlay);

        boolean isFirstPerson = minecraft.options.getCameraType().isFirstPerson();
        int localId = (minecraft.player == null) ? Integer.MIN_VALUE : minecraft.player.getId();

        renderTrailStore(
                vertexBuilder,
                cameraWorldPos,
                cameraForwardWorld,
                cameraUpWorld,
                nowNanos,
                cfg,
                fadeOverLifetime,
                isFirstPerson,
                localId
        );
    }

    private void renderTrailStore(VertexBuilder vertexBuilder,
                                  Vec3 cameraWorldPos,
                                  Vec3 cameraForwardWorld,
                                  Vec3 cameraUpWorld,
                                  long nowNanos,
                                  ModConfig baseCfg,
                                  boolean fadeOverLifetime,
                                  boolean isFirstPerson,
                                  int localPlayerId) {
        Iterator<Long2ObjectMap.Entry<Deque<TrailStore.TrailPoint>>> it = trails.long2ObjectEntrySet().iterator();

        java.util.ArrayList<TrailStore.TrailPoint> run = RUN_BUF.get();

        while (it.hasNext()) {
            Long2ObjectMap.Entry<Deque<TrailStore.TrailPoint>> entityTrailEntry = it.next();
            long packedKey = entityTrailEntry.getLongKey();
            int entityId = TrailStore.entityId(packedKey);
            int trailIndex = TrailStore.trailIndex(packedKey);

            Deque<TrailStore.TrailPoint> trailPoints = entityTrailEntry.getValue();
            if (trailPoints.size() < 2) continue;

            boolean applyFirstPersonNearFade = isFirstPerson && entityId == localPlayerId;

            TrailContextStore.BoneCtx ctx = TrailContextStore.get(entityId, trailIndex);
            String modelName = ctx == null ? null : ctx.modelName();
            String boneName = ctx == null ? null : ctx.boneName();


            boolean isLeftWing = TrailStore.isLeftWing(packedKey);

            ResolvedTrailSettings settings = getResolvedSettingsCached(packedKey, entityId, modelName, boneName);
            if (settings == null) continue;

            float maxWidthBlocks = (float) settings.maxWidth();
            int argb = parseHexColor(settings.color() == null ? baseCfg.color : settings.color(), 0xFFFFFFFF);
            int colorR = (argb >>> 16) & 0xFF;
            int colorG = (argb >>> 8) & 0xFF;
            int colorB = (argb) & 0xFF;
            int colorA = (argb >>> 24) & 0xFF;


            if (maxWidthBlocks <= 0f || colorA <= 0) {
                SETTINGS_CACHE.remove(packedKey);
                TrailContextStore.removeKey(packedKey);
                TrailStore.removeMeta(packedKey);
                it.remove();
                continue;
            }

            long lifetimeNanos = (long) (settings.trailLifetime() * 1_000_000_000L);


            if (lifetimeNanos > 0) {
                while (!trailPoints.isEmpty() && nowNanos - trailPoints.peekFirst().timeNanos() > lifetimeNanos) {
                    trailPoints.removeFirst();
                }
                if (trailPoints.size() < 2) {
                    if (trailPoints.isEmpty()) {
                        SETTINGS_CACHE.remove(packedKey);
                        TrailContextStore.removeKey(packedKey);
                        TrailStore.removeMeta(packedKey);
                        it.remove();
                    }
                    continue;
                }
            }

            run.clear();

            for (TrailStore.TrailPoint p : trailPoints) {
                if (p.breakHere()) {
                    renderRun(vertexBuilder, run, cameraWorldPos, cameraForwardWorld, cameraUpWorld,
                            nowNanos, settings, lifetimeNanos,
                            maxWidthBlocks, fadeOverLifetime, applyFirstPersonNearFade,
                            colorR, colorG, colorB, colorA, isLeftWing);
                    run.clear();
                    continue;
                }

                if (lifetimeNanos > 0 && nowNanos - p.timeNanos() > lifetimeNanos) continue;
                run.add(p);
            }

            renderRun(vertexBuilder, run, cameraWorldPos, cameraForwardWorld, cameraUpWorld,
                    nowNanos, settings, lifetimeNanos,
                    maxWidthBlocks, fadeOverLifetime, applyFirstPersonNearFade,
                    colorR, colorG, colorB, colorA, isLeftWing);
        }
    }

    private void renderRun(VertexBuilder vertexBuilder,
                           java.util.ArrayList<TrailStore.TrailPoint> run,
                           Vec3 cameraWorldPos,
                           Vec3 cameraForwardWorld,
                           Vec3 cameraUpWorld,
                           long nowNanos,
                           ResolvedTrailSettings settings,
                           long lifetimeNanos,
                           float maxWidthBlocks,
                           boolean fadeOverLifetime,
                           boolean applyFirstPersonNearFade,
                           int colorR,
                           int colorG,
                           int colorB,
                           int colorA,
                           boolean isLeftWing) {
        if (run.size() < 2) return;

        if (settings.useSplineTrail()) {
            renderRunSplineAuto(vertexBuilder, run, cameraWorldPos, cameraForwardWorld, cameraUpWorld,
                    nowNanos, settings, lifetimeNanos,
                    maxWidthBlocks, fadeOverLifetime, applyFirstPersonNearFade,
                    colorR, colorG, colorB, colorA, isLeftWing);
        } else {
            renderRunLinear(vertexBuilder, run, cameraWorldPos, cameraForwardWorld, cameraUpWorld,
                    nowNanos, settings, lifetimeNanos,
                    maxWidthBlocks, fadeOverLifetime, applyFirstPersonNearFade,
                    colorR, colorG, colorB, colorA, isLeftWing);
        }
    }

    private static ResolvedTrailSettings getResolvedSettingsCached(long packedKey, int entityId, String modelName, String boneName) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;

        Entity e = level.getEntity(entityId);

        if (e == null) {
            ResolvedTrailSettings cached = SETTINGS_CACHE.get(packedKey);
            if (cached != null) return cached;

            // No cache: fall back to normal "others" resolution (players may look like defaults).
            return TrailPackConfigManager.resolveOthers(modelName, boneName, getConfig());
        }

        if (e instanceof Player) {
            ResolvedTrailSettings base = resolveFromPlayerConfig(ClientPlayerConfigStore.getOrDefault(entityId));
            ResolvedTrailSettings resolved = getConfig().resourcePackOverride
                    ? TrailPackConfigManager.resolveOnTop(modelName, boneName, base)
                    : base;

            if (resolved != null) SETTINGS_CACHE.put(packedKey, resolved);
            return resolved;
        }

        // Non-player: ensure stale player cache doesn't apply if ids get reused.
        SETTINGS_CACHE.remove(packedKey);
        return TrailPackConfigManager.resolveOthers(modelName, boneName, getConfig());
    }

    private void renderRunLinear(VertexBuilder vertexBuilder,
                                 java.util.ArrayList<TrailStore.TrailPoint> run,
                                 Vec3 cameraWorldPos,
                                 Vec3 cameraForwardWorld,
                                 Vec3 cameraUpWorld,
                                 long nowNanos,
                                 ResolvedTrailSettings settings,
                                 long lifetimeNanos,
                                 float maxWidthBlocks,
                                 boolean fadeOverLifetime,
                                 boolean applyFirstPersonNearFade,
                                 int colorR,
                                 int colorG,
                                 int colorB,
                                 int colorA,
                                 boolean isLeftWing) {
        int count = run.size();

        double[] dist = ensureDistCapacity(count);
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
                    cameraForwardWorld,
                    cameraUpWorld,
                    nowNanos,
                    stitchedScaledSideAtStart,
                    maxWidthBlocks,
                    settings,
                    lifetimeNanos,
                    dFromStartA, dToEndA,
                    dFromStartB, dToEndB,
                    fadeOverLifetime,
                    applyFirstPersonNearFade,
                    colorR, colorG, colorB, colorA,
                    isLeftWing
            );
        }
    }

    private void renderRunSplineAuto(VertexBuilder vertexBuilder,
                                     java.util.ArrayList<TrailStore.TrailPoint> run,
                                     Vec3 cameraWorldPos,
                                     Vec3 cameraForwardWorld,
                                     Vec3 cameraUpWorld,
                                     long nowNanos,
                                     ResolvedTrailSettings settings,
                                     long lifetimeNanos,
                                     float maxWidthBlocks,
                                     boolean fadeOverLifetime,
                                     boolean applyFirstPersonNearFade,
                                     int colorR,
                                     int colorG,
                                     int colorB,
                                     int colorA,
                                     boolean isLeftWing) {

        int n = run.size();

        java.util.ArrayList<Vec3> pts = SPLINE_PTS_BUF.get();
        it.unimi.dsi.fastutil.longs.LongArrayList times = SPLINE_TIMES_BUF.get();
        pts.clear();
        times.clear();

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

        double[] dist = ensureDistCapacity(count);
        dist[0] = 0.0;
        for (int i = 1; i < count; i++) {
            dist[i] = dist[i - 1] + pts.get(i).distanceTo(pts.get(i - 1));
        }
        double totalLen = dist[count - 1];
        if (totalLen < 1e-6) return;

        Vec3 stitchedScaledSideAtStart = null;

        for (int i = 1; i < count; i++) {
            Vec3 aPos = pts.get(i - 1);
            Vec3 bPos = pts.get(i);
            long aTime = times.getLong(i - 1);
            long bTime = times.getLong(i);

            float dFromStartA = (float) dist[i - 1];
            float dFromStartB = (float) dist[i];

            float dToEndA = (float) (totalLen - dist[i - 1]);
            float dToEndB = (float) (totalLen - dist[i]);

            stitchedScaledSideAtStart = drawSegmentRaw(
                    vertexBuilder,
                    aPos, aTime,
                    bPos, bTime,
                    cameraWorldPos,
                    cameraForwardWorld,
                    cameraUpWorld,
                    nowNanos,
                    stitchedScaledSideAtStart,
                    maxWidthBlocks,
                    settings,
                    lifetimeNanos,
                    dFromStartA, dToEndA,
                    dFromStartB, dToEndB,
                    fadeOverLifetime,
                    applyFirstPersonNearFade,
                    colorR, colorG, colorB, colorA,
                    isLeftWing
            );
        }
    }

    private static double[] ensureDistCapacity(int needed) {
        double[] arr = DIST_BUF.get();
        if (arr.length >= needed) return arr;
        int n = arr.length;
        while (n < needed) n *= 2;
        double[] grown = new double[n];
        DIST_BUF.set(grown);
        return grown;
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

    private static float cameraDistanceFade(float cameraDistBlocks) {
        float denom = (CAMERA_FADE_FULL - CAMERA_FADE_ZERO);
        float t = (cameraDistBlocks - CAMERA_FADE_ZERO) / denom;
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private Vec3 drawSegment(
            VertexBuilder vertexBuilder,
            TrailStore.TrailPoint startTrailPoint,
            TrailStore.TrailPoint endTrailPoint,
            Vec3 cameraWorldPos,
            Vec3 cameraForwardWorld,
            Vec3 cameraUpWorld,
            long nowNanos,
            Vec3 stitchedScaledSideAtStart,
            float maxWidthBlocks,
            ResolvedTrailSettings settings,
            long lifetimeNanos,
            float distFromStartA, float distToEndA,
            float distFromStartB, float distToEndB,
            boolean fadeOverLifetime,
            boolean applyFirstPersonNearFade,
            int colorR,
            int colorG,
            int colorB,
            int colorA,
            boolean isLeftWing
    ) {
        return drawSegmentRaw(
                vertexBuilder,
                startTrailPoint.pos(), startTrailPoint.timeNanos(),
                endTrailPoint.pos(), endTrailPoint.timeNanos(),
                cameraWorldPos,
                cameraForwardWorld,
                cameraUpWorld,
                nowNanos,
                stitchedScaledSideAtStart,
                maxWidthBlocks,
                settings,
                lifetimeNanos,
                distFromStartA, distToEndA,
                distFromStartB, distToEndB,
                fadeOverLifetime,
                applyFirstPersonNearFade,
                colorR, colorG, colorB, colorA,
                isLeftWing
        );
    }

    private Vec3 drawSegmentRaw(
            VertexBuilder vertexBuilder,
            Vec3 startWorldPos,
            long startTimeNanos,
            Vec3 endWorldPos,
            long endTimeNanos,
            Vec3 cameraWorldPos,
            Vec3 cameraForwardWorld,
            Vec3 cameraUpWorld,
            long nowNanos,
            Vec3 stitchedScaledSideAtStart,
            float maxWidthBlocks,
            ResolvedTrailSettings settings,
            long lifetimeNanos,
            float distFromStartA, float distToEndA,
            float distFromStartB, float distToEndB,
            boolean fadeOverLifetime,
            boolean applyFirstPersonNearFade,
            int colorR,
            int colorG,
            int colorB,
            int colorA,
            boolean isLeftWing
    ) {
        Vec3 startPosCamSpace = startWorldPos.subtract(cameraWorldPos);
        Vec3 endPosCamSpace = endWorldPos.subtract(cameraWorldPos);

        Vec3 segmentDelta = endPosCamSpace.subtract(startPosCamSpace);
        double segmentLength = segmentDelta.length();
        if (segmentLength < 1e-6) return stitchedScaledSideAtStart;

        Vec3 segmentDirection = segmentDelta.scale(1.0 / segmentLength);

        float randStart = 1f;
        float randEnd = 1f;
        if (settings.enableRandomWidth()) {
            float v = (float) settings.randomWidthVariation();
            randStart = 1f + randFromPos(startWorldPos) * v;
            randEnd = 1f + randFromPos(endWorldPos) * v;
        }

        float thicknessStart = thicknessAtDistance(distFromStartA, distToEndA, settings);
        float thicknessEnd = thicknessAtDistance(distFromStartB, distToEndB, settings);

        float widthAtStart = maxWidthBlocks * randStart * thicknessStart * SEAM_OVERLAP;
        float widthAtEnd = maxWidthBlocks * randEnd * thicknessEnd * SEAM_OVERLAP;

        float fpMulStart = alphaMultiplier(startWorldPos, startTimeNanos, nowNanos, applyFirstPersonNearFade, thicknessStart);
        float fpMulEnd = alphaMultiplier(endWorldPos, endTimeNanos, nowNanos, applyFirstPersonNearFade, thicknessEnd);

        float alphaMulStart = fpMulStart;
        float alphaMulEnd = fpMulEnd;

        if (!fadeOverLifetime && applyFirstPersonNearFade) {
            widthAtStart *= fpMulStart;
            widthAtEnd *= fpMulEnd;

            alphaMulStart = 1f;
            alphaMulEnd = 1f;
        }

        // --- Billboarding + stable UV orientation (no rolling/flicker) ---

        Vec3 midWorldPos = startWorldPos.add(endWorldPos).scale(0.5);
        Vec3 toCameraWorld = cameraWorldPos.subtract(midWorldPos);
        double toCameraLen = toCameraWorld.length();
        if (toCameraLen > 1e-6) toCameraWorld = toCameraWorld.scale(1.0 / toCameraLen);

        // Project camera vector onto plane perpendicular to segment => billboard normal
        double camDot = toCameraWorld.dot(segmentDirection);
        Vec3 normalDir = toCameraWorld.subtract(segmentDirection.scale(camDot));
        double normalLen = normalDir.length();

        if (normalLen < 1e-6) {
            double fDot = cameraForwardWorld.dot(segmentDirection);
            normalDir = cameraForwardWorld.subtract(segmentDirection.scale(fDot));
            normalLen = normalDir.length();

            if (normalLen < 1e-6) {
                double uDot = cameraUpWorld.dot(segmentDirection);
                normalDir = cameraUpWorld.subtract(segmentDirection.scale(uDot));
                normalLen = normalDir.length();

                if (normalLen < 1e-6) return stitchedScaledSideAtStart;
            }
        }

        normalDir = normalDir.scale(1.0 / normalLen);

        Vec3 sideDirection = segmentDirection.cross(normalDir);
        double sideLen = sideDirection.length();
        if (sideLen < 1e-6) return stitchedScaledSideAtStart;
        sideDirection = sideDirection.scale(1.0 / sideLen);

        boolean flipU = false;

        // Stable preferred sign using camera up (prevents UV "spins")
        Vec3 preferredSide = segmentDirection.cross(cameraUpWorld);
        double prefLen = preferredSide.length();
        if (prefLen < 1e-6) {
            preferredSide = segmentDirection.cross(cameraForwardWorld);
            prefLen = preferredSide.length();
        }
        if (prefLen > 1e-6) {
            preferredSide = preferredSide.scale(1.0 / prefLen);
            if (sideDirection.dot(preferredSide) < 0.0) {
                sideDirection = sideDirection.scale(-1.0);
                normalDir = normalDir.scale(-1.0);
                flipU = !flipU;
            }
        }

        // Stitch continuity (and flip U together)
        if (stitchedScaledSideAtStart != null) {
            double previousSideLength = stitchedScaledSideAtStart.length();
            if (previousSideLength > 1e-6) {
                Vec3 prevSideDir = stitchedScaledSideAtStart.scale(1.0 / previousSideLength);
                if (sideDirection.dot(prevSideDir) < 0.0) {
                    sideDirection = sideDirection.scale(-1.0);
                    normalDir = normalDir.scale(-1.0);
                    flipU = !flipU;
                }
            }
        }

        // Ensure normal faces camera; if not, flip both and flip U
        if (normalDir.dot(toCameraWorld) < 0.0) {
            sideDirection = sideDirection.scale(-1.0);
            normalDir = normalDir.scale(-1.0);
            flipU = !flipU;
        }

        // --- end billboard ---

        Vec3 scaledSideAtStart = (stitchedScaledSideAtStart != null)
                ? stitchedScaledSideAtStart
                : sideDirection.scale(widthAtStart);
        Vec3 scaledSideAtEnd = sideDirection.scale(widthAtEnd);

        Vec3 startOuterCorner = startPosCamSpace.add(scaledSideAtStart);
        Vec3 startInnerCorner = startPosCamSpace.subtract(scaledSideAtStart);
        Vec3 endOuterCorner = endPosCamSpace.add(scaledSideAtEnd);
        Vec3 endInnerCorner = endPosCamSpace.subtract(scaledSideAtEnd);

        double denom = (lifetimeNanos <= 0) ? 1.0 : (double) lifetimeNanos;
        float ageAtStart = (float) ((nowNanos - startTimeNanos) / denom);
        float ageAtEnd = (float) ((nowNanos - endTimeNanos) / denom);

        if (settings.cameraDistanceFade()) {
            alphaMulStart *= cameraDistanceFade((float) startPosCamSpace.length());
            alphaMulEnd *= cameraDistanceFade((float) endPosCamSpace.length());
        }

        float lifeMulStart = fadeOverLifetime ? (1.0f - ageAtStart) : 1.0f;
        float lifeMulEnd = fadeOverLifetime ? (1.0f - ageAtEnd) : 1.0f;

        int alphaAtStart = (int) (clamp255((int) (255 * lifeMulStart)) * alphaMulStart);
        int alphaAtEnd = (int) (clamp255((int) (255 * lifeMulEnd)) * alphaMulEnd);

        if (colorA != 255) {
            alphaAtStart = (alphaAtStart * colorA) / 255;
            alphaAtEnd = (alphaAtEnd * colorA) / 255;
        }

        float normalX = (float) normalDir.x;
        float normalY = (float) normalDir.y;
        float normalZ = (float) normalDir.z;

        float u0 = flipU ? 1f : 0f;
        float u1 = flipU ? 0f : 1f;

        // Left wing: flip V. (If you instead want lengthwise mirror, flip U.)
        float v0 = isLeftWing ? 1f : 0f;
        float v1 = isLeftWing ? 0f : 1f;

        vertexBuilder.quadQuadUV(
                startOuterCorner,
                startInnerCorner,
                endInnerCorner,
                endOuterCorner,
                u0, u1,
                v0, v1,
                colorR, colorG, colorB,
                alphaAtStart,
                alphaAtEnd,
                0, -0.4f, -0.1f
        );

        return scaledSideAtEnd;
    }

    private static float alphaMultiplier(Vec3 worldPos,
                                         long pointTimeNanos,
                                         long nowNanos,
                                         boolean applyFirstPersonNearFade,
                                         float thicknessMult) {
        if (!applyFirstPersonNearFade) return 1f;
        if (thicknessMult >= 0.999f) return 1f;
        if (worldPos == null) return 0f;

        double hideSeconds = getConfig().firstPersonFadeTime;
        double fadeSeconds = 0.1;
        if (hideSeconds < 0.0) hideSeconds = 0.0;

        double ageSeconds = (nowNanos - pointTimeNanos) / 1_000_000_000.0;
        if (ageSeconds < hideSeconds) return 0f;

        float t = (float) ((ageSeconds - hideSeconds) / fadeSeconds);
        return Mth.clamp(t, 0f, 1f);
    }

    private static float thicknessAtDistance(float distFromStart, float distToEnd, ResolvedTrailSettings settings) {
        float endRamp = (float) settings.startRampDistance();
        float startRamp = (float) settings.endRampDistance();

        if (startRamp < 1e-6f) startRamp = 1e-6f;
        if (endRamp < 1e-6f) endRamp = 1e-6f;

        float up;
        if (distFromStart <= 0f) up = 0f;
        else if (distFromStart >= startRamp) up = 1f;
        else up = (float) Math.sin((distFromStart / startRamp) * (Math.PI / 2.0));

        float down;
        if (distToEnd <= 0f) down = 0f;
        else if (distToEnd >= endRamp) down = 1f;
        else down = (float) Math.sin((distToEnd / endRamp) * (Math.PI / 2.0));

        float mult = Math.min(up, down);
        return (float) Math.pow(mult, THICKNESS_POWER);
    }

    private static float randFromPos(Vec3 pos) {
        return rand01FromPos(
                (int) (pos.x * 10),
                (int) (pos.y * 10),
                (int) (pos.z * 10)
        );
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

    private static int parseHexColor(String s, int fallbackArgb) {
        if (s == null) return fallbackArgb;
        String t = s.trim();
        if (t.isEmpty()) return fallbackArgb;
        if (t.startsWith("#")) t = t.substring(1);
        if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);

        try {
            if (t.length() == 6) {
                int rgb = Integer.parseUnsignedInt(t, 16);
                return 0xFF000000 | rgb;
            }
            if (t.length() == 8) {
                return (int) Long.parseLong(t, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return fallbackArgb;
    }
}
