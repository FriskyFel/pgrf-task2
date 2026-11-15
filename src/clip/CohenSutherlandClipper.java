package clip;

import model.Line;
import model.Point2D;

public class CohenSutherlandClipper {

    private static final int INSIDE = 0b0000;
    private static final int LEFT = 0b0001;
    private static final int RIGHT = 0b0010;
    private static final int TOP = 0b0100;
    private static final int BOTTOM = 0b1000;

    private final int xMin, yMin, xMax, yMax;

    public CohenSutherlandClipper(int xMin, int yMin, int xMax, int yMax) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }

    public Line clip(Line line) {
        double x1 = line.getP1().getX();
        double y1 = line.getP1().getY();
        double x2 = line.getP2().getX();
        double y2 = line.getP2().getY();

        int code1 = regionCode(x1, y1);
        int code2 = regionCode(x2, y2);

        while (true) {
            if ((code1 | code2) == INSIDE) {
                return new Line(
                        new Point2D((int) Math.round(x1), (int) Math.round(y1)),
                        new Point2D((int) Math.round(x2), (int) Math.round(y2)));
            }

            if ((code1 & code2) != INSIDE) {
                return null;
            }

            int outside = code1 != INSIDE ? code1 : code2;
            double x, y;

            if ((outside & LEFT) != 0) {
                x = xMin;
                y = y1 + (y2 - y1) * (xMin - x1) / (x2 - x1);
            } else if ((outside & RIGHT) != 0) {
                x = xMax;
                y = y1 + (y2 - y1) * (xMax - x1) / (x2 - x1);
            } else if ((outside & TOP) != 0) {
                y = yMin;
                x = x1 + (x2 - x1) * (yMin - y1) / (y2 - y1);
            } else {
                y = yMax;
                x = x1 + (x2 - x1) * (yMax - y1) / (y2 - y1);
            }

            if (outside == code1) {
                x1 = x;
                y1 = y;
                code1 = regionCode(x1, y1);
            } else {
                x2 = x;
                y2 = y;
                code2 = regionCode(x2, y2);
            }
        }
    }

    private int regionCode(double x, double y) {
        int code = INSIDE;
        if (x < xMin) {
            code |= LEFT;
        } else if (x > xMax) {
            code |= RIGHT;
        }
        if (y < yMin) {
            code |= TOP;
        } else if (y > yMax) {
            code |= BOTTOM;
        }
        return code;
    }

    public int getXMin() {
        return xMin;
    }

    public int getYMin() {
        return yMin;
    }

    public int getXMax() {
        return xMax;
    }

    public int getYMax() {
        return yMax;
    }
}
