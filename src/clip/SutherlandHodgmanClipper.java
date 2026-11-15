package clip;

import model.Point2D;
import model.Polygon;

import java.util.ArrayList;
import java.util.List;

public class SutherlandHodgmanClipper {

    private final Polygon clipPolygon;
    private final int orientation;

    public SutherlandHodgmanClipper(Polygon clipPolygon) {
        this.clipPolygon = clipPolygon;
        this.orientation = signedArea(clipPolygon) >= 0 ? 1 : -1;
    }

    public Polygon clip(Polygon subject) {
        List<Point2D> in = new ArrayList<>();
        for (int i = 0; i < subject.size(); i++) {
            in.add(subject.getPoint(i));
        }

        int clipSize = clipPolygon.size();

        for (int e = 0; e < clipSize && !in.isEmpty(); e++) {
            Point2D a = clipPolygon.getPoint(e);
            Point2D b = clipPolygon.getPoint((e + 1) % clipSize);

            List<Point2D> out = new ArrayList<>();
            Point2D v1 = in.get(in.size() - 1);

            for (Point2D v2 : in) {
                if (isInside(v2, a, b)) {
                    if (!isInside(v1, a, b)) {
                        out.add(intersection(v1, v2, a, b));
                    }
                    out.add(v2);
                } else if (isInside(v1, a, b)) {
                    out.add(intersection(v1, v2, a, b));
                }
                v1 = v2;
            }

            in = out;
        }

        Polygon result = new Polygon();
        for (Point2D p : in) {
            result.addPoint(p);
        }
        return result;
    }

    private boolean isInside(Point2D p, Point2D a, Point2D b) {
        return cross(a, b, p) * orientation >= 0;
    }

    private Point2D intersection(Point2D v1, Point2D v2, Point2D a, Point2D b) {
        double d1 = cross(a, b, v1);
        double d2 = cross(a, b, v2);
        double t = d1 / (d1 - d2);

        double x = v1.getX() + t * (v2.getX() - v1.getX());
        double y = v1.getY() + t * (v2.getY() - v1.getY());

        return new Point2D((int) Math.round(x), (int) Math.round(y));
    }

    private static double cross(Point2D a, Point2D b, Point2D p) {
        return (double) (b.getX() - a.getX()) * (p.getY() - a.getY())
                - (double) (b.getY() - a.getY()) * (p.getX() - a.getX());
    }

    private static double signedArea(Polygon polygon) {
        double area = 0;
        int n = polygon.size();
        for (int i = 0; i < n; i++) {
            Point2D a = polygon.getPoint(i);
            Point2D b = polygon.getPoint((i + 1) % n);
            area += (double) a.getX() * b.getY() - (double) b.getX() * a.getY();
        }
        return area / 2;
    }

    public Polygon getClipPolygon() {
        return clipPolygon;
    }
}
