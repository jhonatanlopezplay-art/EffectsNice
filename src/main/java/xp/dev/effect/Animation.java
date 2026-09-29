package xp.dev.effect;

public final class Animation {
    public static final float CONVERT_TICKS = 20f;
    public static final float SKY_TICKS = 160f;
    public static final float FRACTURE_TICKS = 60f;
    public static final float OPEN_TICKS = 50f;
    public static final float RIFT_TICKS = FRACTURE_TICKS + OPEN_TICKS;
    private float value;
    private long changedAt;
    private boolean active;
    public float at(long tick, float duration) {
        return sample(value, active, tick - changedAt, duration);
    }
    public void target(boolean active, long tick, float duration) {
        value = at(tick, duration);
        changedAt = tick;
        this.active = active;
    }
    public boolean active() { return active; }
    public static float sample(float value, boolean active, double elapsed, float duration) {
        return Math.clamp(value + (float)Math.max(0, elapsed) / duration * (active ? 1 : -1), 0, 1);
    }
    public static float fracture(float phase) { return Math.clamp(phase * RIFT_TICKS / FRACTURE_TICKS, 0, 1); }
    public static float opening(float phase) { return smooth(Math.clamp((phase * RIFT_TICKS - FRACTURE_TICKS) / OPEN_TICKS, 0, 1)); }
    public static float smooth(float x) { return x * x * (3 - 2 * x); }
}
