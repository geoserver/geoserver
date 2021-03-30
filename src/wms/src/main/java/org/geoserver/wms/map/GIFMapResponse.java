/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

/**
 * Handles a GetMap request that spects a map in GIF format.
 *
 * @author Didier Richard
 * @author Simone Giannecchini - GeoSolutions
 * @author Alessio Fabiani - GeoSolutions
 * @version $Id
 */
public final class GIFMapResponse extends RenderedImageMapResponse {

    private static final Logger LOGGER = Logging.getLogger(GIFMapResponse.class);

    /** the only MIME type this map producer supports */
    static final String MIME_TYPE = "image/gif";

    static final String[] OUTPUT_FORMATS = {MIME_TYPE};

    /**
     * Default capabilities for GIF .
     *
     * <p>
     *
     * <ol>
     *   <li>tiled = supported
     *   <li>multipleValues = unsupported
     *   <li>paletteSupported = supported
     *   <li>transparency = supported
     * </ol>
     *
     * <p>We should soon support multipage tiff.
     */
    private static MapProducerCapabilities CAPABILITIES =
            new MapProducerCapabilities(true, true, true);

    public GIFMapResponse(WMS wms) {
        super(OUTPUT_FORMATS, wms);
    }

    /**
     * Transforms the rendered image into the appropriate format, streaming to the output stream.
     *
     * @param originalImage The image to be formatted.
     * @param outStream The stream to write to.
     * @throws ServiceException not really.
     * @throws IOException if encoding to <code>outStream</code> fails.
     */
    @Override
    public void formatImageOutputStream(
            RenderedImage originalImage, OutputStream outStream, WMSMapContent mapContent)
            throws ServiceException, IOException {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing gif image ...");
        }

        if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Preparing to write a gif...");
        //
        // Now the magic
        //
        try {
            originalImage = applyPalette(originalImage, mapContent, MIME_TYPE, false);
            ImageWorker iw = new ImageWorker(originalImage);
            iw.writeGIF(outStream, "LZW", 0.75f);
            RasterCleaner.addImage(iw.getRenderedImage());
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    public String getContentDisposition() {
        // can be null
        return null;
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }

    @Override
    public String getExtension(RenderedImage image, WMSMapContent mapContent) {
        return "gif";
    }
}
