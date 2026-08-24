package dbrighthd.elytratrails.util;
/**
 * sets up easings for twirling. I got the equations from <a href="https://www.desmos.com/calculator/m8myals511">this desmos tool</a>
 */
public class EasingUtil {
    public enum EaseType {
        Back,
        @SuppressWarnings("unused") Sine, // it thinks its unused because it's not in the code (it's a fallback) but it can be selected by the player
        Cubic,
        Expo,
        Elastic,
        Random,
        None,
    }
}
