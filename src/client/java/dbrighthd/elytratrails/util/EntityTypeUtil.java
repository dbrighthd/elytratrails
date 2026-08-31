package dbrighthd.elytratrails.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class EntityTypeUtil {
    public static Optional<EntityType<?>> parseEntityString(String id)
    {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(id));
    }

    public static Integer expColor(int eid) {
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
}
