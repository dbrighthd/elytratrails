// src/client/java/dbrighthd/elytratrails/handler/WingTipSamplerHandler.java
package dbrighthd.elytratrails.handler;

import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.network.PlayerConfig;
import dbrighthd.elytratrails.trailrendering.TrailContextStore;
import dbrighthd.elytratrails.trailrendering.TrailStore;
import dbrighthd.elytratrails.trailrendering.WingTipPos;
import dbrighthd.elytratrails.util.WingTipSamplerUtil;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
public class WingTipSamplerHandler {
    private static final Int2LongOpenHashMap lastSampleTimeByEntity = new Int2LongOpenHashMap();
    private static long lastGlobalSampleNanos = Long.MIN_VALUE;

    private static final Int2BooleanOpenHashMap wasFallFlyingVanilla = new Int2BooleanOpenHashMap();

    /**
     * Per-(entity, trailIndex) "was showing" state.
     * Needed so speed-dependent trails can gate *each* trail independently (and allow per-trail
     * pack overrides via TrailPackConfigManager).
     */
    private static final Long2BooleanOpenHashMap wasTrailShowingByKey = new Long2BooleanOpenHashMap();

    private static final Int2LongOpenHashMap fallFlyingStartTimeByEntity = new Int2LongOpenHashMap();
    private static final long FALLFLYING_WARMUP_NANOS = 100_000_000L; // 0.1 seconds

    // NEW: per-entity warmup to ignore "bad first frame" samples on async loads (often spikes to 0,0,0)
    private static final Int2LongOpenHashMap warmupUntilByEntity = new Int2LongOpenHashMap();
    private static final long ENTITY_WARMUP_NANOS = 20_000_000L; // 0.02s (tune 0.2–1.0s)

    /**
     * Called when the client removes an entity (despawn/chunk unload/etc.).
     * We insert a break marker immediately so if the entity comes back later (or its numeric id
     * gets reused), we don't render a long connecting segment across the world.
     */
    public static void onEntityRemoved(int entityId) {
        long now = Util.getNanos();

        TrailStore.breakEntity(entityId, now);

        // Clear per-entity cached state so id reuse can't inherit stale context.
        wasFallFlyingVanilla.put(entityId, false);
        fallFlyingStartTimeByEntity.remove(entityId);
        lastSampleTimeByEntity.remove(entityId);
        warmupUntilByEntity.remove(entityId);
        clearTrailShowState(entityId);

        // IMPORTANT:
        // Don't clear TrailContextStore here. Trails can keep rendering for up to trailLifetime
        // after the entity is gone, and clearing context would make them revert to defaults.
        // Context is cleaned up when the trail deque becomes empty (TrailStore.cleanup()).
    }

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            if (!getConfig().enableAllTrails) return;

            long now = Util.getNanos();

            int maxSps = Math.max(1, getConfig().maxSamplePerSecond);
            long intervalNanos = 1_000_000_000L / maxSps;
            if (lastGlobalSampleNanos != Long.MIN_VALUE && (now - lastGlobalSampleNanos) < intervalNanos) {
                TrailStore.cleanup(now);
                return;
            }
            lastGlobalSampleNanos = now;

            final boolean firstPersonNow = mc.options.getCameraType().isFirstPerson();
            final Player localPlayer = mc.player;
            final int localId = (localPlayer == null) ? Integer.MIN_VALUE : localPlayer.getId();
            final boolean skipLocalFirstPerson = firstPersonNow && !getConfig().firstPersonTrail;

            if (skipLocalFirstPerson && localPlayer != null) {
                TrailStore.breakEntity(localId, now);

                wasFallFlyingVanilla.put(localId, false);
                fallFlyingStartTimeByEntity.remove(localId);
                lastSampleTimeByEntity.remove(localId);
                warmupUntilByEntity.remove(localId);
                clearTrailShowState(localId);
            }

            final IntOpenHashSet sampledThisFrame = new IntOpenHashSet();

            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            WingTipSamplerUtil.sample(partialTick);

