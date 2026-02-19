package dbrighthd.elytratrails.trailrendering;

import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

public final class TrailStore {

    public record TrailPoint(Vec3 pos, long timeNanos, boolean breakHere) {
        public static TrailPoint point(Vec3 pos, long timeNanos) {
            return new TrailPoint(pos, timeNanos, false);
        }
        public static TrailPoint brk(long timeNanos) {
            return new TrailPoint(null, timeNanos, true);
        }
    }

    /**
     * one structure for ALL trails. used to be LEFT and RIGHT.
     * key = pack(entityId, trailIndex).
     */
    public static final Long2ObjectMap<Deque<TrailPoint>> TRAILS = new Long2ObjectOpenHashMap<>();

    /** Per-(entityId, trailIndex) cached left/right side for UV flipping. */
    public static final Long2BooleanMap IS_LEFT_WING_BY_KEY = new Long2BooleanOpenHashMap();

    public static long key(int entityId, int trailIndex) {
        return (((long) entityId) << 32) | (trailIndex & 0xffffffffL);
    }

    public static int entityId(long key) {
        return (int) (key >>> 32);
    }

    public static int trailIndex(long key) {
        return (int) key;
    }

    public static void setLeftWing(int entityId, int trailIndex, boolean isLeftWing) {
        IS_LEFT_WING_BY_KEY.put(key(entityId, trailIndex), isLeftWing);
    }

    public static void setLeftWing(long packedKey, boolean isLeftWing) {
        IS_LEFT_WING_BY_KEY.put(packedKey, isLeftWing);
    }

    public static boolean isLeftWing(long packedKey) {
        return IS_LEFT_WING_BY_KEY.getOrDefault(packedKey, false);
    }

    public static void removeMeta(long packedKey) {
        IS_LEFT_WING_BY_KEY.remove(packedKey);
    }

    /**
     * New: add a single indexed trail point.
     */
    public static void add(int entityId, int trailIndex, Vec3 worldPos, long now) {
        addOne(key(entityId, trailIndex), worldPos, now);
    }

    /**
     * Break a specific trail index (optional).
     */
    public static void breakTrail(int entityId, int trailIndex, long now) {
        breakOne(key(entityId, trailIndex), now);
    }

    /**
     * Break ALL trails for this entity.
     */
    public static void breakEntity(int entityId, long now) {
        for (Long2ObjectMap.Entry<Deque<TrailPoint>> e : TRAILS.long2ObjectEntrySet()) {
            if (entityId(e.getLongKey()) != entityId) continue;

            Deque<TrailPoint> trailPoints = e.getValue();
            TrailPoint last = trailPoints.peekLast();
            if (last == null || !last.breakHere()) {
                trailPoints.addLast(TrailPoint.brk(now));
            }
            pruneOld(trailPoints, now);
        }
    }

    public static long getLifetime() {
        ModConfig cfg = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        double lifetimeSeconds = TrailPackConfigManager.getMaxLifetimeSeconds(cfg.trailLifetime);
        return (long) (lifetimeSeconds * 1_000_000_000L);
    }

    private static void addOne(long packedKey, Vec3 worldPos, long nowNanos) {
        if (worldPos == null) return;

        Deque<TrailPoint> q = TrailStore.TRAILS.get(packedKey);
        if (q == null) {
            q = new ArrayDeque<>();
            TrailStore.TRAILS.put(packedKey, q);
        }

        Vec3 lastPos = null;
        for (var it = q.descendingIterator(); it.hasNext(); ) {
            TrailPoint p = it.next();
            if (p.breakHere()) break;
            if (p.pos() != null) { lastPos = p.pos(); break; }
        }

        final double MAX_SEGMENT_LEN = 128.0;
        if (lastPos != null && lastPos.distanceTo(worldPos) > MAX_SEGMENT_LEN) {
            q.addLast(TrailPoint.brk(nowNanos));
        }
        q.addLast(TrailPoint.point(worldPos, nowNanos));
        pruneOld(q, nowNanos);
    }

    private static void breakOne(long key, long nowNanos) {
        Deque<TrailPoint> trailPoints = TrailStore.TRAILS.get(key);
        if (trailPoints == null) {
            trailPoints = new ArrayDeque<>();
            TrailStore.TRAILS.put(key, trailPoints);
        }

        TrailPoint last = trailPoints.peekLast();
        if (last == null || !last.breakHere()) {
            trailPoints.addLast(TrailPoint.brk(nowNanos));
        }

        pruneOld(trailPoints, nowNanos);
    }

    private static void pruneOld(Deque<TrailPoint> trailPoints, long nowNanos) {
        long lifetimeNanos = getLifetime();
        while (!trailPoints.isEmpty() && nowNanos - trailPoints.peekFirst().timeNanos() > lifetimeNanos) {
            trailPoints.removeFirst();
        }
    }

    public static void cleanup(long now) {
        cleanupMap(now);
    }

    private static void cleanupMap(long nowNanos) {
        var entryIterator = TrailStore.TRAILS.long2ObjectEntrySet().iterator();

        while (entryIterator.hasNext()) {
            var entry = entryIterator.next();
            long packedKey = entry.getLongKey();

            Deque<TrailPoint> trailPoints = entry.getValue();
            pruneOld(trailPoints, nowNanos);

            if (trailPoints.isEmpty()) {
                // When the points are fully gone, also drop any cached model/bone context + wing-side meta
                // so ids can be safely reused and we don't leak memory.
                TrailContextStore.removeKey(packedKey);
                removeMeta(packedKey);
                entryIterator.remove();
            }
        }
    }

    private TrailStore() {}
}
