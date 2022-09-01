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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.pgraster.config.Config;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;
import org.geotools.util.logging.Logging;

/**
 * This class reads decoded tiles from the queue and performs the mosaicing and scaling.
 *
 * @author mcr
 */
public class ImageComposerThread extends Thread {

    protected static final Logger LOGGER = Logging.getLogger(ImageComposerThread.class);

    private final Config config;

    private final PostgisRasterReaderState state;

    protected GridCoverageFactory coverageFactory;

    private GridCoverage2D gridCoverage2D;

    public ImageComposerThread(
            PostgisRasterReaderState state, Config config, GridCoverageFactory coverageFactory) {
        this.state = state;
        this.config = config;
        this.coverageFactory = coverageFactory;
    }

    private Dimension getStartDimension() {

        int width =
                (int)
                        Math.round(
                                state.getRequestedEnvelopeTransformedExpanded().getSpan(0)
                                        / state.getImageLevelInfo().getResX());
        int height =
                (int)
                        Math.round(
                                state.getRequestedEnvelopeTransformedExpanded().getSpan(1)
                                        / state.getImageLevelInfo().getResY());

        return new Dimension(width, height);
    }

    @SuppressWarnings("PMD.ReplaceHashtableWithMap")
    private BufferedImage getStartImage(BufferedImage copyFrom) {
        Dimension dim = getStartDimension();
        Hashtable<String, Object> properties = null;

        if (copyFrom.getPropertyNames() != null) {
            properties = new Hashtable<>();
            for (String name : copyFrom.getPropertyNames()) {
                properties.put(name, copyFrom.getProperty(name));
            }
        }

        SampleModel sm =
                copyFrom.getSampleModel()
                        .createCompatibleSampleModel((int) dim.getWidth(), (int) dim.getHeight());
        WritableRaster raster = Raster.createWritableRaster(sm, null);

        ColorModel colorModel = copyFrom.getColorModel();
        boolean alphaPremultiplied = copyFrom.isAlphaPremultiplied();

        DataBuffer dataBuffer =
                createDataBufferFilledWithNoDataValues(raster, colorModel.getPixelSize());
        raster = Raster.createWritableRaster(sm, dataBuffer, null);
        BufferedImage image = new BufferedImage(colorModel, raster, alphaPremultiplied, properties);
        if (state.getImageLevelInfo().getNoDataValue() == null) {
            Graphics2D g2D = (Graphics2D) image.getGraphics();
            Color save = g2D.getColor();
            g2D.setColor(state.getBackgroundColor());
            g2D.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2D.setColor(save);
        }
        return image;
    }

    private BufferedImage getStartImage(int imageType) {
        int imageTypeReviewed;
        if (imageType == BufferedImage.TYPE_CUSTOM) {
            imageTypeReviewed = PostgisRasterReaderState.DEFAULT_IMAGE_TYPE;
        } else {
            imageTypeReviewed = imageType;
        }

        Dimension dim = getStartDimension();
        BufferedImage image =
                new BufferedImage((int) dim.getWidth(), (int) dim.getHeight(), imageTypeReviewed);

        Graphics2D g2D = (Graphics2D) image.getGraphics();
        Color save = g2D.getColor();
        g2D.setColor(state.getBackgroundColor());
        g2D.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2D.setColor(save);

        return image;
    }

    @Override
    public void run() {
        BufferedImage image = null;

        TileQueueElement queueObject = null;
        LinkedBlockingQueue<TileQueueElement> tileQueue = state.getTileQueue();
        GeneralEnvelope rete = state.getRequestedEnvelopeTransformedExpanded();

        try {
            while ((queueObject = tileQueue.take()).isEndElement() == false) {

                if (image == null) {
                    image = getStartImage(queueObject.getTileImage());
                }

                int posx =
                        (int)
                                (Math.round(
                                        (queueObject.getEnvelope().getMinimum(0)
                                                        - rete.getMinimum(0))
                                                / state.getImageLevelInfo().getResX()));
                int posy =
                        (int)
                                (Math.round(
                                        (rete.getMaximum(1)
                                                        - queueObject.getEnvelope().getMaximum(1))
                                                / state.getImageLevelInfo().getResY()));

                image.getRaster().setRect(posx, posy, queueObject.getTileImage().getRaster());
            }
        } catch (OutOfMemoryError e) {
            LOGGER.warning(
                    "Out of memory when trying to render coverage '"
                            + state.getImageLevelInfo().getCoverageName()
                            + "'.");
            LOGGER.warning(
                    "Tips: increase memory, add coarser dataset overviews, or do not zoom that out.");
            // return no image
            gridCoverage2D = null;
            return;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (image == null) {
            // no tiles ??
            image = getStartImage(PostgisRasterReaderState.DEFAULT_IMAGE_TYPE);
        }

        GeneralEnvelope resultEnvelope = null;

        if (state.isXAxisSwitch()) {
            Rectangle2D tmp =
                    new Rectangle2D.Double(
                            rete.getMinimum(1),
                            rete.getMinimum(0),
                            rete.getSpan(1),
                            rete.getSpan(0));
            resultEnvelope = new GeneralEnvelope(tmp);
            resultEnvelope.setCoordinateReferenceSystem(rete.getCoordinateReferenceSystem());
        } else {
            resultEnvelope = state.getRequestedEnvelopeTransformed();
        }

        image = rescaleImageViaPlanarImage(image);
        if (state.getOutputTransparentColor() == null) {
            gridCoverage2D =
                    coverageFactory.create(config.getCoverageName(), image, resultEnvelope);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Support for alpha on final mosaic");
            }
            RenderedImage result =
                    ImageUtilities.maskColor(state.getOutputTransparentColor(), image);
            gridCoverage2D =
                    coverageFactory.create(config.getCoverageName(), result, resultEnvelope);
        }
    }

