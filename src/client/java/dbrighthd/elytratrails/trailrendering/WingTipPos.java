package dbrighthd.elytratrails.trailrendering;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.phys.Vec3;

public final class WingTipPos {

    public record WingTipsSample(Vec3 leftWingTipWorld, Vec3 rightWingTipWorld, long capturedAtNanos) {}

    private static final Int2ObjectMap<WingTipsSample> latestSamplesByEntityId =
            new Int2ObjectOpenHashMap<>();

    private WingTipPos() {}

    public static void put(int entityId, Vec3 leftWingTipWorld, Vec3 rightWingTipWorld, long capturedAtNanos) {
        latestSamplesByEntityId.put(entityId, new WingTipsSample(leftWingTipWorld, rightWingTipWorld, capturedAtNanos));
    }

    @FunctionalInterface
    public interface WingTipsConsumer {
        void accept(int entityId, Vec3 leftWingTipWorld, Vec3 rightWingTipWorld, long capturedAtNanos);
    }

    public static void consumeAll(WingTipsConsumer consumer) {
        var entryIterator = latestSamplesByEntityId.int2ObjectEntrySet().iterator();
        while (entryIterator.hasNext()) {
            var entry = entryIterator.next();

            int entityId = entry.getIntKey();
            WingTipsSample sample = entry.getValue();
            if (sample == null) continue;

            consumer.accept(entityId, sample.leftWingTipWorld(), sample.rightWingTipWorld(), sample.capturedAtNanos());

            entryIterator.remove();
        }
    }
}
