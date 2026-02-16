package dbrighthd.elytratrails.handler;

import dbrighthd.elytratrails.util.WingTipSamplerUtil;
import dbrighthd.elytratrails.trailrendering.TrailStore;
import dbrighthd.elytratrails.trailrendering.WingTipPos;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

public class WingTipSamplerHandler {
    private static final Int2LongOpenHashMap lastSampleTimeByEntity = new Int2LongOpenHashMap();
    private static long lastGlobalSampleNanos = Long.MIN_VALUE;

    private static final Int2BooleanOpenHashMap wasFallFlyingVanilla = new Int2BooleanOpenHashMap();
    private static final Int2BooleanOpenHashMap wasTrailShowing = new Int2BooleanOpenHashMap();
    private static final Int2LongOpenHashMap fallFlyingStartTimeByEntity = new Int2LongOpenHashMap();
    private static final long FALLFLYING_WARMUP_NANOS = 100_000_000L; // 0.1 seconds
    public static void init()
    {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            if (!getConfig().enableTrail) return;

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
                TrailStore.breakTrail(localId, now);

                wasTrailShowing.put(localId, false);
                wasFallFlyingVanilla.put(localId, false);
                fallFlyingStartTimeByEntity.remove(localId);
                lastSampleTimeByEntity.remove(localId);
            }

            final IntOpenHashSet sampledThisFrame = new IntOpenHashSet();

            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            WingTipSamplerUtil.sample(partialTick);

            WingTipPos.consumeAll((entityId, left, right, capturedAtNanos) -> {
                sampledThisFrame.add(entityId);

                if (skipLocalFirstPerson && entityId == localId) {
                    return;
                }

                var entity = mc.level.getEntity(entityId);
                if (!(entity instanceof Player living)) {
                    return;
                }

                boolean flyingNow = living.isFallFlying();
                boolean flyingBefore = wasFallFlyingVanilla.getOrDefault(entityId, false);

                if (flyingNow && !flyingBefore) {
                    fallFlyingStartTimeByEntity.put(entityId, now);
                } else if (!flyingNow && flyingBefore) {
                    fallFlyingStartTimeByEntity.remove(entityId);
                }
                wasFallFlyingVanilla.put(entityId, flyingNow);

                boolean showingNow = showTrail(living, now);
                boolean showingBefore = wasTrailShowing.getOrDefault(entityId, false);

                if (showingNow && !showingBefore) {
                    TrailStore.breakTrail(entityId, now);
                }
                wasTrailShowing.put(entityId, showingNow);

                if (!showingNow) {
                    return;
                }

                long last = lastSampleTimeByEntity.getOrDefault(entityId, Long.MIN_VALUE);
                if (last != Long.MIN_VALUE && now - last < intervalNanos) {
                    return;
                }
                lastSampleTimeByEntity.put(entityId, now);

                TrailStore.add(entityId, left, right, now);
            });

            {
                var it = wasTrailShowing.keySet().iterator();
                while (it.hasNext()) {
                    int entityId = it.nextInt();
                    if (!sampledThisFrame.contains(entityId)) {
                        TrailStore.breakTrail(entityId, now);

                        wasTrailShowing.put(entityId, false);
                        wasFallFlyingVanilla.put(entityId, false);
                        fallFlyingStartTimeByEntity.remove(entityId);
                        lastSampleTimeByEntity.remove(entityId);
                    }
                }
            }

            TrailStore.cleanup(now);
        });
    }
    private static boolean showTrail(Player living, long nowNanos) {
        var cfg = getConfig();
        if (!living.isFallFlying()) return false;

        if (cfg.speedDependentTrail) {
            long start = fallFlyingStartTimeByEntity.getOrDefault(living.getId(), Long.MIN_VALUE);
            if (start == Long.MIN_VALUE) {
                fallFlyingStartTimeByEntity.put(living.getId(), nowNanos);
                return false;
            }

            if (nowNanos - start < FALLFLYING_WARMUP_NANOS) return false;

            if (cfg.trailMinSpeed >= 0.001) {
                return living.getKnownSpeed().length() > cfg.trailMinSpeed;
            }
            return true;
        }
        return true;
    }
}
