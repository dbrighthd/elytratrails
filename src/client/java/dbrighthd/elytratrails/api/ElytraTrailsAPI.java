package dbrighthd.elytratrails.api;

import dbrighthd.elytratrails.config.pack.TrailOverrides;
import dbrighthd.elytratrails.rendering.ColorOverride;
import dbrighthd.elytratrails.twirling.TwirlManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ElytraTrailsAPI {
    private static final Map<Function<Entity, Boolean>, Function<Integer, Integer>> conditionalColorOverrides = new HashMap<>();
    private static final Map<EntityType<?>, ResolvedValues> trailOverridesPerEntityType = new HashMap<>();
    private static final Map<Integer,ResolvedValues> trailOverridesPerEntity = new HashMap<>();


    public static void setConditionalColorOverrides(Function<Entity, Boolean> trailConditionFunction, Function<Integer, Integer> colorOverrideFunction) {
        conditionalColorOverrides.put(trailConditionFunction, colorOverrideFunction);
    }

    public static void setEntityTypeTrailOverrides(EntityType<?> entityType, TrailOverrides trailOverrides)
    {
        trailOverridesPerEntityType.put(entityType, new ResolvedValues(trailOverrides));
    }

    public static void addEntityTypeTrailOverridesIfNotPresent(EntityType<?> entityType, TrailOverrides trailOverrides)
    {
        if(trailOverridesPerEntityType.containsKey(entityType))
        {
            return;
        }
        trailOverridesPerEntityType.put(entityType, new ResolvedValues(trailOverrides));
    }

    public static void setEntityTrailOverrides(int entityId, TrailOverrides trailOverrides)
    {
        trailOverridesPerEntity.put(entityId, new ResolvedValues(trailOverrides));
    }

    public static void addEntityTrailOverridesIfNotPresent(int eid, TrailOverrides trailOverrides)
    {
        if(trailOverridesPerEntity.containsKey(eid))
        {
            return;
        }
        trailOverridesPerEntity.put(eid, new ResolvedValues(trailOverrides));
    }

    public static void removeConditionalColorOverrides(Function<Entity, Boolean> trailConditionFunction) {
        conditionalColorOverrides.remove(trailConditionFunction);
    }

    public static void removeEntityTypeTrailOverride(EntityType<?> entityType)
    {
        trailOverridesPerEntityType.remove(entityType);
    }

    public static void removeEntityTrailOverride(int entityId)
    {
        trailOverridesPerEntity.remove(entityId);
    }

    public static boolean entityHasAnyTrailOverrides(Entity entity)
    {
        return trailOverridesPerEntity.containsKey(entity.getId()) || trailOverridesPerEntityType.containsKey(entity.getType());
    }

    public static boolean entityHasSpecificTrailOverrides(Entity entity)
    {
        return trailOverridesPerEntity.containsKey(entity.getId());
    }

    public static boolean entityTypeHasOverrides(EntityType<?> entityType)
    {
        return trailOverridesPerEntityType.containsKey(entityType);
    }


    public static ResolvedValues getTrailOverrides(Entity entity)
    {
        if(trailOverridesPerEntity.containsKey(entity.getId()))
        {
            return trailOverridesPerEntity.get(entity.getId());
        }
        if(trailOverridesPerEntityType.containsKey(entity.getType()))
        {
            return trailOverridesPerEntityType.get(entity.getType());
        }
        return null;
    }

    public static boolean doAPIOverridesExist()
    {
        return !trailOverridesPerEntityType.isEmpty() || !trailOverridesPerEntity.isEmpty();
    }

    public static ColorOverride getColorOverrideForEntity(Entity entity) {
        for (var pair : conditionalColorOverrides.entrySet()) {
            if (pair.getKey().apply(entity)) {
                return new ColorOverride(pair.getValue(), entity.getId());
            }
        }
        return null;
    }

    public static boolean isEntityTwirling(Entity entity)
    {
        return TwirlManager.isRolling(entity.getId());
    }

    public static float getEntityTwirlAngleRadians(Entity entity,float partialTick)
    {
        return TwirlManager.getExtraRollRadians(entity.getId(),partialTick);
    }
}