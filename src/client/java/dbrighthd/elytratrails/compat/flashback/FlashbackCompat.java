package dbrighthd.elytratrails.compat.flashback;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.playback.ReplayServer;


public final class FlashbackCompat {
    private FlashbackCompat() {}

    public static boolean isReplayPaused() {
        ReplayServer replayServer = Flashback.getReplayServer();
        return replayServer != null && replayServer.replayPaused;
    }

    @SuppressWarnings("unused")
    public static boolean isInReplay() {
        return Flashback.getReplayServer() != null;
    }
}