            WingTipPos.consumeAll((entityId, pointsWorld, capturedAtNanos, modelName, boneNames) -> {
                sampledThisFrame.add(entityId);

                // Keep updating context even during warmup so pack overrides stick during fade-out.
                if (boneNames != null) {
                    TrailContextStore.update(entityId, modelName, boneNames);
                }

                if (skipLocalFirstPerson && entityId == localId) {
                    return;
                }

                Entity entity = mc.level.getEntity(entityId);
                if (entity == null) return;

                // Per-entity warmup to avoid "origin spike" samples for 1–2 frames.
                long warmupUntil = warmupUntilByEntity.getOrDefault(entityId, Long.MIN_VALUE);
                if (warmupUntil == Long.MIN_VALUE) {
                    warmupUntilByEntity.put(entityId, now + ENTITY_WARMUP_NANOS);
                    TrailStore.breakEntity(entityId, now); // don't bridge into warmup
                    return;
                }
                if (now < warmupUntil) {
                    return;
                }

                if (pointsWorld == null || pointsWorld.length == 0) return;

                // Entity-level eligibility.
                boolean entityEligible;
                if (entity instanceof Player living) {
                    boolean flyingNow = living.isFallFlying();
                    boolean flyingBefore = wasFallFlyingVanilla.getOrDefault(entityId, false);

                    if (flyingNow && !flyingBefore) {
                        fallFlyingStartTimeByEntity.put(entityId, now);
                    } else if (!flyingNow && flyingBefore) {
                        fallFlyingStartTimeByEntity.remove(entityId);
                    }
                    wasFallFlyingVanilla.put(entityId, flyingNow);

                    entityEligible = flyingNow;
                } else {
                    entityEligible = true;
                }

                if (!entityEligible) {
                    for (int i = 0; i < pointsWorld.length; i++) {
                        wasTrailShowingByKey.put(TrailStore.key(entityId, i), false);
                    }
                    return;
                }

                // Resolve + evaluate per-trail show state first (so transitions are detected even if we skip sampling this frame).
                boolean[] showThis = new boolean[pointsWorld.length];
                for (int i = 0; i < pointsWorld.length; i++) {
                    String boneName = (boneNames != null && i < boneNames.length) ? boneNames[i] : null;
                    TrailPackConfigManager.ResolvedTrailSettings trailCfg =
                            TrailPackConfigManager.resolve(modelName, boneName, getConfig());

                    boolean show = showTrailForIndex(entity, trailCfg, now);
                    showThis[i] = show;

                    long packedKey = TrailStore.key(entityId, i);
                    boolean wasShowing = wasTrailShowingByKey.getOrDefault(packedKey, false);
                    if (show && !wasShowing) {
                        TrailStore.breakTrail(entityId, i, now);
                    }
                    wasTrailShowingByKey.put(packedKey, show);
                }

                long last = lastSampleTimeByEntity.getOrDefault(entityId, Long.MIN_VALUE);
                if (last != Long.MIN_VALUE && now - last < intervalNanos) {
                    return;
                }
                lastSampleTimeByEntity.put(entityId, now);

                // Extra sanity: reject bad points far from the entity (common during load).
                Vec3 entityPos = entity.position();
                final double MAX_DIST_FROM_ENTITY = 64.0;
                final double MAX_DIST_SQ = MAX_DIST_FROM_ENTITY * MAX_DIST_FROM_ENTITY;

                for (int i = 0; i < pointsWorld.length; i++) {
                    if (!showThis[i]) continue;

                    Vec3 p = pointsWorld[i];
                    if (p == null) continue;

                    if (!Double.isFinite(p.x) || !Double.isFinite(p.y) || !Double.isFinite(p.z)) {
                        continue;
                    }

                    if (p.distanceToSqr(entityPos) > MAX_DIST_SQ) {
                        // Drop bogus sample instead of creating a huge segment (often towards 0,0,0)
                        continue;
                    }

                    TrailStore.add(entityId, i, p, now);
                }
            });

            // Entities not sampled this frame likely unloaded; break to avoid bridging and reset warmup.
            {
                var it = warmupUntilByEntity.keySet().iterator();
                while (it.hasNext()) {
                    int entityId = it.nextInt();
                    if (!sampledThisFrame.contains(entityId)) {
                        TrailStore.breakEntity(entityId, now);

                        wasFallFlyingVanilla.put(entityId, false);
                        fallFlyingStartTimeByEntity.remove(entityId);
                        lastSampleTimeByEntity.remove(entityId);
                        clearTrailShowState(entityId);

                        it.remove();
                    }
                }
            }

            TrailStore.cleanup(now);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TrailStore.TRAILS.clear();

            TrailContextStore.clear();

            WingTipSamplerHandler.clearAllState();
        });
    }

    public static void clearAllState() {
        lastSampleTimeByEntity.clear();
        wasFallFlyingVanilla.clear();
        wasTrailShowingByKey.clear();
        fallFlyingStartTimeByEntity.clear();
        warmupUntilByEntity.clear();
        lastGlobalSampleNanos = Long.MIN_VALUE;
    }

    private static void clearTrailShowState(int entityId) {
        wasTrailShowingByKey.long2BooleanEntrySet().removeIf(e -> TrailStore.entityId(e.getLongKey()) == entityId);
    }

    /**
     * determine whether a specific trail index should currently be emitting points.
     * when the global config enables speed-dependent trails, this applies the same min-speed gate
     * to *all* entities. per-trail resource-pack configs can override this by setting
     * "speedDependentTrail" (and optionally "trailMinSpeed" (if trailMinSpeed is 0 its the same)) for that model/bone.
     */
    private static boolean showTrailForIndex(Entity entity, TrailPackConfigManager.ResolvedTrailSettings cfg, long nowNanos) {
        if (entity instanceof Player living) {
            if (!living.isFallFlying()) return false;

            PlayerConfig playerConfig = ClientPlayerConfigStore.getOrDefault(living.getId());

            if (!playerConfig.enableTrail())
            {
                return false;
            }
            if (playerConfig.speedDependentTrail()) {
                int id = living.getId();

                long start = fallFlyingStartTimeByEntity.getOrDefault(id, Long.MIN_VALUE);
                if (start == Long.MIN_VALUE) {
                    fallFlyingStartTimeByEntity.put(id, nowNanos);
                    return false;
                }

                if (nowNanos - start < FALLFLYING_WARMUP_NANOS) return false;

                double min = cfg.trailMinSpeed();
                if (min >= 0.001) {
                    double minSq = min * min;
                    return living.getKnownSpeed().lengthSqr() > minSq;
                }
            }

            return true;
        }
        if (!cfg.enableTrail())
        {
            return false;
        }
        if (cfg.speedDependentTrail()) {
            double min = cfg.trailMinSpeed();
            if (min >= 0.001) {
                // delta movement for non players
                double minSq = min * min;
                return entity.getDeltaMovement().lengthSqr() > minSq;
            }
        }
        return true;
    }
}
