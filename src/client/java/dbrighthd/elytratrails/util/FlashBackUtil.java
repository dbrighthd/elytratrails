package dbrighthd.elytratrails.util;

import dbrighthd.elytratrails.compat.flashback.FlashbackCompat;

import static dbrighthd.elytratrails.compat.ModStatuses.FLASHBACK_LOADED;

public final class FlashBackUtil {
    public static boolean isInReplay() {
        if (!FLASHBACK_LOADED) return false;
        return FlashbackCompat.isInReplay();
    }
}
