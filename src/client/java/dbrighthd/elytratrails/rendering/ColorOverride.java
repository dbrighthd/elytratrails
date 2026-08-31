package dbrighthd.elytratrails.rendering;

import java.util.function.Function;

/**
 *  I want to expand this system eventually, but now it is just used for the exp color.
 *  Once I make a trail API, mods will be able to put their own function in the supplier (right now only the default one is used, and its just used for exp orbs)
 */
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
