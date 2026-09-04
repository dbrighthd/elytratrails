package dbrighthd.elytratrails.api;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;


import static dbrighthd.elytratrails.api.ElytraTrailsAPI.setConditionalColorOverrides;

public class APIExample {

    /**
     * "default" color override function. right now only for exp orbs, will expand int the future
     * @param eid entity id
     */
    public static Integer expOrbColorOverride(int eid) {
        Level level = Minecraft.getInstance().level;
        if(level == null)
        {
            return null;
        }
        Entity e = level.getEntity(eid);
        if (e == null)
        {
            return null;
        }
        float rr = e.tickCount/2.0F;
        int rc = (int)((Mth.sin(rr + 0.0F) + 1.0F) * 0.5F * 255.0F);
        int gc = 255;
        int bc = (int)((Mth.sin(rr + (float) (Math.PI * 4.0 / 3.0)) + 1.0F) * 0.1F * 255.0F);
        return (255 << 24) | (rc << 16) | (gc << 8) | bc;
    }


    public static void RegisterExampleAPIHook()
    {
        setConditionalColorOverrides((entity) -> entity instanceof ExperienceOrb, APIExample::expOrbColorOverride);
    }
}
