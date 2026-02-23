package dbrighthd.elytratrails.compat.emf;

import net.minecraft.client.model.geom.ModelPart;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class EmfWingTipHooks {
    private EmfWingTipHooks() {}

    public enum WhichRoot { LEFT_WING, RIGHT_WING }

    public record SpawnerPath(WhichRoot where, String path, String key) {}


    public static List<SpawnerPath> findAllSpawnerPaths(ModelPart leftWingRoot, ModelPart rightWingRoot) {
        ArrayList<SpawnerPath> out = new ArrayList<>();
        if (leftWingRoot != null) out.addAll(findSpawnerPaths(leftWingRoot, WhichRoot.LEFT_WING));
        if (rightWingRoot != null) out.addAll(findSpawnerPaths(rightWingRoot, WhichRoot.RIGHT_WING));

        out.sort(Comparator
                .comparing((SpawnerPath p) -> p.where().ordinal())
                .thenComparing(SpawnerPath::path)
                .thenComparing(SpawnerPath::key));

        return out;
    }

    private static List<SpawnerPath> findSpawnerPaths(ModelPart root, WhichRoot where) {
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

                if (matchesKeyword(key)) {
                    found.add(new SpawnerPath(where, childPath, key));
                }

                stack.push(new Node(child, childPath));
            }
        }
        return found;
    }

    private static boolean matchesKeyword(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return lower.contains("wingtip") || lower.contains("trailspawner");
    }

    private record Found(WhichRoot where, String path, String key, ModelPart part) {}


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

    private record Node(ModelPart part, String path) {}
}
