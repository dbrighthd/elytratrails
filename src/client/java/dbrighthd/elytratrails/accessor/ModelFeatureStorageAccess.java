package dbrighthd.elytratrails.accessor;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.List;
import java.util.Map;

public interface ModelFeatureStorageAccess {
    Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> elytratrails$getSolidModelSubmits();
    List<SubmitNodeStorage.TranslucentModelSubmit<?>> elytratrails$getTranslucentModelSubmits();
}