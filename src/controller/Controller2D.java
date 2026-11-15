package controller;

import fill.PatternFill;
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

    private enum Drag {NONE, LINE, POLYGON, FREEHAND}

    private enum Mode {DRAW, FREEHAND, SEED}

    private final Panel panel;
    private final Raster raster;
    private final LineRasterizer lineRasterizer;
    private final PolygonRasterizer polygonRasterizer;
    private final SeedFiller seedFiller;

    private final List<Line> lines = new ArrayList<>();
    private final Polygon polygon = new Polygon();
    private final List<Line> strokes = new ArrayList<>();
    private final List<Point2D> seeds = new ArrayList<>();

    private Drag drag = Drag.NONE;
    private Point2D lineStart;
    private Point2D mouse;

    private Mode mode = Mode.DRAW;
    private SeedFiller.Boundary boundary = SeedFiller.Boundary.BORDER;

    public Controller2D(Panel panel) {
        this.panel = panel;

        raster = panel.getRaster();
        lineRasterizer = new LineRasterizerTrivial(raster);
        polygonRasterizer = new PolygonRasterizer(lineRasterizer);

        PatternFill fill = new SolidFill(FILL_COLOR);
        seedFiller = new SeedFiller(raster, fill, BORDER_COLOR, BACKGROUND_COLOR);

        initlizeEventListeners();
        updateTitle();
        redraw();
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
                    strokes.add(new Line(mouse, current));
                    mouse = current;
                } else {
                    mouse = drag == Drag.LINE ? snapIfShift(current, e) : current;
                }
                redraw();
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


    private void clearAll() {
        lines.clear();
        polygon.clear();
        strokes.clear();
        seeds.clear();
        mode = Mode.DRAW;
        boundary = SeedFiller.Boundary.BORDER;
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

        drawOutlines();

        seedFiller.setBoundary(boundary);
        for (Point2D seed : seeds) {
            seedFiller.fill(seed);
        }

        drawOutlines();

        drawRubberBand();
        panel.repaint();
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


    private void drawOutlines() {
        for (Line line : lines) {
            lineRasterizer.rasterize(line, LINE_COLOR);
        }

        drawPolygonOutline();

        for (Line stroke : strokes) {
            lineRasterizer.rasterize(stroke, BORDER_COLOR);
        }
    }


    private void drawPolygonOutline() {
        polygonRasterizer.rasterize(polygon, POLYGON_COLOR, drag != Drag.POLYGON);
    }


    private void updateTitle() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel);
        if (frame == null) {
            return;
        }
        frame.setTitle(String.format(
                "PGRF1 | mode %s | seed %s"
                        + " | D draw, H freehand, S seed, B, C",
                mode, boundary));
    }

}
