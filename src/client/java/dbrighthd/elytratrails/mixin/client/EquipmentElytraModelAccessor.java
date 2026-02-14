package dbrighthd.elytratrails.mixin.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.equipment.ElytraModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ElytraModel.class)
public interface EquipmentElytraModelAccessor {
	@Accessor("leftWing")
	ModelPart elytratrails$getLeftWing();

	@Accessor("rightWing")
	ModelPart elytratrails$getRightWing();
}
