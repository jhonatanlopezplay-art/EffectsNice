package xp.dev.effect;

public final class SphereGeometry {
    public static final float MIN_RADIUS = 2, MAX_RADIUS = 128, FADE_TICKS = 20;
    private SphereGeometry() {}

    public static double contact(double x, double y, double z, double dx, double dy, double dz,
                                 double radius, double padding) {
        double distanceSquared = x*x + y*y + z*z;
        double inner = Math.max(0, radius - padding), outer = radius + padding;
        if (distanceSquared >= inner*inner && distanceSquared <= outer*outer) return 0;
        double a = dx*dx + dy*dy + dz*dz;
        if (a < 1e-16) return Double.NaN;
        boolean inside = distanceSquared < inner*inner;
        double surface = inside ? inner : outer;
        double b = x*dx + y*dy + z*dz;
        double discriminant = b*b - a*(distanceSquared - surface*surface);
        if (discriminant < 0) return Double.NaN;
        double t = (-b + (inside ? 1 : -1)*Math.sqrt(discriminant)) / a;
        return t >= 0 && t <= 1 ? t : Double.NaN;
    }
}
