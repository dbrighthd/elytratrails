package dbrighthd.elytratrails.handler;

import dbrighthd.elytratrails.rendering.TrailSystem;
import dbrighthd.elytratrails.util.ElytraTimeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static dbrighthd.elytratrails.config.ConfigManager.getConfig;


public class ArrowHandler {
    public record ArrowData(Vec3 knownPos, float xRot, float yRot, Vec3 nextPosGuess, long tickAdded, Vec3 knownDeltaMovement){}


    public static List<ArrowAndTick> fakeArrows = new ArrayList<>();

    //for those new to the stream, "calc" is slang for "calculate"
    public static void addAndCalcFakeArrow(Arrow arrow, ClientLevel level) {
        if(!getConfig().arrowOvershoot)
        {
            return;
        }
        ArrowData arrowData = new ArrowData(arrow.position(), arrow.getXRot(), arrow.getYRot(), arrow.position().add(arrow.getDeltaMovement()), ElytraTimeUtil.currentTick(), arrow.getDeltaMovement());
        Arrow fakeArrow = EntityTypes.ARROW.create(level, EntitySpawnReason.COMMAND);
        fakeArrow.setXRot(arrowData.xRot());
        fakeArrow.setYRot(arrowData.yRot());
        fakeArrow.setId(arrow.getId());
        fakeArrow.tickCount = 100;
        fakeArrow.xo = arrowData.knownPos().x;
        fakeArrow.yo = arrowData.knownPos().y;
        fakeArrow.zo = arrowData.knownPos().z;
        fakeArrow.xOld = arrowData.knownPos().x;
        fakeArrow.yOld = arrowData.knownPos().y;
        fakeArrow.zOld = arrowData.knownPos().z;
        fakeArrow.setOldRot();
        fakeArrow.setPos(arrowData.nextPosGuess);
        fakeArrow.setDeltaMovement(arrowData.knownDeltaMovement());
        fakeArrows.add(new ArrowAndTick(fakeArrow,ElytraTimeUtil.currentTick(),Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)));
    }

    public static void stepFakeArrows(ClientLevel level)
    {
        if(!getConfig().arrowOvershoot)
        {
            fakeArrows.clear();
        }
        for(ArrowAndTick arrowAndTick : fakeArrows)
        {
            arrowAndTick.ticksLasted++;
            arrowAndTick.lastPartialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Arrow fakeArrow = arrowAndTick.arrow;
            fakeArrow.xo = fakeArrow.getX();
            fakeArrow.yo = fakeArrow.getY();
            fakeArrow.zo = fakeArrow.getZ();
            fakeArrow.xOld = fakeArrow.getX();
            fakeArrow.yOld = fakeArrow.getY();
            fakeArrow.zOld = fakeArrow.getZ();
            fakeArrow.tick();
        }
    }
    public static Vec3 positionGuess(ArrowData arrowData,float partialTick)
    {
        return Mth.lerp(partialTick,arrowData.knownPos(),arrowData.nextPosGuess());
    }

    public static boolean isArrowInEntity(Arrow fakeArrowOriginal, ClientLevel level, float partialTick)
    {
        Arrow fakeArrow = EntityTypes.ARROW.create(level, EntitySpawnReason.COMMAND);
        fakeArrow.setPos(fakeArrowOriginal.getPosition(partialTick));
        List<Entity> entitiesThatSupposedlyAreColliding = level.getEntities(fakeArrow,fakeArrow.getBoundingBox());
        return !entitiesThatSupposedlyAreColliding.isEmpty();
    }
    public static List<Entity> getFakeArrows(ClientLevel level, float partialTick)
    {
        List<Entity> allArrows = new ArrayList<>();
        List<ArrowAndTick> removals = new ArrayList<>();
        for(ArrowAndTick arrowAndTick : fakeArrows)
        {
            if(arrowAndTick.ticksLasted > 1)
            {
                removals.add(arrowAndTick);
                TrailSystem.getTrailManager().stopTrail(arrowAndTick.arrow.getId());
            }
            else if(!(partialTick < arrowAndTick.lastPartialTick))
            {
                allArrows.add(arrowAndTick.arrow);
                arrowAndTick.lastPartialTick = partialTick;
            }
        }

        fakeArrows.removeIf(removals::contains);
        return allArrows;

    }

    public static class ArrowAndTick
    {
        Arrow arrow;
        long tick;
        public float lastPartialTick;
        public int ticksLasted;
        public long timeAdded;

        public ArrowAndTick(Arrow arrow, long tick, float lastPartialTick)
        {
            this.arrow = arrow;
            this.tick = tick;
            this.lastPartialTick = lastPartialTick;
            ticksLasted = 0;
            timeAdded = ElytraTimeUtil.currentMillis();

        }
    }
}
