// src/client/java/dbrighthd/elytratrails/trailrendering/TrailContextStore.java
package dbrighthd.elytratrails.trailrendering;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

/**
 * This is used at render time to resolve resource-pack overrides.
 */
public final class TrailContextStore {
    private TrailContextStore() {}

    public record BoneCtx(String modelName, String boneName) {}

    private static final Long2ObjectMap<BoneCtx> CTX = new Long2ObjectOpenHashMap<>();

    public static void update(int entityId, String modelName, String[] boneNames) {
        if (boneNames == null || boneNames.length == 0) return;
        for (int i = 0; i < boneNames.length; i++) {
            String bone = boneNames[i];
            if (bone == null) continue;
            CTX.put(TrailStore.key(entityId, i), new BoneCtx(modelName, bone));
        }
    }

    public static BoneCtx get(int entityId, int trailIndex) {
        return CTX.get(TrailStore.key(entityId, trailIndex));
    }

    /**
     * Remove cached context for a single packed (entityId, trailIndex) key.
     * This lets resource-pack overrides (model/bone) keep applying while a trail is
     * fading out even after the entity has been removed, but still allows cleanup
     * when that trail fully expires.
     */
    public static void removeKey(long packedKey) {
        CTX.remove(packedKey);
    }

    public static void clear() {
        CTX.clear();
    }

    /**
     * Remove any cached (model,bone) context for all trails belonging to the given entity.
     * Important on the client because numeric entity ids can be reused after unload.
     */
    public static void clearEntity(int entityId) {
        CTX.long2ObjectEntrySet().removeIf(e -> TrailStore.entityId(e.getLongKey()) == entityId);
    }
}
