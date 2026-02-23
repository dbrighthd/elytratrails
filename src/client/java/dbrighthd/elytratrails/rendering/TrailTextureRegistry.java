package dbrighthd.elytratrails.rendering;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolves custom trail textures from:
 *   assets/elytratrails/textures/trails/*.png
 * Users can specify a string like "genderfluid" (case-insensitive) and it maps to:
 *   elytratrails:textures/trails/genderfluid.png
 * If the texture doesn't exist, resolveTextureOrNull returns null so the caller can
 * fall back to the default TRAIL_TEX.
 */
public final class TrailTextureRegistry {
    private TrailTextureRegistry() {}

    private static volatile Set<String> AVAILABLE = Collections.emptySet();

    /**
     * Synchronously reload available trail textures from the current ResourceManager.
     * Intended to be called from your existing SimpleSynchronousResourceReloadListener.
     * Scans:
     *   assets/elytratrails/textures/trails/*.png
     */
    public static void reloadNow(ResourceManager manager) {
        Map<Identifier, ?> found = manager.listResources("textures/trails", id -> id.getPath().endsWith(".png"));

        Set<String> out = new HashSet<>();
        for (Identifier id : found.keySet()) {
            // Only our namespace
            if (!"elytratrails".equals(id.getNamespace())) continue;

            String path = id.getPath(); // textures/trails/<name>.png
            if (!path.startsWith("textures/trails/") || !path.endsWith(".png")) continue;

            String name = path.substring("textures/trails/".length(), path.length() - ".png".length());
            name = normalizeName(name);
            if (!name.isEmpty()) out.add(name);
        }

        AVAILABLE = Collections.unmodifiableSet(out);
    }

    /**
     * Returns a texture Identifier if present in assets/elytratrails/textures/trails/,
     * otherwise returns null (caller should use default trail texture).
     * Case-insensitive.
     * Accepts:
     *  - "genderfluid"
     *  - "ELYTRATRAILS:genderfluid" (namespace ignored; path used)
     */
    public static Identifier resolveTextureOrNull(String trailTextureName) {
        String name = normalizeFromUserString(trailTextureName);
        if (name == null) return null;

        if (!AVAILABLE.contains(name)) return null;

        return Identifier.parse("elytratrails:textures/trails/" + name + ".png");
    }

    private static String normalizeFromUserString(String in) {
        if (in == null) return null;
        String s = in.trim();
        if (s.isEmpty()) return null;

        // Allow "namespace:path" but ignore namespace (use path)
        if (s.contains(":")) {
            Identifier id = Identifier.tryParse(s);
            if (id == null) return null;
            s = id.getPath();
        }

        s = normalizeName(s);
        return s.isEmpty() ? null : s;
    }

    static String normalizeName(String s) {
        String name = s.toLowerCase();
        name = name.replaceAll("[^a-z0-9_\\-./]", "");
        name = name.replace("..", "");
        name = name.replace("\\", "/");
        while (name.startsWith("/")) name = name.substring(1);
        while (name.endsWith("/")) name = name.substring(0, name.length() - 1);
        return name;
    }
}
