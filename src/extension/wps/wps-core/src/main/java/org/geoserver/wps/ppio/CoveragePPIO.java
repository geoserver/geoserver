/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.awt.image.RenderedImage;
import java.io.InputStream;
import java.io.OutputStream;

import org.geotools.coverage.grid.GridCoverage2D;

import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codecimpl.JPEGImageEncoder;
import com.sun.media.jai.codecimpl.PNGImageEncoder;

/**
 * Process parameter input / output for GridCoverage on a specific mime type. 
 * Current implementation only supports PNG/JPEG encoding.
 */
public abstract class CoveragePPIO extends BinaryPPIO {

    protected CoveragePPIO(final String mimeType) {
        super(GridCoverage2D.class, GridCoverage2D.class, mimeType);
    }

    public abstract ImageEncoder getEncoder(OutputStream os);

    @Override
    public void encode(Object value, OutputStream outputStream) throws Exception {
        GridCoverage2D gridCoverage = (GridCoverage2D) value;
        RenderedImage renderedImage = gridCoverage.getRenderedImage();
        ImageEncoder encoder = getEncoder(outputStream);
        encoder.encode(renderedImage);
    }

    /**
     * GridCoverage2D to PNG encoding PPIO.
     * Note that we cannot decode a GridCoverage2D out of a pure PNG Image.
     * Report this by overriding the getDirection method and throwing an 
     * UnsupportedOperationException on a decode call.
     */
    public static class PNGPPIO extends CoveragePPIO {

        public PNGPPIO() {
            super("image/png");
        }

        @Override
        public final ImageEncoder getEncoder(OutputStream outputStream) {
            return new PNGImageEncoder(outputStream, null);
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
     * GridCoverage2D to JPEG encoding PPIO.
     * Note that we cannot decode a GridCoverage2D out of a pure JPEG Image.
     * Report this by overriding the getDirection method and throwing an 
     * UnsupportedOperationException on a decode call.
     */
    public static class JPEGPPIO extends CoveragePPIO {

        public JPEGPPIO() {
            super("image/jpeg");
        }

        @Override
        public ImageEncoder getEncoder(OutputStream outputStream) {
            return new JPEGImageEncoder(outputStream, null);
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
