package xp.dev.effect;

import java.util.*;

public final class SphereMesh {
    public record Point(double x, double y, double z) {
        public Point add(Point p) { return new Point(x+p.x, y+p.y, z+p.z); }
        public Point subtract(Point p) { return new Point(x-p.x, y-p.y, z-p.z); }
        public Point scale(double n) { return new Point(x*n, y*n, z*n); }
        public double dot(Point p) { return x*p.x + y*p.y + z*p.z; }
        public Point cross(Point p) { return new Point(y*p.z-z*p.y, z*p.x-x*p.z, x*p.y-y*p.x); }
        public double length() { return Math.sqrt(dot(this)); }
        public Point unit() { return scale(1 / length()); }
    }
    public record Cell(Point center, List<Point> corners) {}
    private SphereMesh() {}

    public static List<Cell> create(int subdivisions) {
        if (subdivisions < 1 || subdivisions > 4) throw new IllegalArgumentException("Sphere detail must be 1–4");
        double t = (1 + Math.sqrt(5)) / 2;
        List<Point> points = new ArrayList<>();
        for (double[] p : new double[][]{{-1,t,0},{1,t,0},{-1,-t,0},{1,-t,0},
                {0,-1,t},{0,1,t},{0,-1,-t},{0,1,-t},{t,0,-1},{t,0,1},{-t,0,-1},{-t,0,1}})
            points.add(new Point(p[0],p[1],p[2]).unit());
        List<int[]> triangles = new ArrayList<>(List.of(new int[][]{
                {0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},
                {1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},
                {3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},
                {4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}}));
        for (int level = 0; level < subdivisions; level++) {
            Map<Long, Integer> midpoints = new HashMap<>();
            List<int[]> next = new ArrayList<>();
            for (int[] f : triangles) {
                int a = midpoint(points, midpoints, f[0], f[1]);
                int b = midpoint(points, midpoints, f[1], f[2]);
                int c = midpoint(points, midpoints, f[2], f[0]);
                next.add(new int[]{f[0],a,c}); next.add(new int[]{f[1],b,a});
                next.add(new int[]{f[2],c,b}); next.add(new int[]{a,b,c});
            }
            triangles = next;
        }
        List<List<Point>> adjacent = new ArrayList<>();
        for (Point ignored : points) adjacent.add(new ArrayList<>());
        for (int[] f : triangles) {
            Point center = points.get(f[0]).add(points.get(f[1])).add(points.get(f[2])).unit();
            for (int vertex : f) adjacent.get(vertex).add(center);
        }
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Point normal = points.get(i);
            Point axis = Math.abs(normal.y) < .9 ? new Point(0,1,0) : new Point(1,0,0);
            Point right = normal.cross(axis).unit(), up = normal.cross(right);
            List<Point> corners = adjacent.get(i);
            corners.sort(Comparator.comparingDouble(p -> Math.atan2(p.dot(up), p.dot(right))));
            cells.add(new Cell(normal, List.copyOf(corners)));
        }
        return List.copyOf(cells);
    }

    private static int midpoint(List<Point> points, Map<Long, Integer> cache, int a, int b) {
        long key = ((long)Math.min(a,b) << 32) | Math.max(a,b);
        return cache.computeIfAbsent(key, k -> {
            points.add(points.get(a).add(points.get(b)).unit());
            return points.size()-1;
        });
    }
}
