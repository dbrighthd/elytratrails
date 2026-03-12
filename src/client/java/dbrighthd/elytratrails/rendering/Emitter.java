package dbrighthd.elytratrails.rendering;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record Emitter(Vec3 position, boolean isLeftWing, @Nullable String modelName, @Nullable String boneName, boolean visible) {
}
