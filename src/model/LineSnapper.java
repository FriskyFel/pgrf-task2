package model;

public class LineSnapper {

    public static Point2D snap(Point2D start, Point2D mouse) {
        int dx = mouse.getX() - start.getX();
        int dy = mouse.getY() - start.getY();

        Point2D horizontal = new Point2D(start.getX() + dx, start.getY());
        Point2D vertical = new Point2D(start.getX(), start.getY() + dy);

        int length = (Math.abs(dx) + Math.abs(dy)) / 2;
        Point2D diagonal = new Point2D(
                start.getX() + Integer.signum(dx) * length,
                start.getY() + Integer.signum(dy) * length);

        Point2D best = horizontal;
        if (distanceSquared(vertical, mouse) < distanceSquared(best, mouse)) {
            best = vertical;
        }
        if (distanceSquared(diagonal, mouse) < distanceSquared(best, mouse)) {
            best = diagonal;
        }
        return best;
    }

    private static int distanceSquared(Point2D a, Point2D b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }
}
