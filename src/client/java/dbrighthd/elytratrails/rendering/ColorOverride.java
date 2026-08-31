package dbrighthd.elytratrails.rendering;

import java.util.function.Function;

public class ColorOverride {
    Function<Integer,Integer> colorSupplier;
    int entityId;
    int color;
    public ColorOverride(Function<Integer,Integer> colorSupplier, int entityId)
    {
        this.colorSupplier = colorSupplier;
        this.entityId = entityId;
    }

    public void setColor()
    {
        Integer colorInt = colorSupplier.apply(entityId);
        if(colorInt != null)
        {
            color = colorInt;
        }
    }
    public int getColor()
    {
        return color;
    }
}
