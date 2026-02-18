package dbrighthd.elytratrails.trailrendering;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.phys.Vec3;

public final class WingTipPos {

    /**
     * @param modelName  The active model name (pack-facing), e.g. "allay" for allay.jem.
     * @param boneNames  One entry per trail index, ideally matching the EMF bone name.
     */
    public record WingTipsSample(Vec3[] pointsWorld, long capturedAtNanos, String modelName, String[] boneNames) {}

    private static final Int2ObjectMap<WingTipsSample> latestSamplesByEntityId =
            new Int2ObjectOpenHashMap<>();

    private WingTipPos() {}

    public static void put(int entityId, Vec3[] pointsWorld, long capturedAtNanos, String modelName, String[] boneNames) {
        if (pointsWorld == null || pointsWorld.length == 0) return;
        // Defensive copy so later mutations don't affect queued samples
        latestSamplesByEntityId.put(entityId, new WingTipsSample(pointsWorld.clone(), capturedAtNanos, modelName, boneNames == null ? null : boneNames.clone()));
    }

    /**
     * back-compat: no model/bone metadata.
     */
    public static void put(int entityId, Vec3[] pointsWorld, long capturedAtNanos) {
        put(entityId, pointsWorld, capturedAtNanos, null, null);
    }

    @FunctionalInterface
    public interface WingTipsConsumer {
        void accept(int entityId, Vec3[] pointsWorld, long capturedAtNanos, String modelName, String[] boneNames);
    }

    public static void consumeAll(WingTipsConsumer consumer) {
        var entryIterator = latestSamplesByEntityId.int2ObjectEntrySet().iterator();
        while (entryIterator.hasNext()) {
            var entry = entryIterator.next();

            int entityId = entry.getIntKey();
            WingTipsSample sample = entry.getValue();
            if (sample == null) continue;

            consumer.accept(entityId, sample.pointsWorld(), sample.capturedAtNanos(), sample.modelName(), sample.boneNames());

            entryIterator.remove();
        }
    }
}
