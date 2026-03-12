package dbrighthd.elytratrails.rendering;

import java.util.List;

public record EntityTrailGroup(List<Trail> elytraTrails, List<Trail> genericTrails) {

    public boolean containsTrail(Trail trail) {
        return elytraTrails.contains(trail) || genericTrails.contains(trail);
    }
}
