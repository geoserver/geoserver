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

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.util.logging.Logging;

/**
 * This class is used for multithreaded decoding of the tiles read from the database.
 *
 * <p>Decoding errors result in a log message and the result is <code>null</code>.
 *
 * <p>Creating an ImageDocoderThread object with a null or empty bytearray result in a <code>null
 * </code> value for <code>g {@link #getBufferedImage()}</code>
 *
 * @author mcr, christian
 */
class ImageDecoderThread extends Thread {

    protected static final Logger LOGGER = Logging.getLogger(ImageDecoderThread.class);

    LinkedBlockingQueue<TileQueueElement> tileQueue;

    GeneralEnvelope requestEnvelope;

    ImageLevelInfo levelInfo;

    private byte[] imageBytes;

    private String location;

    private GeneralEnvelope dbTileEnvelope;

    /**
     * Decoder thread.
     *
     * @param bytes the image bytes
     * @param location the tile name
     * @param dbTileEnvelope the georeferencing information for the tile
     * @param requestEnvelope the requested envelope
     * @param levelInfo the proper levelInfo
     * @param tileQueue the queue where to put the result
     */
    public ImageDecoderThread(
            byte[] bytes,
            String location,
            GeneralEnvelope dbTileEnvelope,
            GeneralEnvelope requestEnvelope,
            ImageLevelInfo levelInfo,
            LinkedBlockingQueue<TileQueueElement> tileQueue) {
        this.tileQueue = tileQueue;
        this.requestEnvelope = requestEnvelope;
        this.levelInfo = levelInfo;
        this.imageBytes = bytes; // maybe it would be better to save a copy
        this.location = location;
        this.dbTileEnvelope = dbTileEnvelope;
    }

    @Override
    public void run() {
        if ((imageBytes == null) || (imageBytes.length == 0)) { // nothing to do
            return;
        }

        try {

            BufferedImage dbTileImage = null;

            boolean triedFromStream = false;
            if (levelInfo.getCanImageIOReadFromInputStream()) {
                dbTileImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                triedFromStream = true;
            }
            if (dbTileImage == null) {
                if (triedFromStream) {
                    LOGGER.warning("Could not read " + location + " from stream, switch to JAI");
                }
                dbTileImage = readImage2(imageBytes);
            }

            if (requestEnvelope.contains(dbTileEnvelope, true)) {
                tileQueue.add(new TileQueueElement(location, dbTileImage, dbTileEnvelope));
            } else {
                GeneralEnvelope intersectionEnvelope = new GeneralEnvelope(dbTileEnvelope);
                intersectionEnvelope.intersect(requestEnvelope);

                // x and y refers here to Image coordinates (so y is inverted wrt Envelope)
                int xmin =
                        (int)
                                (Math.round(
                                        (intersectionEnvelope.getMinimum(0)
                                                        - dbTileEnvelope.getMinimum(0))
                                                / levelInfo.getResX()));
                int ymin =
                        (int)
                                (Math.round(
                                        (dbTileEnvelope.getMaximum(1)
                                                        - intersectionEnvelope.getMaximum(1))
                                                / levelInfo.getResY()));
                int xmax =
                        (int)
                                (Math.round(
                                        (intersectionEnvelope.getMaximum(0)
                                                        - dbTileEnvelope.getMinimum(0))
                                                / levelInfo.getResX()));
                int ymax =
                        (int)
                                (Math.round(
                                        (dbTileEnvelope.getMaximum(1)
                                                        - intersectionEnvelope.getMinimum(1))
                                                / levelInfo.getResY()));
                int width = xmax - xmin;
                int height = ymax - ymin;

                if ((width > 0) && (height > 0)) {

                    BufferedImage clippedImage = dbTileImage.getSubimage(xmin, ymin, width, height);

                    tileQueue.add(
                            new TileQueueElement(location, clippedImage, intersectionEnvelope));
                }
            }
        } catch (IOException ex) {
            LOGGER.severe("Decorde error for tile " + location);
            LOGGER.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Fallback Method, in some jre implementations, ImageIO.read(InputStream in) returns null. If
     * this happens, this method is called, which is not so efficient but it works
     */
    private BufferedImage readImage2(byte[] imageBytes) throws IOException {
        try (SeekableStream stream = new ByteArraySeekableStream(imageBytes)) {
            String decoderName = null;

            for (String dn : ImageCodec.getDecoderNames(stream)) {
                decoderName = dn;
                break;
            }

            ImageDecoder decoder = ImageCodec.createImageDecoder(decoderName, stream, null);
            PlanarImage img = PlanarImage.wrapRenderedImage(decoder.decodeAsRenderedImage());
            return img.getAsBufferedImage();
        }
    }
}
