/* (c) 2014-2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.map.turbojpeg;

import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.JPEGMapResponse;
import org.geoserver.wms.map.RenderedImageMapResponse;

/** @author Simone Giannecchini, GeoSolutions SAS */
public class TurboJPEGMapResponse extends RenderedImageMapResponse {

    /** Logger. */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(TurboJPEGMapResponse.class.toString());

    private static final boolean TURBO_JPEG_LIB_AVAILABLE =
            TurboJpegUtilities.isTurboJpegAvailable();

    private static final boolean DISABLE_TURBO = Boolean.getBoolean("disable.turbojpeg");

    public static boolean isDisabled() {
        return DISABLE_TURBO;
    }

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

    private final JPEGMapResponse fallback;

    public TurboJPEGMapResponse(WMS wms) {
        super(MIME_TYPE, wms);
        fallback = new JPEGMapResponse(wms);
        if (!TURBO_JPEG_LIB_AVAILABLE) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(
                        "The turbo jpeg encoder is not available, check the native libs installation");
            }
        } else {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("The turbo jpeg encoder is available for usage");
            }
        }
        if (DISABLE_TURBO) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("The turbo jpeg encoder has been explicitly disabled");
            }
        }
    }

    @Override
    public void formatImageOutputStream(
            RenderedImage image, OutputStream outStream, WMSMapContent mapContent)
            throws IOException {

        // FALLBACK
        if (!TURBO_JPEG_LIB_AVAILABLE || DISABLE_TURBO) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("About to fallback on standard lib as libjpeg-turbi is not available");
            }
            fallback.formatImageOutputStream(image, outStream, mapContent);
            return;
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("About to write a JPEG image using libjpeg-turbo");
        }
        float quality = (100 - wms.getJpegCompression()) / 100.0f;
        TurboJpegImageWorker iw = null;
        try {
            iw = new TurboJpegImageWorker(image);
            iw.writeTurboJPEG(outStream, quality);
        } finally {
            try {
                if (iw != null) {
                    iw.dispose();
                }
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, e.getLocalizedMessage(), e);
                }
            }
        }

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
