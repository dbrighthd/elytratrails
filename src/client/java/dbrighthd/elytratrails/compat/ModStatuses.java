package dbrighthd.elytratrails.compat;

import net.fabricmc.loader.api.FabricLoader;

public class ModStatuses {
    public static final boolean EMF_LOADED = FabricLoader.getInstance().isModLoaded("entity_model_features");
    public static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");
}
