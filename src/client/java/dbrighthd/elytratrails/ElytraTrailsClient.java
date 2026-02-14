package dbrighthd.elytratrails;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.InputConstants;
import dbrighthd.elytratrails.config.ModConfig;
import dbrighthd.elytratrails.trailrendering.TrailRenderer;
import dbrighthd.elytratrails.trailrendering.TrailStore;
import dbrighthd.elytratrails.trailrendering.WingTipPos;
import dbrighthd.elytratrails.util.ShaderChecksUtil;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiFunction;

import static dbrighthd.elytratrails.trailrendering.TrailRenderer.TRAIL_TEX;

public class ElytraTrailsClient implements ClientModInitializer {
	private final TrailRenderer trailRendererCorrectLeft = new TrailRenderer(TrailStore.LEFT);
	private final TrailRenderer trailRendererCorrectRight = new TrailRenderer(TrailStore.RIGHT);
	private static final double MIN_SPEED = 0.10;
	private static final Int2LongOpenHashMap lastSampleTimeByEntity = new Int2LongOpenHashMap();

	private static final Int2BooleanOpenHashMap wasFallFlyingRaw = new Int2BooleanOpenHashMap();
	private static final Int2LongOpenHashMap fallFlyingStartTimeByEntity = new Int2LongOpenHashMap();
	private static final long FALLFLYING_WARMUP_NANOS = 250_000_000L; // 0.5 seconds
	private static final Int2BooleanOpenHashMap wasTrailShowing = new Int2BooleanOpenHashMap();

	@Override
	public void onInitializeClient() {
		ElytraTrailsKeybind.init();
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		RenderPipelines.register(PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT);
		WorldRenderEvents.END_MAIN.register(worldRenderContext -> {
			RenderType elytraTrailRenderType = entityTranslucentEmissiveUnlit(TRAIL_TEX);
			if(ShaderChecksUtil.isUsingShaders())
			{
				elytraTrailRenderType = RenderTypes.entityTranslucentEmissive(TRAIL_TEX);
			}
			worldRenderContext.commandQueue().order(1)
					.submitCustomGeometry(worldRenderContext.matrices(),
							elytraTrailRenderType,
							trailRendererCorrectLeft);
			worldRenderContext.commandQueue().order(1)
					.submitCustomGeometry(worldRenderContext.matrices(),
							elytraTrailRenderType,
							trailRendererCorrectRight);
		});
		WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) return;
			if (!getConfig().enableTrail) return;

			long now = Util.getNanos();

			WingTipPos.consumeAll((entityId, left, right, capturedAtNanos) -> {
				var entity = mc.level.getEntity(entityId);
				if (!(entity instanceof Player living)) return;

				boolean flyingNowRaw = living.isFallFlying();
				boolean flyingBeforeRaw = wasFallFlyingRaw.getOrDefault(entityId, false);

				if (flyingNowRaw && !flyingBeforeRaw) {
					//just started fallflying
					fallFlyingStartTimeByEntity.put(entityId, now);
				} else if (!flyingNowRaw && flyingBeforeRaw) {
					//stopped fallflying
					fallFlyingStartTimeByEntity.remove(entityId);
				}
				wasFallFlyingRaw.put(entityId, flyingNowRaw);

				boolean showingNow = showTrail(living, now);
				boolean showingBefore = wasTrailShowing.getOrDefault(entityId, false);


				if (showingNow && !showingBefore) {
					TrailStore.breakTrail(entityId, now);
				}

				wasTrailShowing.put(entityId, showingNow);

				if (!showingNow) return;

				long last = lastSampleTimeByEntity.getOrDefault(entityId, Long.MIN_VALUE);
				if (last != Long.MIN_VALUE && now - last < 1_000_000_000L / getConfig().maxSamplePerSecond) return;
				lastSampleTimeByEntity.put(entityId, now);

				TrailStore.add(entityId, left, right, now);
			});

			TrailStore.cleanup(now);
		});

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

	public static final RenderPipeline PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
			.withLocation(Identifier.parse("elytratrails:pipeline/entity_translucent_emissive_unlit"))
			.withShaderDefine("ALPHA_CUTOUT", 0.1F)
			.withShaderDefine("NO_CARDINAL_LIGHTING")
			.withSampler("Sampler1")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withCull(false)
			.withDepthWrite(false)
			.build();

	private static final BiFunction<Identifier, Boolean, RenderType> RENDER_TYPE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT = Util.memoize(
			(identifier, boolean_) -> {
				RenderSetup renderSetup = RenderSetup.builder(PIPELINE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT)
						.withTexture("Sampler0", identifier)
						.useOverlay()
						.affectsCrumbling()
						.sortOnUpload()
						.setOutline(boolean_ ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
						.createRenderSetup();
				return RenderType.create("entity_translucent_emissive_unlit", renderSetup);
			}
	);

	public static RenderType entityTranslucentEmissiveUnlit(Identifier texture) {
		return RENDER_TYPE_ENTITY_TRANSLUCENT_EMISSIVE_UNLIT.apply(texture, false);
	}

	public static ModConfig getConfig() {
		return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}
	public static void setConfig(ModConfig modConfig) {
		AutoConfig.getConfigHolder(ModConfig.class).setConfig(modConfig);
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

	private static boolean showTrail(Player living, long nowNanos) {
		var cfg = getConfig();
		if (!living.isFallFlying()) return false;

		if (cfg.speedDependentTrail) {
			long start = fallFlyingStartTimeByEntity.getOrDefault(living.getId(), Long.MIN_VALUE);
			if (start == Long.MIN_VALUE) {
				fallFlyingStartTimeByEntity.put(living.getId(), nowNanos);
				return false;
			}

			if (nowNanos - start < FALLFLYING_WARMUP_NANOS) return false;

			if (cfg.trailMinSpeed >= 0.001) {
				return living.getKnownSpeed().length() > cfg.trailMinSpeed;
			}
			return true;
		}

		return true;
	}
}
