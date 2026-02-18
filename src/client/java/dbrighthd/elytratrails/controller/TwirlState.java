package dbrighthd.elytratrails.controller;

public enum TwirlState {
    OFF(0),

    // One-shot 360 (ease-in/out over DURATION)
    NORMAL(1),

    // Continuous phases (start / middle / ending)
    CONTINUOUS_BEGIN(2),
    CONTINUOUS_MIDDLE(3),
    CONTINUOUS_END(4);

    public final int id;

    TwirlState(int id) {
        this.id = id;
    }

    public static TwirlState fromId(int id) {
        int a = Math.abs(id);
        for (TwirlState s : values()) {
            if (s.id == a) return s;
        }
        return OFF;
    }

    public static int dirFromId(int id) {
        if (id == 0) return 1;
        return id < 0 ? -1 : 1;
    }
}
