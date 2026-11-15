package fill;

import model.Point2D;
import model.Polygon;
import raster.Raster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScanLineFiller extends Filler {

    private static class Edge {
        final int yMin, yMax;
        final double k, q;

        Edge(Point2D a, Point2D b) {
            Point2D top = a.getY() < b.getY() ? a : b;
            Point2D bottom = a.getY() < b.getY() ? b : a;

            yMin = top.getY();
            yMax = bottom.getY() - 1;

            k = (double) (bottom.getX() - top.getX()) / (bottom.getY() - top.getY());
            q = top.getX() - k * top.getY();
        }

        boolean hasIntersection(int y) {
            return y >= yMin && y <= yMax;
        }

        int intersection(int y) {
            return (int) Math.round(k * y + q);
        }
    }

    public ScanLineFiller(Raster raster, PatternFill pattern) {
        super(raster, pattern);
    }

    public void fill(Polygon polygon) {
        int n = polygon.size();
        if (n < 3) {
            return;
        }

        List<Edge> edges = new ArrayList<>();
        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;

        for (int i = 0; i < n; i++) {
            Point2D a = polygon.getPoint(i);
            Point2D b = polygon.getPoint((i + 1) % n);

            if (a.getY() == b.getY()) {
                continue;
            }

            Edge edge = new Edge(a, b);
            edges.add(edge);
            yMin = Math.min(yMin, edge.yMin);
            yMax = Math.max(yMax, edge.yMax);
        }

        yMin = Math.max(yMin, 0);
        yMax = Math.min(yMax, raster.getHeight() - 1);

        List<Integer> intersections = new ArrayList<>();

        for (int y = yMin; y <= yMax; y++) {
            intersections.clear();

            for (Edge edge : edges) {
                if (edge.hasIntersection(y)) {
                    intersections.add(edge.intersection(y));
                }
            }

            Collections.sort(intersections);

            for (int i = 1; i < intersections.size(); i += 2) {
                int from = Math.max(intersections.get(i - 1), 0);
                int to = Math.min(intersections.get(i), raster.getWidth() - 1);
                for (int x = from; x <= to; x++) {
                    raster.setPixel(x, y, pattern.getColor(x, y));
                }
            }
        }
    }
}
