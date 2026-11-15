package fill;

import model.Point2D;
import raster.Raster;

import java.util.ArrayDeque;
import java.util.Deque;

public class SeedFiller extends Filler {

    public enum Boundary {BORDER, BACKGROUND}

    private final int borderColor;
    private final int backgroundColor;
    private Boundary boundary = Boundary.BORDER;

    public SeedFiller(Raster raster, PatternFill pattern, int borderColor, int backgroundColor) {
        super(raster, pattern);
        this.borderColor = borderColor;
        this.backgroundColor = backgroundColor;
    }

    public void setBoundary(Boundary boundary) {
        this.boundary = boundary;
    }

    public void fill(Point2D seed) {
        boolean[] visited = new boolean[raster.getWidth() * raster.getHeight()];
        Deque<Point2D> stack = new ArrayDeque<>();

        push(stack, visited, seed.getX(), seed.getY());

        while (!stack.isEmpty()) {
            Point2D p = stack.pop();
            int x = p.getX();
            int y = p.getY();

            raster.setPixel(x, y, pattern.getColor(x, y));

            push(stack, visited, x + 1, y);
            push(stack, visited, x - 1, y);
            push(stack, visited, x, y + 1);
            push(stack, visited, x, y - 1);
        }
    }

    private void push(Deque<Point2D> stack, boolean[] visited, int x, int y) {
        if (x < 0 || y < 0 || x >= raster.getWidth() || y >= raster.getHeight()) {
            return;
        }

        int index = y * raster.getWidth() + x;
        if (visited[index] || !belongsToArea(x, y)) {
            return;
        }

        visited[index] = true;
        stack.push(new Point2D(x, y));
    }

    private boolean belongsToArea(int x, int y) {
        int color = raster.getPixel(x, y);
        if (boundary == Boundary.BORDER) {
            return color != borderColor;
        }
        return color == backgroundColor;
    }
}
