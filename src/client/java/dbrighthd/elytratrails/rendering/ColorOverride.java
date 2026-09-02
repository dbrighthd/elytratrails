package dbrighthd.elytratrails.rendering;

import java.util.function.Function;

/**
 *  I want to expand this system eventually, but now it is just used for the exp color.
 *   Mods can add their own with the API, but I want to make it so that there are more built in ones that players can use
 *
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
