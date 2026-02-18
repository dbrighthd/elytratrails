package dbrighthd.elytratrails.mixin.client;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(targets = "net.minecraft.client.renderer.feature.ModelFeatureRenderer$Storage")
public interface ModelFeatureStorageAccessor {
    @Accessor("opaqueModelSubmits")
    Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> elytratrails$getOpaqueModelSubmits();

    @Accessor("translucentModelSubmits")
    List<SubmitNodeStorage.TranslucentModelSubmit<?>> elytratrails$getTranslucentModelSubmits();
}
