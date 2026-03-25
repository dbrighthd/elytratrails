package dbrighthd.elytratrails.compat.emf;

import net.minecraft.client.model.geom.ModelPart;

import java.util.*;

public final class EmfWingTipHooks {
    private EmfWingTipHooks() {
    }

    public enum WhichRoot {LEFT_WING, RIGHT_WING}

    public record SpawnerPath(WhichRoot where, String path, String key) {
    }


    public static List<SpawnerPath> findAllSpawnerPaths(ModelPart leftWingRoot, ModelPart rightWingRoot, boolean FAPlayerCheck) {
        ArrayList<SpawnerPath> out = new ArrayList<>();
        if (leftWingRoot != null) out.addAll(findSpawnerPaths(leftWingRoot, WhichRoot.LEFT_WING, FAPlayerCheck));
        if (rightWingRoot != null) out.addAll(findSpawnerPaths(rightWingRoot, WhichRoot.RIGHT_WING, FAPlayerCheck));

        out.sort(Comparator
                .comparing((SpawnerPath p) -> p.where().ordinal())
                .thenComparing(SpawnerPath::path)
                .thenComparing(SpawnerPath::key));

        return out;
    }

    public static List<SpawnerPath> findAllSpawnerPathsGeneric(ModelPart modelRoot, boolean FAPlayerCheck) {
        ArrayList<SpawnerPath> out = new ArrayList<>();
        if (modelRoot != null) out.addAll(findSpawnerPaths(modelRoot, WhichRoot.LEFT_WING, FAPlayerCheck));

        out.sort(Comparator
                .comparing((SpawnerPath p) -> p.where().ordinal())
                .thenComparing(SpawnerPath::path)
                .thenComparing(SpawnerPath::key));

        return out;
    }

    private static List<SpawnerPath> findSpawnerPaths(ModelPart root, WhichRoot where, boolean FAPlayerCheck) {
        ArrayList<SpawnerPath> found = new ArrayList<>();
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(new Node(root, ""));

        while (!stack.isEmpty()) {
            Node n = stack.pop();
            Map<String, ModelPart> kids = n.part.children;

            for (var e : kids.entrySet()) {
                String key = e.getKey();
                ModelPart child = e.getValue();

                String childPath = n.path.isEmpty() ? key : (n.path + "/" + key);

                if (matchesKeyword(key, FAPlayerCheck)) {
                    found.add(new SpawnerPath(where, childPath, key));
                }

                stack.push(new Node(child, childPath));
            }
        }
        return found;
    }

    private static boolean matchesKeyword(String key, boolean FAPlayerCheck) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return lower.contains("wingtip") || lower.contains("trailspawner") || (FAPlayerCheck && (lower.contains("left_wing2") || lower.contains("right_wing2")));
    }

    private record Found(WhichRoot where, String path, String key, ModelPart part) {
    }


    @SuppressWarnings("unused")
    private static Found findFirstByName(ModelPart root, String[] aliases, WhichRoot where) {
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(new Node(root, ""));

        while (!stack.isEmpty()) {
            Node n = stack.pop();
            Map<String, ModelPart> kids = n.part.children;

            for (var e : kids.entrySet()) {
                String key = e.getKey();
                ModelPart child = e.getValue();

                String childPath = n.path.isEmpty() ? key : (n.path + "/" + key);

                if (matches(key, aliases)) {
                    return new Found(where, childPath, key, child);
                }

                stack.push(new Node(child, childPath));
            }
        }
        return null;
    }

    private static boolean matches(String name, String[] aliases) {
        for (String a : aliases) {
            if (name.equals(a) || name.equalsIgnoreCase(a)) return true;
        }
        return false;
    }

    private record Node(ModelPart part, String path) {
    }
}
