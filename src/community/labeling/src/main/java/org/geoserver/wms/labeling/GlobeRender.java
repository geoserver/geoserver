/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

/** Shapes rendering internal functions. */
class GlobeRender {

    static final Color BACKGROUND_COLOR = Color.WHITE;
    static final Color STROKE_COLOR = Color.GRAY;
    static final Color SHADOW_COLOR = Color.BLACK;
    static final double SHADOW_DISTANCE = 4.0d;
    static final float SHADOW_ALPHA = 0.3f;
    static final int STROKE_SIZE = 2;
    static final int BOTTON_TRIANGLE_WIDTH = 12;
    static final int BOTTON_TRIANGLE_HEIGHT = 12;

    static final AlphaComposite SHADOW_COMPOSITE =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, SHADOW_ALPHA);

    /**
     * Draws the attributes background globe.
     *
     * @param graphics2d the {@link Graphics2D} instance where to draw the globe
     * @param bounds the bounds to use for drawing the rounded rectangle
     */
    static void renderGlobe(Graphics2D graphics2d, GlobeBounds bounds) {
        renderGlobe(graphics2d, bounds, defaultRenderConfig());
    }

    static void renderGlobe(Graphics2D graphics2d, GlobeBounds bounds, TailDimensions config) {
        renderShadow(graphics2d, bounds, config);
        renderGlobeFront(graphics2d, bounds, config);
    }

    private static void renderGlobeFront(
            Graphics2D graphics2d, GlobeBounds bounds, TailDimensions config) {
        graphics2d.setComposite(AlphaComposite.SrcOver);
        graphics2d.setStroke(new BasicStroke(STROKE_SIZE));
        // create the text globe shape
        Area shape = buildGlobeShape(bounds, 0d, config);

        graphics2d.setColor(BACKGROUND_COLOR);
        graphics2d.fill(shape);
        graphics2d.setColor(STROKE_COLOR);
        graphics2d.draw(shape);
    }

    private static Area buildGlobeShape(GlobeBounds bounds, double delta, TailDimensions config) {
        RoundRectangle2D rectangle =
                new RoundRectangle2D.Double(
                        delta,
                        delta,
                        bounds.getWidth(),
                        bounds.getHeight(),
                        bounds.getRadius(),
                        bounds.getRadius());
        Shape triangle = getGlobeBottonTriangle(bounds, delta, config);
        Area shape = new Area(rectangle);
        shape.add(new Area(triangle));
        return shape;
    }

    private static void renderShadow(
            Graphics2D graphics2d, GlobeBounds bounds, TailDimensions config) {
        graphics2d.setComposite(SHADOW_COMPOSITE);
        graphics2d.setStroke(new BasicStroke(0f));
        graphics2d.setColor(SHADOW_COLOR);
        // create the text globe shape
        Area shape = buildGlobeShape(bounds, SHADOW_DISTANCE, config);
        graphics2d.fill(shape);
    }

    private static Shape getGlobeBottonTriangle(
            GlobeBounds bounds, double delta, TailDimensions config) {
        double middlex = (bounds.getWidth() / 2) + delta;
        double xmin = middlex - config.getTailWidthHalf();
        double xmax = middlex + config.getTailWidthHalf();
        double ymin = bounds.getHeight() + delta;
        double ymax = ymin + config.getTaildHeight();
        Polygon triangle =
                new Polygon(
                        new int[] {(int) xmin, (int) xmax, (int) middlex},
                        new int[] {(int) ymin, (int) ymin, (int) ymax},
                        3);
        return triangle;
    }

    public static class TailDimensions {
        private int tailWidth;
        private int taildHeight;
        private int tailWidthHalf;

        public TailDimensions(int tailWidth, int taildHeight) {
            super();
            setTailWidth(tailWidth);
            this.taildHeight = taildHeight;
        }

        public int getTailWidth() {
            return tailWidth;
        }

        public void setTailWidth(int tailWidth) {
            this.tailWidth = tailWidth;
            this.tailWidthHalf = tailWidth / 2;
        }

        public int getTaildHeight() {
            return taildHeight;
        }

        public void setTaildHeight(int taildHeight) {
            this.taildHeight = taildHeight;
        }

        public int getTailWidthHalf() {
            return tailWidthHalf;
        }

        @Override
        public String toString() {
            return "GlobeRenderConfiguration [tailWidth="
                    + tailWidth
                    + ", taildHeight="
                    + taildHeight
                    + "]";
        }
    }

    static TailDimensions defaultRenderConfig() {
        return new TailDimensions(BOTTON_TRIANGLE_WIDTH, BOTTON_TRIANGLE_HEIGHT);
    }
}
