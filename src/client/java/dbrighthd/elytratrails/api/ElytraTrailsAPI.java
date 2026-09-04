package dbrighthd.elytratrails.api;

import dbrighthd.elytratrails.config.pack.TrailOverrides;
import dbrighthd.elytratrails.rendering.ColorOverride;
import dbrighthd.elytratrails.rendering.TrailSystem;
import dbrighthd.elytratrails.twirling.EaseTypes;
import dbrighthd.elytratrails.twirling.TwirlManager;
import dbrighthd.elytratrails.twirling.types.EaseType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This is how other mods should interact with Elytra Contrails!
 * Will be expanded in the future
 */
public class ElytraTrailsAPI {
    private static final Map<Function<Entity, Boolean>, Function<Integer, Integer>> conditionalColorOverrides = new HashMap<>();
    private static final Map<EntityType<?>, ResolvedValues> trailOverridesPerEntityType = new HashMap<>();
    private static final Map<Integer,ResolvedValues> trailOverridesPerEntity = new HashMap<>();

    /**
     * If people want to register their own Ease Types, they can use this. It should then show up in the Elytra Contrails modmenu
     * @param easeType new EaseType to register
     * @return the same easeType back, in case they want to use it for their own purposes.
     */
    @SuppressWarnings("unused") //it's an API method!
    public static EaseType registerEaseType(EaseType easeType)
    {
        return EaseTypes.register(easeType);
    }

    /**
     * Set a color override for trails based on a condition function
     * @param trailConditionFunction condition
     * @param colorOverrideFunction color function (eid -> color)
     */
    public static void setConditionalColorOverrides(Function<Entity, Boolean> trailConditionFunction, Function<Integer, Integer> colorOverrideFunction) {
        conditionalColorOverrides.put(trailConditionFunction, colorOverrideFunction);
    }

    /**
     * adds trail overrides per entity type;
     * @param entityType entity type
     * @param trailOverrides the settings to override for this trail. see the TrailOverrides class for more information!
     */
    @SuppressWarnings("unused") //it's an API method!
    public static void setEntityTypeTrailOverrides(EntityType<?> entityType, TrailOverrides trailOverrides)
    {
        trailOverridesPerEntityType.put(entityType, new ResolvedValues(trailOverrides));
    }

    /**
     * adds trail overrides per entity type if none exist.
     * @param entityType entity type
     * @param trailOverrides the settings to override for this trail. see the TrailOverrides class for more information!
     */
    @SuppressWarnings("unused") //it's an API method!
    public static void addEntityTypeTrailOverridesIfNotPresent(EntityType<?> entityType, TrailOverrides trailOverrides)
    {
        if(trailOverridesPerEntityType.containsKey(entityType))
        {
            return;
        }
        trailOverridesPerEntityType.put(entityType, new ResolvedValues(trailOverrides));
    }

    /**
     * adds trail overrides per entity
     * @param entity entity
     * @param trailOverrides the settings to override for this trail. see the TrailOverrides class for more information!
     */
    @SuppressWarnings("unused") //it's an API method!
    public static void setEntityTrailOverrides(Entity entity, TrailOverrides trailOverrides)
    {
        trailOverridesPerEntity.put(entity.getId(), new ResolvedValues(trailOverrides));
    }

    /**
     * adds trail overrides per entity if none exist
     * @param entity entity
     * @param trailOverrides the settings to override for this trail. see the TrailOverrides class for more information!
     */
    @SuppressWarnings("unused") //it's an API method!
    public static void addEntityTrailOverridesIfNotPresent(Entity entity, TrailOverrides trailOverrides)
    {
        if(trailOverridesPerEntity.containsKey(entity.getId()))
        {
            return;
        }
        trailOverridesPerEntity.put(entity.getId(), new ResolvedValues(trailOverrides));
    }

    /**
     * removes trail color overrides
     */
    @SuppressWarnings("unused") //it's an API method!
    public static void removeConditionalColorOverrides(Function<Entity, Boolean> trailConditionFunction) {
        conditionalColorOverrides.remove(trailConditionFunction);
    }

    /**
     * removes EntityType trail overrides
     * @param entityType EntityType
     */
    @SuppressWarnings("unused") //it's an API method!
    public static void removeEntityTypeTrailOverride(EntityType<?> entityType)
    {
        trailOverridesPerEntityType.remove(entityType);
    }

    /**
     * Removes trail overrides for an entity
     * @param entity entity to remove overrides from
     */
    @SuppressWarnings("unused") //it's an API method!
    public static void removeEntityTrailOverride(Entity entity)
    {
        trailOverridesPerEntity.remove(entity.getId());
        TrailSystem.getTrailManager().stopTrail(entity.getId());
    }

    public static boolean entityHasAnyTrailOverrides(Entity entity)
    {
        return trailOverridesPerEntity.containsKey(entity.getId()) || trailOverridesPerEntityType.containsKey(entity.getType());
    }

    /**
     * Checks if entity has any API trail overrides
     * @param entity entity to check
     * @return if the entity has overrides
     */
    @SuppressWarnings("unused") //it's an API method!
    public static boolean entityHasSpecificTrailOverrides(Entity entity)
    {
        return trailOverridesPerEntity.containsKey(entity.getId());
    }

    /**
     * if the entityType has any trail overrides
     * @param entityType EntityType to check
     * @return if the entityType has overrides
     */
    @SuppressWarnings("unused") //it's an API method!
    public static boolean entityTypeHasOverrides(EntityType<?> entityType)
    {
        return trailOverridesPerEntityType.containsKey(entityType);
    }


    /**
     * get API overrides of an entity
     * @param entity entity to get overrides for
     * @return trail overrides
     */
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

    /**
     * Are there any API trail overrides to begin with?
     * @return if there are any API trail overrides
     */
    public static boolean doAPIOverridesExist()
    {
        return !trailOverridesPerEntityType.isEmpty() || !trailOverridesPerEntity.isEmpty();
    }

    /**
     * Get color overrides for entity
     * @param entity entity to check overrides for
     * @return color overrides
     */
    public static ColorOverride getColorOverrideForEntity(Entity entity) {
        for (var pair : conditionalColorOverrides.entrySet()) {
            if (pair.getKey().apply(entity)) {
                return new ColorOverride(pair.getValue(), entity.getId());
            }
        }
        return null;
    }

    /**
     * Is the entity twirling?
     * @param entity entity to check
     * @return if its twirling
     */
    @SuppressWarnings("unused") //it's an API method!
    public static boolean isEntityTwirling(Entity entity)
    {
        return TwirlManager.isRolling(entity.getId());
    }

    /**
     * Current rotation of a twirling entity
     * @param entity entity to check
     * @param partialTick current partial tick
     * @return twirl angle (radians)
     */
    @SuppressWarnings("unused") //it's an API method!
    public static float getEntityTwirlAngleRadians(Entity entity,float partialTick)
    {
        return TwirlManager.getExtraRollRadians(entity.getId(),partialTick);
    }

    /**
     * does the entity have any active (emitting) trails?
     * @param entity entity to check
     * @return if the entity is currently emitting trails
     */
    @SuppressWarnings("unused")
    public static boolean doesEntityHaveActiveTrails(Entity entity)
    {
        return TrailSystem.getTrailManager().entityHasActiveTrails(entity.getId());
    }
}