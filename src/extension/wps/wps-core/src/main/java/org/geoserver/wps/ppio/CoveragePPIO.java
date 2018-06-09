/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.awt.image.RenderedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

/**
 * Process parameter input / output for GridCoverage on a specific mime type. Current implementation
 * only supports PNG/JPEG encoding.
 */
public abstract class CoveragePPIO extends BinaryPPIO {

    private static float DEFAULT_QUALITY = 0.75f;

    private static final Logger LOGGER = Logging.getLogger(CoveragePPIO.class);

    protected CoveragePPIO(final String mimeType) {
        super(GridCoverage2D.class, GridCoverage2D.class, mimeType);
    }

    @Override
    public void encode(Object value, OutputStream outputStream) throws Exception {
        // Call default implementation with no params
        encode(value, null, outputStream);
    }

    private static float extractQuality(Map<String, Object> encodingParameters) {
        float quality = DEFAULT_QUALITY;
        if (encodingParameters != null
                && !encodingParameters.isEmpty()
                && encodingParameters.containsKey(QUALITY_KEY)) {
            String compressionQuality = (String) encodingParameters.get(QUALITY_KEY);
            try {
                quality = Float.parseFloat(compressionQuality);
            } catch (NumberFormatException nfe) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(
                            "Specified quality is not valid (it should be in the range [0,1])."
                                    + " quality = "
                                    + compressionQuality
                                    + "\nUsing default Quality: "
                                    + DEFAULT_QUALITY);
                }
            }
        }
        return quality;
    }

    /**
     * GridCoverage2D to PNG encoding PPIO. Note that we cannot decode a GridCoverage2D out of a
     * pure PNG Image. Report this by overriding the getDirection method and throwing an
     * UnsupportedOperationException on a decode call.
     */
    public static class PNGPPIO extends CoveragePPIO {

        public PNGPPIO() {
            super("image/png");
        }

        @Override
        public void encode(
                Object value, Map<String, Object> encodingParameters, OutputStream outputStream)
                throws Exception {
            GridCoverage2D gridCoverage = (GridCoverage2D) value;
            RenderedImage renderedImage = gridCoverage.getRenderedImage();
            ImageWorker worker = new ImageWorker(renderedImage);
            float quality = extractQuality(encodingParameters);
            worker.writePNG(outputStream, "FILTERED", quality, false, false);
        }

        @Override
        public String getFileExtension() {
            return "png";
        }

        @Override
        public PPIODirection getDirection() {
            return PPIODirection.ENCODING;
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            // ComplexPPIO requires overriding the decode method
            throw new UnsupportedOperationException();
        }
    }

    /**
     * GridCoverage2D to JPEG encoding PPIO. Note that we cannot decode a GridCoverage2D out of a
     * pure JPEG Image. Report this by overriding the getDirection method and throwing an
     * UnsupportedOperationException on a decode call.
     */
    public static class JPEGPPIO extends CoveragePPIO {

        public JPEGPPIO() {
            super("image/jpeg");
        }

        @Override
        public void encode(
                Object value, Map<String, Object> encodingParameters, OutputStream outputStream)
                throws Exception {
            GridCoverage2D gridCoverage = (GridCoverage2D) value;
            RenderedImage renderedImage = gridCoverage.getRenderedImage();
            ImageWorker worker = new ImageWorker(renderedImage);
            float quality = extractQuality(encodingParameters);
            worker.writeJPEG(outputStream, "JPEG", quality, false);
        }

        @Override
        public String getFileExtension() {
            return "jpeg";
        }

        @Override
        public PPIODirection getDirection() {
            return PPIODirection.ENCODING;
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            // ComplexPPIO requires overriding the decode method
            throw new UnsupportedOperationException();
        }
    }
}
