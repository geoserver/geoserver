/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import org.apache.batik.ext.awt.g2d.AbstractGraphics2D;
import org.apache.batik.ext.awt.g2d.GraphicContext;

/**
 * Fake Graphics2D, pretends to draw but actually does nothing (Very dishonest!)
 *
 * <p>Implementation note, the base class provides a lot of state management ensuring clients do not
 * go NPE when asking for current clip, color, stroke and the like
 *
 * @author Andrea Aime - GeoSolutions
 */
class NoOpGraphics2D extends AbstractGraphics2D {

    public NoOpGraphics2D() {
        super(true);
        this.gc = new GraphicContext();
    }

    @Override
    public void draw(Shape s) {}

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {}

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {}

    @Override
    public void drawString(String str, float x, float y) {}

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {}

    @Override
    public void fill(Shape s) {}

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {

        return null;
    }

    @Override
    public Graphics create() {

        return this;
    }

    @Override
    public void setXORMode(Color c1) {}

    @Override
    public FontMetrics getFontMetrics(Font f) {
        // works also in headless mode
        return new Canvas().getFontMetrics(f);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {}

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(
            Image img, int x, int y, int width, int height, ImageObserver observer) {
        return false;
    }

    @Override
    public void dispose() {}
}
