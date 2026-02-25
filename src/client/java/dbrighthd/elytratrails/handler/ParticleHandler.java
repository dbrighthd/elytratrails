package dbrighthd.elytratrails.handler;

import com.ibm.icu.text.MessagePattern;
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

            ParticleOptions particle = chosenParticle();

            int cubeRadiusBlocks = (int) Math.max(0, Math.floor(cfg.particlesBlockRadius));
            int perTick = Math.max(0, cfg.particleSpawnsPerTick);

            // Center: player's blockpos shifted forward/back along velocity
            // (use full velocity vector; multiplier is “how many blocks worth of vel”)
            Vec3 shiftedCenterPos = player.position()
                    .add(vel.scale(cfg.particlesVelocityAhead))
                    .subtract(vel.scale(cfg.particlesVelocityBackwards));

            BlockPos shiftedCenterBlock = BlockPos.containing(shiftedCenterPos);

            // Particle motion inherits player velocity (multiplier)
            // NOTE: Using particlesVelocityBackwards as "inherit multiplier" too (no dedicated config field provided).
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

    private static ParticleOptions chosenParticle() {
        var cfg = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        return switch (cfg.particle) {
            case ANGRY_VILLAGER -> ParticleTypes.ANGRY_VILLAGER;
            case ASH -> ParticleTypes.ASH;
            case BUBBLE -> ParticleTypes.BUBBLE;
            case BUBBLE_COLUMN_UP -> ParticleTypes.BUBBLE_COLUMN_UP;
            case BUBBLE_POP -> ParticleTypes.BUBBLE_POP;
            case CAMPFIRE_COSY_SMOKE -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
            case CAMPFIRE_SIGNAL_SMOKE -> ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;
            case CHERRY_LEAVES -> ParticleTypes.CHERRY_LEAVES;
            case CLOUD -> ParticleTypes.CLOUD;
            case COMPOSTER -> ParticleTypes.COMPOSTER;
            case COPPER_FLAME -> ParticleTypes.COPPER_FIRE_FLAME; // naming mismatch handled here
            case CRIMSON_SPORE -> ParticleTypes.CRIMSON_SPORE;
            case CRIT -> ParticleTypes.CRIT;
            case CURRENT_DOWN -> ParticleTypes.CURRENT_DOWN;
            case DAMAGE_INDICATOR -> ParticleTypes.DAMAGE_INDICATOR;
            case DOLPHIN -> ParticleTypes.DOLPHIN;
            case DRIPPING_DRIPSTONE_LAVA -> ParticleTypes.DRIPPING_DRIPSTONE_LAVA;
            case DRIPPING_DRIPSTONE_WATER -> ParticleTypes.DRIPPING_DRIPSTONE_WATER;
            case DRIPPING_HONEY -> ParticleTypes.DRIPPING_HONEY;
            case DRIPPING_LAVA -> ParticleTypes.DRIPPING_LAVA;
            case DRIPPING_OBSIDIAN_TEAR -> ParticleTypes.DRIPPING_OBSIDIAN_TEAR;
            case DRIPPING_WATER -> ParticleTypes.DRIPPING_WATER;

            case DUST_PLUME -> ParticleTypes.DUST_PLUME;
            case EGG_CRACK -> ParticleTypes.EGG_CRACK;
            case ELECTRIC_SPARK -> ParticleTypes.ELECTRIC_SPARK;
            case ELDER_GUARDIAN -> ParticleTypes.ELDER_GUARDIAN;
            case ENCHANT -> ParticleTypes.ENCHANT;
            case ENCHANTED_HIT -> ParticleTypes.ENCHANTED_HIT;
            case END_ROD -> ParticleTypes.END_ROD;

            case EXPLOSION -> ParticleTypes.EXPLOSION;
            case EXPLOSION_EMITTER -> ParticleTypes.EXPLOSION_EMITTER;

            case FALLING_DRIPSTONE_LAVA -> ParticleTypes.FALLING_DRIPSTONE_LAVA;
            case FALLING_DRIPSTONE_WATER -> ParticleTypes.FALLING_DRIPSTONE_WATER;
            case FALLING_HONEY -> ParticleTypes.FALLING_HONEY;
            case FALLING_LAVA -> ParticleTypes.FALLING_LAVA;
            case FALLING_NECTAR -> ParticleTypes.FALLING_NECTAR;
            case FALLING_OBSIDIAN_TEAR -> ParticleTypes.FALLING_OBSIDIAN_TEAR;
            case FALLING_SPORE_BLOSSOM -> ParticleTypes.FALLING_SPORE_BLOSSOM;
            case FALLING_WATER -> ParticleTypes.FALLING_WATER;

            case FIREFLY -> ParticleTypes.FIREFLY;
            case FIREWORK -> ParticleTypes.FIREWORK;
            case FISHING -> ParticleTypes.FISHING;
            case FLAME -> ParticleTypes.FLAME;

            case GLOW -> ParticleTypes.GLOW;
            case GLOW_SQUID_INK -> ParticleTypes.GLOW_SQUID_INK;

            case GUST -> ParticleTypes.GUST;
            case GUST_EMITTER_LARGE -> ParticleTypes.GUST_EMITTER_LARGE;
            case GUST_EMITTER_SMALL -> ParticleTypes.GUST_EMITTER_SMALL;
            case HAPPY_VILLAGER -> ParticleTypes.HAPPY_VILLAGER;
            case HEART -> ParticleTypes.HEART;
            case INFESTED -> ParticleTypes.INFESTED;

            case ITEM_COBWEB -> ParticleTypes.ITEM_COBWEB;
            case ITEM_SLIME -> ParticleTypes.ITEM_SLIME;
            case ITEM_SNOWBALL -> ParticleTypes.ITEM_SNOWBALL;

            case LANDING_HONEY -> ParticleTypes.LANDING_HONEY;
            case LANDING_LAVA -> ParticleTypes.LANDING_LAVA;
            case LANDING_OBSIDIAN_TEAR -> ParticleTypes.LANDING_OBSIDIAN_TEAR;

            case LARGE_SMOKE -> ParticleTypes.LARGE_SMOKE;
            case LAVA -> ParticleTypes.LAVA;
            case MYCELIUM -> ParticleTypes.MYCELIUM;
            case NAUTILUS -> ParticleTypes.NAUTILUS;
            case NOTE -> ParticleTypes.NOTE;

            case OMINOUS_SPAWNING -> ParticleTypes.OMINOUS_SPAWNING;
            case PALE_OAK_LEAVES -> ParticleTypes.PALE_OAK_LEAVES;

            case POOF -> ParticleTypes.POOF;
            case PORTAL -> ParticleTypes.PORTAL;
            case RAIN -> ParticleTypes.RAIN;
            case RAID_OMEN -> ParticleTypes.RAID_OMEN;
            case REVERSE_PORTAL -> ParticleTypes.REVERSE_PORTAL;

            case SCULK_CHARGE_POP -> ParticleTypes.SCULK_CHARGE_POP;
            case SCULK_SOUL -> ParticleTypes.SCULK_SOUL;
            case SCRAPE -> ParticleTypes.SCRAPE;

            case SMALL_FLAME -> ParticleTypes.SMALL_FLAME;
            case SMALL_GUST -> ParticleTypes.SMALL_GUST;

            case SMOKE -> ParticleTypes.SMOKE;
            case SNEEZE -> ParticleTypes.SNEEZE;
            case SNOWFLAKE -> ParticleTypes.SNOWFLAKE;
            case SONIC_BOOM -> ParticleTypes.SONIC_BOOM;

            case SOUL -> ParticleTypes.SOUL;
            case SOUL_FIRE_FLAME -> ParticleTypes.SOUL_FIRE_FLAME;
            case SPORE_BLOSSOM_AIR -> ParticleTypes.SPORE_BLOSSOM_AIR;

            case SPLASH -> ParticleTypes.SPLASH;
            case SPIT -> ParticleTypes.SPIT;
            case SQUID_INK -> ParticleTypes.SQUID_INK;
            case SWEEP_ATTACK -> ParticleTypes.SWEEP_ATTACK;
            case TOTEM_OF_UNDYING -> ParticleTypes.TOTEM_OF_UNDYING;

            case TRIAL_OMEN -> ParticleTypes.TRIAL_OMEN;
            case TRIAL_SPAWNER_DETECTED_PLAYER -> ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER;
            case TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS -> ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS;

            case UNDERWATER -> ParticleTypes.UNDERWATER;
            case VAULT_CONNECTION -> ParticleTypes.VAULT_CONNECTION;

            case WARPED_SPORE -> ParticleTypes.WARPED_SPORE;
            case WAX_OFF -> ParticleTypes.WAX_OFF;
            case WAX_ON -> ParticleTypes.WAX_ON;

            case WHITE_ASH -> ParticleTypes.WHITE_ASH;
            case WHITE_SMOKE -> ParticleTypes.WHITE_SMOKE;
            case WITCH -> ParticleTypes.WITCH;
        };
    }
}
