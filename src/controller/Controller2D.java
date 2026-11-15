package controller;

import clip.CohenSutherlandClipper;
import clip.SutherlandHodgmanClipper;
import fill.CheckerboardFill;
import fill.PatternFill;
import fill.ScanLineFiller;
import fill.SeedFiller;
import fill.SolidFill;
import model.Line;
import model.LineSnapper;
import model.Point2D;
import model.Polygon;
import raster.Raster;
import rasterize.LineRasterizer;
import rasterize.LineRasterizerTrivial;
import rasterize.PolygonRasterizer;
import view.Panel;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;


public class Controller2D {
    private static final int LINE_COLOR = 0xFFFFFF;
    private static final int POLYGON_COLOR = 0xFFFF00;
    private static final int RUBBER_COLOR = 0xFF0000;
    private static final int BACKGROUND_COLOR = 0x000000;
    private static final int BORDER_COLOR = 0x00FFFF;
    private static final int FILL_COLOR = 0x00AA44;
    private static final int PATTERN_COLOR = 0xAA00AA;
    private static final int CLIP_WINDOW_COLOR = 0xFF4444;
    private static final int CLIP_POLYGON_COLOR = 0xFF44FF;
    private static final int CLIPPED_COLOR = 0x44FF44;
    private static final int GHOST_COLOR = 0x404040;

    private enum Drag {NONE, LINE, POLYGON, FREEHAND}

    private enum Mode {DRAW, FREEHAND, SEED}

    private final Panel panel;
    private final Raster raster;
    private final LineRasterizer lineRasterizer;
    private final PolygonRasterizer polygonRasterizer;
    private final SeedFiller seedFiller;
    private final ScanLineFiller scanLineFiller;
    private final CohenSutherlandClipper lineClipper;
    private final SutherlandHodgmanClipper polygonClipper;

    private final List<Line> lines = new ArrayList<>();
    private final Polygon polygon = new Polygon();
    private final List<Line> strokes = new ArrayList<>();
    private final List<Point2D> seeds = new ArrayList<>();

    private Drag drag = Drag.NONE;
    private Point2D lineStart;
    private Point2D mouse;

    private Mode mode = Mode.DRAW;
    private SeedFiller.Boundary boundary = SeedFiller.Boundary.BORDER;
    private boolean scanLineEnabled = false;
    private boolean clippingEnabled = false;
    private boolean patternEnabled = false;

    private int[] scene;

    public Controller2D(Panel panel) {
        this.panel = panel;

        raster = panel.getRaster();
        lineRasterizer = new LineRasterizerTrivial(raster);
        polygonRasterizer = new PolygonRasterizer(lineRasterizer);

        PatternFill fill = new SolidFill(FILL_COLOR);
        seedFiller = new SeedFiller(raster, fill, BORDER_COLOR, BACKGROUND_COLOR);
        scanLineFiller = new ScanLineFiller(raster, fill);

        lineClipper = new CohenSutherlandClipper(150, 100, 650, 500);
        polygonClipper = new SutherlandHodgmanClipper(createClipPolygon());

        initlizeEventListeners();
        updateTitle();
        redraw();
    }

    private Polygon createClipPolygon() {
        Polygon clip = new Polygon();
        clip.addPoint(new Point2D(400, 60));
        clip.addPoint(new Point2D(700, 260));
        clip.addPoint(new Point2D(585, 540));
        clip.addPoint(new Point2D(215, 540));
        clip.addPoint(new Point2D(100, 260));
        return clip;
    }

