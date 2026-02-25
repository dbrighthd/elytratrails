package dbrighthd.elytratrails.rendering;


import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import dbrighthd.elytratrails.network.ClientPlayerConfigStore;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.controller.EntityTwirlManager.isRolling;

public class TrailManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrailManager.class);

    private final Int2ObjectMap<EntityTrailGroup> activeTrails = new Int2ObjectOpenHashMap<>();
    private final List<Trail> trails = new ArrayList<>();
    private float lastSample;
    private final WingTipSampler sampler;

    public TrailManager(WingTipSampler sampler) {
        this.sampler = sampler;
        ClientTickEvents.END_CLIENT_TICK.register(this::removeDeadPoints);
        WorldRenderEvents.AFTER_ENTITIES.register(cxt -> {
            float now = Util.getMillis();
            if((now - lastSample) < (1000f / getConfig().maxSamplePerSecond))
            {
                return;
            }
            lastSample = Util.getMillis();
            gatherPlayerTrails(Minecraft.getInstance());
        });
    }


    private void removeDeadPoints(Minecraft ctx) {
        long currentTime = Util.getMillis();
        trails.removeIf(t -> t.points().isEmpty() || t.points().stream().allMatch(p -> currentTime - p.epoch() > t.config().trailLifetime() * 1000));
    }

    public void removeTrail(int entityId)
    {
        activeTrails.remove(entityId);
    }

    private void gatherPlayerTrails(Minecraft ctx) {
        //ModConfig config = ElytraTrailsClient.getConfig();
        if (ctx.level == null) return;

        List<AbstractClientPlayer> players = ctx.level.players();
        for (AbstractClientPlayer player : players) {
            int eid = player.getId();
            TrailPackConfigManager.ResolvedTrailSettings config = getConfigFromPlayerId(eid);
            boolean valid = TrailManager.isEntityTrailValid(config, player);

            if (valid) {
                List<Emitter> emitters = sampler.getTrailEmitterPositions(player, ctx.getDeltaTracker().getGameTimeDeltaPartialTick(false));

                if (emitters.isEmpty())
                {
                    activeTrails.remove(eid);
                    continue;
                }

                EntityTrailGroup trailGroup = activeTrails.computeIfAbsent(eid, id -> {
                    List<Trail> emittedTrails = new ArrayList<>();
                    for (Emitter emitter : emitters) {
                        emittedTrails.add(Trail.fromPlayerConfig(player.getId(), emitter.flipUv()));
                    }

                    trails.addAll(emittedTrails);
                    LOGGER.info("Created new trail group with {} trails for entity {} (player)", emittedTrails.size(), id);
                    return new EntityTrailGroup(
                            emittedTrails
                    );
                });
                if (trailGroup.trails().size() != emitters.size()) {
                    activeTrails.remove(eid);
                    //trails.removeAll(trailGroup.trails());
                    return;
                }
                for (int i = 0; i < trailGroup.trails().size(); i++)  {

                    Trail trail = trailGroup.trails().get(i);
//                    if (i >= emitters.size())
//                    {
//                        activeTrails.remove(eid);
//                        continue;
//                    }
                    Vec3 emitter = emitters.get(i).position();
                    trail.points().add(new Trail.Point(emitter));
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
            if (player.getPose() != Pose.FALL_FLYING) return false;
        }

        return (isRolling(entity.getId()) && getConfig().alwaysShowTrailDuringTwirl) || !config.speedDependentTrail() || (entity.getDeltaMovement().lengthSqr() > config.trailMinSpeed() * config.trailMinSpeed());
    }

    public List<Trail> trails() {
        return trails;
    }

    public void removeAllTrails()
    {
        activeTrails.clear();
        trails.clear();
    }
}
