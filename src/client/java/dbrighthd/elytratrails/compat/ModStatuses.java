package dbrighthd.elytratrails.compat;

import net.fabricmc.loader.api.FabricLoader;

public class ModStatuses {
    public static final boolean EMF_LOADED = FabricLoader.getInstance().isModLoaded("entity_model_features");
    public static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");
    public static final boolean FLASHBACK_LOADED = FabricLoader.getInstance().isModLoaded("flashback");
    public static final boolean CLOTH_LOADED = FabricLoader.getInstance().isModLoaded("cloth-config");
    public static final boolean CPM_LOADED = FabricLoader.getInstance().isModLoaded("cpm");;
}
