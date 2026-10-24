package dbrighthd.elytratrails.compat.recolor;

import bluebird.recolor.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class RecolorCompat {
    public static Integer expOrbColorOverrideRecolor(int eid) {
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
        //snippet from recolor's xp color handling
        float t = (Mth.sin((e.tickCount / 2.0F)) + 1.0F) * 0.5F;
        int xp1 = Colors.xpOrb1;
        int xp2 = Colors.xpOrb2;
        int rc = (int)Mth.lerp(t, (float)(xp1 >> 16 & 255), (float)(xp2 >> 16 & 255));
        int gc = (int)Mth.lerp(t, (float)(xp1 >> 8 & 255), (float)(xp2 >> 8 & 255));
        int bc = (int)Mth.lerp(t, (float)(xp1 & 255), (float)(xp2 & 255));
        return (255 << 24) | (rc << 16) | (gc << 8) | bc;

    }
}
