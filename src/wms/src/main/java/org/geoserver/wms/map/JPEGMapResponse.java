/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import com.sun.media.imageioimpl.common.PackageUtil;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.image.ImageWorker;

/**
 * Map response handler for JPEG image format.
 *
 * @author Simone Giannecchini
 * @since 1.4.x
 */
public final class JPEGMapResponse extends RenderedImageMapResponse {

    /** Logger. */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(JPEGMapResponse.class.toString());

    private static final boolean CODEC_LIB_AVAILABLE = PackageUtil.isCodecLibAvailable();

    /**
     * Default capabilities for JPEG .
     *
     * <p>
     *
     * <ol>
     *   <li>tiled = supported
     *   <li>multipleValues = unsupported
     *   <li>paletteSupported = false
     *   <li>transparency = false
     * </ol>
     *
     * <p>We should soon support multipage tiff.
     */
    private static MapProducerCapabilities CAPABILITIES =
            new MapProducerCapabilities(true, false, false, false, null);

    /** the only MIME type this map producer supports */
    private static final String MIME_TYPE = "image/jpeg";

    public JPEGMapResponse(WMS wms) {
        super(MIME_TYPE, wms);
    }

    @Override
    public void formatImageOutputStream(
            RenderedImage image, OutputStream outStream, WMSMapContent mapContent)
            throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("About to write a JPEG image.");
        }

        boolean JPEGNativeAcc = wms.getJPEGNativeAcceleration() && CODEC_LIB_AVAILABLE;
        float quality = (100 - wms.getJpegCompression()) / 100.0f;
        ImageWorker iw = new ImageWorker(image);
        iw.writeJPEG(outStream, "JPEG", quality, JPEGNativeAcc);
        RasterCleaner.addImage(iw.getRenderedImage());

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing a JPEG done!!!");
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }

    @Override
    public String getExtension(RenderedImage image, WMSMapContent mapContent) {
        return "jpg";
    }
}
