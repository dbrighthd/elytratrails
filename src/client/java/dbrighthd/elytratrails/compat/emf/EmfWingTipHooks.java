// EmfWingTipHooks.java
package dbrighthd.elytratrails.compat.emf;

import net.minecraft.client.model.geom.ModelPart;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public final class EmfWingTipHooks {
    private EmfWingTipHooks() {}

    public enum WhichRoot { LEFT_WING, RIGHT_WING }

    /** Paths are now CHILD-ONLY, e.g. "EMF_left_wing/EMF_bone/EMF_leftWingTip" (no "root/"). */
    public record TipPaths(WhichRoot leftRoot, String leftPath, String leftKey,
                           WhichRoot rightRoot, String rightPath, String rightKey) {}

    private static final String[] LEFT_ALIASES = {
            "leftWingTip", "left_wing_tip", "elytratrails:leftWingTip", "elytratrails:left_wing_tip",
            "EMF_leftWingTip", "EMF_left_wing_tip", "EMF_elytratrails:leftWingTip", "EMF_elytratrails:left_wing_tip"
    };
    private static final String[] RIGHT_ALIASES = {
            "rightWingTip", "right_wing_tip", "elytratrails:rightWingTip", "elytratrails:right_wing_tip",
            "EMF_rightWingTip", "EMF_right_wing_tip", "EMF_elytratrails:rightWingTip", "EMF_elytratrails:right_wing_tip"
    };

    public static TipPaths findTipPaths(ModelPart leftWingRoot, ModelPart rightWingRoot) {
        if (leftWingRoot == null || rightWingRoot == null) return null;

        Found left = findFirstByNameInEitherTree(leftWingRoot, rightWingRoot, LEFT_ALIASES);
        Found right = findFirstByNameInEitherTree(leftWingRoot, rightWingRoot, RIGHT_ALIASES);

        if (left == null || right == null) return null;

        return new TipPaths(left.where, left.path, left.key, right.where, right.path, right.key);
    }

    private record Found(WhichRoot where, String path, String key, ModelPart part) {}

    private static Found findFirstByNameInEitherTree(ModelPart a, ModelPart b, String[] aliases) {
        Found inA = findFirstByName(a, aliases, WhichRoot.LEFT_WING);
        if (inA != null) return inA;
        return findFirstByName(b, aliases, WhichRoot.RIGHT_WING);
    }

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
