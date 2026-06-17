package dbrighthd.elytratrails.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import javax.swing.text.html.parser.Entity;
import java.util.Optional;

public class EntityTypeUtil {
    public static Optional<EntityType<?>> parseEntityString(String id)
    {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(id));
    }
}
