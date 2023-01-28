/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
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

/**
 * Map response handler for WEBP image format.
 *
 * @author Martin Over
 */
public final class WEBPMapResponse extends RenderedImageMapResponse {

    /** Logger. */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(WEBPMapResponse.class.toString());

    private static MapProducerCapabilities CAPABILITIES =
            new MapProducerCapabilities(true, true, true);

    /** the only MIME type this map producer supports */
    private static final String MIME_TYPE = "image/webp";

    private static final ImageWriterSpi writerSPI =
            new com.luciad.imageio.webp.WebPImageWriterSpi();

    public WEBPMapResponse(WMS wms) {
        super(MIME_TYPE, wms);
    }

    @Override
    public void formatImageOutputStream(
            RenderedImage image, OutputStream outStream, WMSMapContent mapContent)
            throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("About to write a WEBP image.");
        }
        image = applyPalette(image, mapContent, MIME_TYPE, true);
        final ImageWriter writer = writerSPI.createWriterInstance();

        try (final ImageOutputStream ioutstream =
                ImageIOExt.createImageOutputStream(image, outStream)) {
            if (ioutstream == null)
                throw new ServiceException("Unable to create ImageOutputStream.");
            writer.setOutput(ioutstream);
            writer.write(image);
        } finally {
            try {

                writer.dispose();
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.log(Level.FINEST, "Unable to properly dispose writer", e);
            }

            RasterCleaner.addImage(image);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing a WEBP done!!!");
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }

    @Override
    public String getExtension(RenderedImage image, WMSMapContent mapContent) {
        return "webp";
    }
}
