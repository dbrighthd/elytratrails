package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.config.ModConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//in 1.20.1/1.21.1 the sampling happens in a kind of backwards way where the sampler is called to return the sampled wingtip from the render mixin
public class WingTipSampler {
    public Map<Integer, List<Emitter>> gatheredTrailsThisFrame = new HashMap<>();
    public Map<Integer, List<Emitter>> gatherdTrailsThisFrameSnapCache = new HashMap<>();

    public void clearFrameCache() {
        gatheredTrailsThisFrame.clear();
    }
    public void clearFrameSnapCache() {
        gatherdTrailsThisFrameSnapCache.clear();
    }

    public void insertWingTips(int eid, Vec3 positionLeft, Vec3 positionRight)
    {
        gatheredTrailsThisFrame.put(eid, List.of(new Emitter(positionLeft, true, "elytra", "left_wing", true),new Emitter(positionRight, false, "elytra", "right_wing", true)));
    }

    public List<Emitter> getPlayerTrailEmitterPositions(Player player, float partialTick, ModConfig modConfig)
    {
        if(gatheredTrailsThisFrame.containsKey(player.getId()))
        {
            List<Emitter> output = gatheredTrailsThisFrame.get(player.getId());
            gatherdTrailsThisFrameSnapCache.put(player.getId(),output);
            return output;
        }
        return List.of();
    }
}