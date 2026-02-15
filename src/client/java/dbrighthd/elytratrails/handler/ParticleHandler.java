package dbrighthd.elytratrails.handler;

import dbrighthd.elytratrails.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

public class ParticleHandler {
    private static final double MIN_SPEED = 0.10;
    public static void init()
    {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!getConfig().enableParticles) return;

            ClientLevel level = client.level;
            LocalPlayer player = client.player;

            if (level == null || player == null || !player.isFallFlying()) return;
            if (client.isPaused()) return;

            Vec3 vel = player.getDeltaMovement();
            double speedSq = vel.lengthSqr();
            if (speedSq < (MIN_SPEED * MIN_SPEED)) return;

            ParticleOptions particle = chosenParticle();

            for (BlockPos blockPos : BlockPos.randomInCube(player.getRandom(), getConfig().particleSpawnsPerTick, player.blockPosition(), 10)) {
                Vec3 pos = Vec3.atCenterOf(blockPos).offsetRandom(player.getRandom(), 2);
                level.addParticle(particle, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        });
    }
    private static ParticleOptions chosenParticle() {
        var cfg = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        return switch (cfg.particle) {
            case CLOUD -> ParticleTypes.CLOUD;
            case END_ROD -> ParticleTypes.END_ROD;
            case HAPPY_VILLAGER -> ParticleTypes.HAPPY_VILLAGER;
            case FIREWORK -> ParticleTypes.FIREWORK;
            case ASH -> ParticleTypes.ASH;
        };
    }
}
