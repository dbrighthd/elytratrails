package dbrighthd.elytratrails.handler;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
//import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
//import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import org.intellij.lang.annotations.Identifier;

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

//    public static String encodeParticle(ParticleOptions particleOptions)
//    {
//
//        // it's better if you don't look too closely at this and just know that it works
//        @SuppressWarnings("rawtypes")
//        ParticleType type = particleOptions.getType();
//        var codec = type.codec().codec();
//        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getResourceKey(type).map(ResourceKey::identifier).orElse(Identifier.withDefaultNamespace("poof"));
//
//
//        //noinspection unchecked
//        return id.toString() + codec.encodeStart(NbtOps.INSTANCE, particleOptions != null ? particleOptions : ParticleTypes.POOF).result().map(o -> ((Tag)o).toString()).orElse("");
//    }
//
//    public static ParticleOptions decodeParticle(String newValue)
//    {
//        HolderLookup.Provider lookup = VanillaRegistries.createLookup();
//
//        try {
//            return ParticleArgument.readParticle(new StringReader(newValue), lookup);
//        } catch (CommandSyntaxException e) {
//            return null;
//        }
//    }
}
