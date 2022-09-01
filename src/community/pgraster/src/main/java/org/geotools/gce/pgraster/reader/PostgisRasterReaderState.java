/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.gce.pgraster.reader;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.LinkedBlockingQueue;
import javax.media.jai.RenderedImageAdapter;
import org.geotools.gce.pgraster.PostgisRasterGridCoverage2DReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.image.util.ImageUtilities;

/**
 * Holds the state of the {@link PostgisRasterGridCoverage2DReader} making the reader thread safe.
 *
 * @author mcr
 * @since 2.6
 */
public class PostgisRasterReaderState {
    public static int DEFAULT_IMAGE_TYPE = BufferedImage.TYPE_3BYTE_BGR;

    private Color backgroundColor = null;

    private Color outputTransparentColor = null;

    private Rectangle renderedImageRectangle = null;

    private boolean xAxisSwitch = false;

    private GeneralEnvelope requestedEnvelope = null;

    private GeneralEnvelope requestedEnvelopeTransformed = null;

    private GeneralEnvelope requestedEnvelopeTransformedExpanded = null;

    private ImageLevelInfo imageLevelInfo = null;

    private final LinkedBlockingQueue<TileQueueElement> tileQueue = new LinkedBlockingQueue<>();

    /** @return BufferdImage filled with outputTransparentColor */
    public BufferedImage getEmptyImage(int width, int height) {
        Color backGroundcolor = getBackgroundColor();
        Color outputTransparentColor = getOutputTransparentColor();
        BufferedImage emptyImage =
                new BufferedImage(width, height, PostgisRasterReaderState.DEFAULT_IMAGE_TYPE);
        Graphics2D g2D = (Graphics2D) emptyImage.getGraphics();
        Color save = g2D.getColor();
        g2D.setColor(backGroundcolor);
        g2D.fillRect(0, 0, emptyImage.getWidth(), emptyImage.getHeight());
        g2D.setColor(save);
        if (outputTransparentColor != null) {
            emptyImage =
                    new RenderedImageAdapter(
                                    ImageUtilities.maskColor(outputTransparentColor, emptyImage))
                            .getAsBufferedImage();
        }
        return emptyImage;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getOutputTransparentColor() {
        return outputTransparentColor;
    }

    public void setOutputTransparentColor(Color outputTransparentColor) {
        this.outputTransparentColor = outputTransparentColor;
    }

    public Rectangle getRenderedImageRectangle() {
        return renderedImageRectangle;
    }

    public void setRenderedImageRectangle(Rectangle renderedImageRectangle) {
        this.renderedImageRectangle = renderedImageRectangle;
    }

    public boolean isXAxisSwitch() {
        return xAxisSwitch;
    }

    public void setXAxisSwitch(boolean axisSwitch) {
        xAxisSwitch = axisSwitch;
    }

    public GeneralEnvelope getRequestedEnvelope() {
        return requestedEnvelope;
    }

    public void setRequestedEnvelope(GeneralEnvelope requestedEnvelope) {
        this.requestedEnvelope = requestedEnvelope;
    }

    public GeneralEnvelope getRequestedEnvelopeTransformed() {
        return requestedEnvelopeTransformed;
    }

    public void setRequestedEnvelopeTransformed(GeneralEnvelope requestedEnvelopeTransformed) {
        this.requestedEnvelopeTransformed = requestedEnvelopeTransformed;
    }

    public GeneralEnvelope getRequestedEnvelopeTransformedExpanded() {
        return requestedEnvelopeTransformedExpanded;
    }

    public void setRequestedEnvelopeTransformedExpanded(
            GeneralEnvelope requestedEnvelopeTransformedExpanded) {
        this.requestedEnvelopeTransformedExpanded = requestedEnvelopeTransformedExpanded;
    }

    public ImageLevelInfo getImageLevelInfo() {
        return imageLevelInfo;
    }

    public void setImageLevelInfo(ImageLevelInfo imageLevelInfo) {
        this.imageLevelInfo = imageLevelInfo;
    }

    public LinkedBlockingQueue<TileQueueElement> getTileQueue() {
        return tileQueue;
    }
}
