package dbrighthd.elytratrails.handler;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

public class ParticleHandler {
    private static final double MIN_SPEED = 0.10;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = getConfig();
            if (!cfg.enableParticles) return;

            ClientLevel level = client.level;
            LocalPlayer player = client.player;

            if (level == null || player == null || !player.isFallFlying()) return;
            if (client.isPaused()) return;

            Vec3 vel = player.getDeltaMovement();
            double speedSq = vel.lengthSqr();
            if (speedSq < (MIN_SPEED * MIN_SPEED)) return;

            ParticleOptions particle = cfg.particle;

            int cubeRadiusBlocks = (int) Math.max(0, Math.floor(cfg.particlesBlockRadius));
            int perTick = Math.max(0, cfg.particleSpawnsPerTick);

            Vec3 shiftedCenterPos = player.position()
                    .add(vel.scale(cfg.particlesVelocityAhead))
                    .subtract(vel.scale(cfg.particlesVelocityBackwards));

            BlockPos shiftedCenterBlock = BlockPos.containing(shiftedCenterPos);

            Vec3 particleVel = vel.scale(cfg.particlesVelocityBackwards);

            for (BlockPos blockPos : BlockPos.randomInCube(player.getRandom(), perTick, shiftedCenterBlock, cubeRadiusBlocks)) {
                Vec3 pos = Vec3.atCenterOf(blockPos).offsetRandom(player.getRandom(), 2);

                level.addParticle(
                        particle,
                        pos.x, pos.y, pos.z,
                        -particleVel.x, -particleVel.y, -particleVel.z
                );
            }
        });
    }
}