    private void initlizeEventListeners() {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                panel.requestFocusInWindow();
                if (drag != Drag.NONE) {
                    return;
                }

                if (mode == Mode.SEED) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        seeds.add(new Point2D(e.getX(), e.getY()));
                        redraw();
                    }
                    return;
                }

                if (mode == Mode.FREEHAND) {
                    if (e.getButton() != MouseEvent.BUTTON1) {
                        return;
                    }
                    drag = Drag.FREEHAND;
                } else if (e.getButton() == MouseEvent.BUTTON3) {

                    drag = Drag.LINE;
                    lineStart = new Point2D(e.getX(), e.getY());
                } else if (e.getButton() == MouseEvent.BUTTON1) {

                    drag = Drag.POLYGON;
                } else {
                    return;
                }
                mouse = new Point2D(e.getX(), e.getY());
                redraw();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point2D end = new Point2D(e.getX(), e.getY());

                if (drag == Drag.LINE && e.getButton() == MouseEvent.BUTTON3) {

                    lines.add(new Line(lineStart, snapIfShift(end, e)));
                } else if (drag == Drag.POLYGON && e.getButton() == MouseEvent.BUTTON1) {
                    polygon.addPoint(end);
                } else if (drag == Drag.FREEHAND && e.getButton() == MouseEvent.BUTTON1) {
                    strokes.add(new Line(mouse, end));
                } else {
                    return;
                }
                stopDrag();
                redraw();
            }
        });

        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (drag == Drag.NONE) {
                    return;
                }
                Point2D current = new Point2D(e.getX(), e.getY());

                if (drag == Drag.FREEHAND) {
                    Line stroke = new Line(mouse, current);
                    strokes.add(stroke);
                    lineRasterizer.rasterize(stroke, BORDER_COLOR);
                    mouse = current;
                    panel.repaint();
                    return;
                }

                mouse = drag == Drag.LINE ? snapIfShift(current, e) : current;
                redrawPreview();
            }
        });

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_C:
                        clearAll();
                        return;
                    case KeyEvent.VK_D:
                        mode = Mode.DRAW;
                        break;
                    case KeyEvent.VK_H:
                        mode = Mode.FREEHAND;
                        break;
                    case KeyEvent.VK_S:
                        mode = Mode.SEED;
                        break;
                    case KeyEvent.VK_F:
                        scanLineEnabled = !scanLineEnabled;
                        break;
                    case KeyEvent.VK_O:
                        clippingEnabled = !clippingEnabled;
                        break;
                    case KeyEvent.VK_P:
                        patternEnabled = !patternEnabled;
                        applyPattern();
                        break;
                    case KeyEvent.VK_B:
                        boundary = boundary == SeedFiller.Boundary.BORDER
                                ? SeedFiller.Boundary.BACKGROUND
                                : SeedFiller.Boundary.BORDER;
                        break;
                    default:
                        return;
                }
                updateTitle();
                redraw();
            }
        });
    }

    private Point2D snapIfShift(Point2D point, MouseEvent e) {
        return e.isShiftDown() ? LineSnapper.snap(lineStart, point) : point;
    }

    private void applyPattern() {
        PatternFill fill = patternEnabled
                ? new CheckerboardFill(FILL_COLOR, PATTERN_COLOR, 8)
                : new SolidFill(FILL_COLOR);
        seedFiller.setPattern(fill);
        scanLineFiller.setPattern(fill);
    }

    private void clearAll() {
        lines.clear();
        polygon.clear();
        strokes.clear();
        seeds.clear();
        mode = Mode.DRAW;
        boundary = SeedFiller.Boundary.BORDER;
        scanLineEnabled = false;
        clippingEnabled = false;
        patternEnabled = false;
        applyPattern();
        stopDrag();
        updateTitle();
        redraw();
    }

    private void stopDrag() {
        drag = Drag.NONE;
        lineStart = null;
        mouse = null;
    }

    private void redraw() {
        raster.clear();

        drawPolygonFill();
        drawOutlines();

        seedFiller.setBoundary(boundary);
        for (Point2D seed : seeds) {
            seedFiller.fill(seed);
        }

        drawOutlines();
        drawClipRegions();

        saveScene();
        drawRubberBand();
        panel.repaint();
    }

    private void redrawPreview() {
        restoreScene();
        drawRubberBand();
        panel.repaint();
    }

    private void saveScene() {
        int width = raster.getWidth();
        int height = raster.getHeight();
        if (scene == null || scene.length != width * height) {
            scene = new int[width * height];
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                scene[y * width + x] = raster.getPixel(x, y);
            }
        }
    }

    private void restoreScene() {
        int width = raster.getWidth();
        for (int y = 0; y < raster.getHeight(); y++) {
            for (int x = 0; x < width; x++) {
                raster.setPixel(x, y, scene[y * width + x]);
            }
        }
    }

    private void drawRubberBand() {
        if (drag == Drag.LINE) {
            lineRasterizer.rasterize(lineStart, mouse, RUBBER_COLOR);
        } else if (drag == Drag.POLYGON) {
            if (polygon.isEmpty()) {
                polygonRasterizer.rasterizePoint(mouse, RUBBER_COLOR);
            } else {
                lineRasterizer.rasterize(polygon.getFirst(), mouse, RUBBER_COLOR);
                lineRasterizer.rasterize(polygon.getLast(), mouse, RUBBER_COLOR);
            }
        }
    }

    private void drawLine(Line line) {
        if (!clippingEnabled) {
            lineRasterizer.rasterize(line, LINE_COLOR);
            return;
        }

        lineRasterizer.rasterize(line, GHOST_COLOR);
        Line clipped = lineClipper.clip(line);
        if (clipped != null) {
            lineRasterizer.rasterize(clipped, CLIPPED_COLOR);
        }
    }

    private void drawOutlines() {
        for (Line line : lines) {
            drawLine(line);
        }

        drawPolygonOutline();

        for (Line stroke : strokes) {
            lineRasterizer.rasterize(stroke, BORDER_COLOR);
        }
    }

    private void drawPolygonFill() {
        if (!scanLineEnabled || drag == Drag.POLYGON) {
            return;
        }
        scanLineFiller.fill(clippingEnabled ? polygonClipper.clip(polygon) : polygon);
    }

    private void drawPolygonOutline() {
        boolean closed = drag != Drag.POLYGON;

        if (!clippingEnabled) {
            polygonRasterizer.rasterize(polygon, POLYGON_COLOR, closed);
            return;
        }

        polygonRasterizer.rasterize(polygon, GHOST_COLOR, closed);
        polygonRasterizer.rasterize(polygonClipper.clip(polygon), CLIPPED_COLOR);
    }

    private void drawClipRegions() {
        if (!clippingEnabled) {
            return;
        }

        int x1 = lineClipper.getXMin();
        int y1 = lineClipper.getYMin();
        int x2 = lineClipper.getXMax();
        int y2 = lineClipper.getYMax();

        lineRasterizer.rasterize(x1, y1, x2, y1, CLIP_WINDOW_COLOR);
        lineRasterizer.rasterize(x2, y1, x2, y2, CLIP_WINDOW_COLOR);
        lineRasterizer.rasterize(x2, y2, x1, y2, CLIP_WINDOW_COLOR);
        lineRasterizer.rasterize(x1, y2, x1, y1, CLIP_WINDOW_COLOR);

        polygonRasterizer.rasterize(polygonClipper.getClipPolygon(), CLIP_POLYGON_COLOR);
    }

    private void updateTitle() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel);
        if (frame == null) {
            return;
        }
        frame.setTitle(String.format(
                "PGRF1 | mode %s | seed %s | scanline %s | clip %s | pattern %s"
                        + " | D draw, H freehand, S seed, F, O, B, P, C",
                mode, boundary, onOff(scanLineEnabled), onOff(clippingEnabled), onOff(patternEnabled)));
    }

    private static String onOff(boolean value) {
        return value ? "on" : "off";
    }
}
