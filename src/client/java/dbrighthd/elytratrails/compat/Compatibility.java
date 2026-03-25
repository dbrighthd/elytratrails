package dbrighthd.elytratrails.compat;

public class Compatibility {
    public static void init() {
        if (ModStatuses.IRIS_LOADED) IrisCompat.registerPipelines();
    }
}
