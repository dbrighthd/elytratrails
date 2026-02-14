package dbrighthd.elytratrails.trailrendering;

import dbrighthd.elytratrails.config.ModConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
    public static final Int2ObjectMap<Deque<TrailPoint>> LEFT = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<Deque<TrailPoint>> RIGHT = new Int2ObjectOpenHashMap<>();

    public static void add(int id, Vec3 left, Vec3 right, long now) {
        addOne(LEFT, id, left, now);
        addOne(RIGHT, id, right, now);
    }

    public static void breakTrail(int entityId, long now) {
        breakOne(LEFT, entityId, now);
        breakOne(RIGHT, entityId, now);
    }

    public static long getLifetime() {
        double lifetimeSeconds = AutoConfig.getConfigHolder(ModConfig.class).getConfig().trailLifetime;
        return (long) (lifetimeSeconds * 1_000_000_000L);
    }

    private static void addOne(Int2ObjectMap<Deque<TrailPoint>> trailsByEntityId, int entityId, Vec3 worldPos, long nowNanos) {
        if (worldPos == null) return;

        Deque<TrailPoint> trailPoints = trailsByEntityId.get(entityId);
        if (trailPoints == null) {
            trailPoints = new ArrayDeque<>();
            trailsByEntityId.put(entityId, trailPoints);
        }

        trailPoints.addLast(TrailPoint.point(worldPos, nowNanos));

        pruneOld(trailPoints, nowNanos);
    }

    private static void breakOne(Int2ObjectMap<Deque<TrailPoint>> trailsByEntityId, int entityId, long nowNanos) {
        Deque<TrailPoint> trailPoints = trailsByEntityId.get(entityId);
        if (trailPoints == null) {
            trailPoints = new ArrayDeque<>();
            trailsByEntityId.put(entityId, trailPoints);
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
        cleanupMap(LEFT, now);
        cleanupMap(RIGHT, now);
    }

    private static void cleanupMap(Int2ObjectMap<Deque<TrailPoint>> trailsByEntityId, long nowNanos) {
        var entryIterator = trailsByEntityId.int2ObjectEntrySet().iterator();

        while (entryIterator.hasNext()) {
            var entityEntry = entryIterator.next();

            Deque<TrailPoint> trailPoints = entityEntry.getValue();
            pruneOld(trailPoints, nowNanos);

            if (trailPoints.isEmpty()) {
                entryIterator.remove();
            }
        }
    }

    public static void clear(int entityId) {
        LEFT.remove(entityId);
        RIGHT.remove(entityId);
    }

    private TrailStore() {}
}