    public GridCoverage2D getGridCoverage2D() {
        return gridCoverage2D;
    }

    private DataBuffer createDataBufferFilledWithNoDataValues(
            WritableRaster raster, int pixelSize) {
        int dataType = raster.getDataBuffer().getDataType();

        Number noDataValue = state.getImageLevelInfo().getNoDataValue();

        int dataBufferSize = raster.getDataBuffer().getSize();
        int nrBanks = raster.getDataBuffer().getNumBanks();
        DataBuffer dataBuffer;
        switch (dataType) {
            case DataBuffer.TYPE_INT:
                int[][] intDataArray = new int[nrBanks][dataBufferSize];
                if (noDataValue != null) {
                    for (int i = 0; i < nrBanks; i++) {
                        Arrays.fill(intDataArray[i], noDataValue.intValue());
                    }
                }
                dataBuffer = new DataBufferInt(intDataArray, dataBufferSize);
                break;
            case DataBuffer.TYPE_FLOAT:
                float[][] floatDataArray = new float[nrBanks][dataBufferSize];
                if (noDataValue != null) {
                    for (int i = 0; i < nrBanks; i++) {
                        Arrays.fill(floatDataArray[i], noDataValue.floatValue());
                    }
                }
                dataBuffer = new DataBufferFloat(floatDataArray, dataBufferSize);
                break;
            case DataBuffer.TYPE_DOUBLE:
                double[][] doubleDataArray = new double[nrBanks][dataBufferSize];
                if (noDataValue != null) {
                    for (int i = 0; i < nrBanks; i++) {
                        Arrays.fill(doubleDataArray[i], noDataValue.doubleValue());
                    }
                }
                dataBuffer = new DataBufferDouble(doubleDataArray, dataBufferSize);
                break;

            case DataBuffer.TYPE_SHORT:
                short[][] shortDataArray = new short[nrBanks][dataBufferSize];
                if (noDataValue != null) {
                    for (int i = 0; i < nrBanks; i++) {
                        Arrays.fill(shortDataArray[i], noDataValue.shortValue());
                    }
                }
                dataBuffer = new DataBufferShort(shortDataArray, dataBufferSize);
                break;

            case DataBuffer.TYPE_BYTE:
                byte[][] byteDataArray = new byte[nrBanks][dataBufferSize];
                if (noDataValue != null) {
                    for (int i = 0; i < nrBanks; i++) {
                        Arrays.fill(byteDataArray[i], noDataValue.byteValue());
                    }
                }
                dataBuffer = new DataBufferByte(byteDataArray, dataBufferSize);
                break;

            case DataBuffer.TYPE_USHORT:
                short[][] ushortDataArray = new short[nrBanks][dataBufferSize];
                if (noDataValue != null) {
                    for (int i = 0; i < nrBanks; i++) {
                        Arrays.fill(ushortDataArray[i], noDataValue.shortValue());
                    }
                }
                dataBuffer = new DataBufferUShort(ushortDataArray, dataBufferSize);
                break;

            default:
                throw new IllegalStateException(
                        "Couldn't create DataBuffer for  data type "
                                + dataType
                                + " and "
                                + pixelSize
                                + " pixel size");
        }
        return dataBuffer;
    }

    protected BufferedImage rescaleImageViaPlanarImage(BufferedImage image) {
        int interpolation = Interpolation.INTERP_NEAREST;
        if (config.getInterpolation().intValue() == 2) {
            interpolation = Interpolation.INTERP_BILINEAR;
        }
        if (config.getInterpolation().intValue() == 3) {
            interpolation = Interpolation.INTERP_BICUBIC;
        }

        GeneralEnvelope ret = state.getRequestedEnvelopeTransformed();
        GeneralEnvelope rete = state.getRequestedEnvelopeTransformedExpanded();
        Rectangle rir = state.getRenderedImageRectangle();
        // On GetServiceInfo requests, 'rir' is not located at (0,0).
        Rectangle rirO = new Rectangle(rir);
        rirO.setLocation(0, 0);

        double resRequestedX = ret.getSpan(0) / rir.getWidth();
        double resRequestedY = ret.getSpan(1) / rir.getHeight();

        PlanarImage planarImage = new TiledImage(image, image.getWidth(), image.getHeight());
        ImageWorker w = new ImageWorker(planarImage);
        w.scale(
                state.getImageLevelInfo().getResX() / resRequestedX,
                state.getImageLevelInfo().getResY() / resRequestedY,
                (rete.getMinimum(0) - ret.getMinimum(0)) / resRequestedX,
                (ret.getMaximum(1) - rete.getMaximum(1)) / resRequestedY,
                Interpolation.getInstance(interpolation));
        RenderedOp result = w.getRenderedOperation();
        Raster scaledImageRaster = result.getData(rirO);
        if (!(scaledImageRaster instanceof WritableRaster)) {
            scaledImageRaster =
                    result.copyData(scaledImageRaster.createCompatibleWritableRaster(rirO));
        }

        BufferedImage scaledImage =
                new BufferedImage(
                        image.getColorModel(),
                        (WritableRaster) scaledImageRaster,
                        image.isAlphaPremultiplied(),
                        null);
        return scaledImage;
    }
}
