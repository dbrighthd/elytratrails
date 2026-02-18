package dbrighthd.elytratrails.compat.pride;

import io.github.queerbric.pride.PrideFlag;
import io.github.queerbric.pride.PrideFlags;
import io.github.queerbric.pride.shape.HorizontalPrideFlagShape;
import io.github.queerbric.pride.shape.PrideFlagShape;
import io.github.queerbric.pride.shape.VerticalPrideFlagShape;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;

//I know this doesnt need to be in the compat section, but It felt weird not putting it here
public final class PrideCompat {
    private PrideCompat() {}

    public static @Nullable PrideStripes getStripes(String flagId) {
        if (flagId == null) return null;
        String id = flagId.trim();
        if (id.isEmpty()) return null;

        PrideFlag flag = PrideFlags.getFlag(id);
        if (flag == null) return null;

        PrideFlagShape shape = flag.getShape();
        if (shape == null) return null;

        final IntList colors;
        if (shape instanceof HorizontalPrideFlagShape h) {
            colors = h.colors();
        } else if (shape instanceof VerticalPrideFlagShape v) {
            colors = v.colors();
        } else {
            return null;
        }

        if (colors == null || colors.isEmpty()) return null;

        int[] argb = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            argb[i] = colors.getInt(i);
        }
        return new PrideStripes(argb);
    }

    public record PrideStripes(int[] argbColors) {
        public boolean isEmpty() {
            return argbColors == null || argbColors.length == 0;
        }
    }
}
