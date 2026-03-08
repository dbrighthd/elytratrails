package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import dbrighthd.elytratrails.util.TimeUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.config.pack.TrailPackConfigManager.*;
import static dbrighthd.elytratrails.controller.EntityTwirlManager.isRolling;

public class TrailManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrailManager.class);

    private final Int2ObjectMap<EntityTrailGroup> activeTrails = new Int2ObjectOpenHashMap<>();
    public final Map<Long,Float> deadPointDistance = new HashMap<>();
    long trailId = 0;
    private final List<Trail> trails = new ArrayList<>();
    private float lastSample;
    private final WingTipSampler sampler;
    private ModConfig modConfig;
    public TrailManager(WingTipSampler sampler) {
        this.sampler = sampler;
        ClientTickEvents.END_CLIENT_TICK.register(this::removeDeadPoints);
        WorldRenderEvents.AFTER_ENTITIES.register(cxt -> {
            modConfig = getConfig();
            float now = TimeUtil.currentMillis();
            boolean recordEmitters = true;
            if((now - lastSample) < (1000f / modConfig.maxSamplePerSecond))
            {
                if(modConfig.alwaysSnapTrail)
                {
                    recordEmitters = false;
                }
                else
                {
                    return;
                }
            }
            else
            {
                lastSample = TimeUtil.currentMillis();
            }
            gatherPlayerTrails(Minecraft.getInstance(), recordEmitters);
            if(modConfig.extendedEmfSupport && (!entitiesWithTrails.isEmpty() || !entitiesWithTrailOverrides.isEmpty()))
            {
                gatherEntityTrails(Minecraft.getInstance(), recordEmitters);
            }
        });
    }
    public long newTrailId()
    {
        return trailId++;
    }
    public boolean isActiveTrail(Trail trail)
    {
        return (activeTrails.containsKey(trail.entityId()) && activeTrails.get(trail.entityId()).trails().contains(trail));
    }

    @SuppressWarnings("unused")
    public boolean entityHasActiveTrails(int eid)
    {
        return(activeTrails.containsKey(eid));
    }
    private void removeDeadPoints(Minecraft ctx) {
        long currentTime = TimeUtil.currentMillis();
        if(getConfig().logTrails)
        {
            for (Trail trail : trails)
            {
                if (trail.points().isEmpty())
                {
                    LOGGER.info("Removed trail from entity {}, no trail points.", trail.entityId());
                    continue;
                }
                if (trail.points().stream().allMatch(p -> currentTime - p.epoch() > trail.config().trailLifetime() * 1000))
                {
                    LOGGER.info("Removed trail from entity {}, all trail points exceeded lifetime {}", trail.entityId(), trail.config().trailLifetime());
                }

            }
        }
        trails.removeIf(t -> (t.points().isEmpty() || t.points().stream().allMatch(p -> currentTime - p.epoch() > t.config().trailLifetime() * 1000)) && removeTrailFromMap(t));
        for (Trail trail : trails) {
            List<Trail.Point> points = trail.points();
            if (points.size() < 2) continue;

            float removedDistance = 0.0f;
            long lifetimeMs = (long) (trail.config().trailLifetime() * 1000.0);

            while (points.size() > 1) {
                Trail.Point point = points.get(0);

                if (currentTime - point.epoch() <= lifetimeMs) {
                    break;
                }

                removedDistance += (float) point.pos().distanceTo(points.get(1).pos());
                points.removeFirst();
            }

            if (removedDistance > 0.0f) {
                deadPointDistance.merge(trail.trailId(), removedDistance, Float::sum);
            }
        }
    }
    public boolean removeTrailFromMap(Trail trail)
    {
        deadPointDistance.remove(trail.trailId());
        return true;

    }

    public void removeTrail(int entityId)
    {
        if(getConfig().logTrails && activeTrails.containsKey(entityId))
        {
            LOGGER.info("Stopped trail for entity {}", entityId);
        }
        activeTrails.remove(entityId);
    }

    private void gatherPlayerTrails(Minecraft ctx, boolean recordEmitter) {
        if (ctx.level == null) return;

        List<AbstractClientPlayer> players = ctx.level.players();
        sampler.clearFrameCache();
        for (AbstractClientPlayer player : players) {
            int eid = player.getId();
            TrailPackConfigManager.ResolvedTrailSettings config = getConfigFromPlayerId(eid);
            boolean valid = TrailManager.isEntityTrailValid(config, player);

            if (valid) {
                List<Emitter> emitters = sampler.getPlayerTrailEmitterPositions(player, ctx.getDeltaTracker().getGameTimeDeltaPartialTick(false),modConfig);
                double speed = player.getDeltaMovement().length();
                if (emitters.isEmpty())
                {
                    if(modConfig.logTrails)
                    {
                        LOGGER.info("Empty Emitters from {}, resetting trails if exist",eid);
                    }
                    activeTrails.remove(eid);
                    continue;
                }
                if(!recordEmitter)
                {
                    return;
                }
                EntityTrailGroup trailGroup = activeTrails.computeIfAbsent(eid, id -> {
                    List<Trail> emittedTrails = new ArrayList<>();
                    int emitterId = 0;
                    for (Emitter emitter : emitters) {
                        emittedTrails.add(Trail.fromPlayerConfig(player.getId(), emitter,emitterId, newTrailId()));
                        emitterId++;
                    }

                    trails.addAll(emittedTrails);
                    if(modConfig.logTrails)
                    {
                        LOGGER.info("Created new trail group with {} trails for entity {} (player)", emittedTrails.size(), id);
                    }
                    return new EntityTrailGroup(
                            emittedTrails
                    );
                });
                if (trailGroup.trails().size() != emitters.size()) {
                    activeTrails.remove(eid);
                    return;
                }
                for (int i = 0; i < trailGroup.trails().size(); i++)  {

                    Trail trail = trailGroup.trails().get(i);
                    Vec3 emitter = emitters.get(i).position();
                    trail.points().add(new Trail.Point(emitter,speed));
                }
            } else {
                removeTrail(eid);
            }
        }
    }
    public int activeTrailsNumber() {
        return activeTrails.values().stream()
                .mapToInt(group -> group.trails().size())
                .sum();
    }
    public int trailsNumber()
    {
        return trails.size();
    }
    private void gatherEntityTrails(Minecraft ctx, boolean recordEmitter) {
        if (ctx.level == null) return;
        for (Entity entity :  ctx.level.entitiesForRendering()) {
            if(!TrailPackConfigManager.doesEntityHaveEmfTrails(entity) && ((!modConfig.tryWithoutEmf) && doesEntityHaveOverrides(entity)) || (!doesEntityHaveOverrides(entity) && !doesEntityHaveEmfTrails(entity)))
            {
                continue;
            }
            int eid = entity.getId();
            TrailPackConfigManager.ResolvedTrailSettings config = getConfigFromPlayerId(eid);
            boolean valid = TrailManager.isEntityTrailValid(config, entity);

            if (valid) {
                List<Emitter> emitters = sampler.getEntityTrailEmitterPositions(entity, ctx.getDeltaTracker().getGameTimeDeltaPartialTick(false));
                double speed = entity.getDeltaMovement().length();
                if(entity instanceof Player)
                {
                    continue;
                }
                if (emitters.isEmpty())
                {
                    if(modConfig.logTrails)
                    {
                        LOGGER.info("Empty Emitters from non-player entity {} ({}), resetting trails if exist",eid, entity.getType());
                    }
                    activeTrails.remove(eid);
                    continue;
                }
                if(!recordEmitter)
                {
                    return;
                }
                EntityTrailGroup trailGroup = activeTrails.computeIfAbsent(eid, id -> {
                    List<Trail> emittedTrails = new ArrayList<>();
                    int emitterId = 0;
                    for (Emitter emitter : emitters) {
                        emittedTrails.add(Trail.fromPlayerConfig(entity.getId(), emitter,emitterId, newTrailId()));
                        emitterId++;
                    }

                    trails.addAll(emittedTrails);
                    if(modConfig.logTrails)
                    {
                        LOGGER.info("Created new trail group with {} trails for entity {} ({}})", emittedTrails.size(), id, entity.getType());
                    }
                    return new EntityTrailGroup(
                            emittedTrails
                    );
                });
                if (trailGroup.trails().size() != emitters.size()) {
                    activeTrails.remove(eid);
                    return;
                }
                for (int i = 0; i < trailGroup.trails().size(); i++)  {

                    Trail trail = trailGroup.trails().get(i);
                    Vec3 emitter = emitters.get(i).position();
                    trail.points().add(new Trail.Point(emitter,speed));
                }
            } else {
                removeTrail(eid);
            }
        }
    }
    public static TrailPackConfigManager.ResolvedTrailSettings getConfigFromPlayerId(int entityId)
    {
        return TrailPackConfigManager.resolveFromPlayerConfig(ClientPlayerConfigStore.getOrDefault(entityId));
    }

    public static boolean isEntityTrailValid(TrailPackConfigManager.ResolvedTrailSettings config, Entity entity) {
        if (entity instanceof Player player) {
            if(!(player.getPose() == Pose.FALL_FLYING))
            {
                return false;
            }
        }
        return (isRolling(entity.getId()) && config.alwaysShowTrailDuringTwirl()) || !config.speedDependentTrail() || (entity.getDeltaMovement().lengthSqr() > config.trailMinSpeed() * config.trailMinSpeed());
    }

    public List<Trail> trails() {
        return trails;
    }

    public void removeAllTrails()
    {
        if(modConfig.logTrails)
        {
            LOGGER.info("Cleared {} trails, of which {} were active.", trails.size(), activeTrails.size());
        }
        activeTrails.clear();
        trails.clear();
    }
}
