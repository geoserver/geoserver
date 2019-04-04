/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.image.io.ImageIOExt;
import org.geotools.util.logging.Logging;

/**
 * Map response to encode Tiff images out of a map.
 *
 * @author Simone Giannecchini
 * @since 1.4.x
 */
public final class TIFFMapResponse extends RenderedImageMapResponse {

    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(TIFFMapResponse.class);

    private static final ImageWriterSpi writerSPI =
            new it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriterSpi();

    /** the only MIME type this map producer supports */
    private static final String MIME_TYPE = "image/tiff";

    private static final String IMAGE_TIFF8 = "image/tiff8";

    private static final String[] OUTPUT_FORMATS = {MIME_TYPE, IMAGE_TIFF8};

    /**
     * Default capabilities for TIFF format.
     *
     * <p>
     *
     * <ol>
     *   <li>tiled = supported
     *   <li>multipleValues = unsupported
     *   <li>paletteSupported = supported
     *   <li>transparency = supported
     * </ol>
     */
    private static MapProducerCapabilities CAPABILITIES =
            new MapProducerCapabilities(true, false, true, true, null);

    /**
     * Creates a {@link GetMapProducer} to encode the {@link RenderedImage} generated in <code>
     * outputFormat</code> format.
     *
     * @param wms service facade
     */
    public TIFFMapResponse(WMS wms) {
        super(OUTPUT_FORMATS, wms);
    }

    /**
     * Transforms the rendered image into the appropriate format, streaming to the output stream.
     *
     * @param image The image to be formatted.
     * @param outStream The stream to write to.
     * @throws ServiceException not really.
     * @throws IOException if the image writing fails.
     */
    public void formatImageOutputStream(
            RenderedImage image, OutputStream outStream, WMSMapContent mapContent)
            throws ServiceException, IOException {
        // getting a writer
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting a writer for tiff");
        }

        // get a writer
        final ImageWriter writer = writerSPI.createWriterInstance();

        // getting a stream caching in memory
        final ImageOutputStream ioutstream = ImageIOExt.createImageOutputStream(image, outStream);
        if (ioutstream == null) throw new ServiceException("Unable to create ImageOutputStream.");

        // tiff
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing tiff image ...");
        }

        // do we want it to be 8 bits?
        image = applyPalette(image, mapContent, IMAGE_TIFF8, false);

        // write it out
        try {
            writer.setOutput(ioutstream);
            writer.write(image);
        } finally {
            try {
                ioutstream.close();
            } catch (Throwable e) {
                // eat exception to release resources silently
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.log(Level.FINEST, "Unable to properly close output stream", e);
            }

            try {

                writer.dispose();
            } catch (Throwable e) {
                // eat exception to release resources silently
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.log(Level.FINEST, "Unable to properly dispose writer", e);
            }

            // let go of the image
            RasterCleaner.addImage(image);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing tiff image done!");
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }

    @Override
    public String getExtension(RenderedImage image, WMSMapContent mapContent) {
        return "tif";
    }
}
