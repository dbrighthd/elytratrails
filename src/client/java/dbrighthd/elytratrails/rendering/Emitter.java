package dbrighthd.elytratrails.rendering;

import dbrighthd.elytratrails.config.pack.TrailPackConfigManager;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record Emitter(Vec3 position, boolean flipUv, @Nullable String modelName, @Nullable String boneName) {
}
