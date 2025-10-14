/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import org.geoserver.wps.WPSException;

public abstract class ImagePPIO extends BinaryPPIO {

    protected ImagePPIO(final String mimeType) {
        super(RenderedImage.class, RenderedImage.class, mimeType);
    }

    public ImageWriter getWriter() {
        return ImageIO.getImageWritersBySuffix(getFileExtension()).next();
    }

    public ImageReader getReader() {
        return ImageIO.getImageReadersBySuffix("PNG").next();
    }

    @Override
    public void encode(Object value, OutputStream outputStream) throws Exception {
        RenderedImage renderedImage = (RenderedImage) value;
        ImageWriter encoder = getWriter();
        encoder.setOutput(outputStream);
        encoder.write(renderedImage);
    }

    @Override
    public Object decode(InputStream inputStream) throws Exception {
        ImageReader decoder = getReader();
        RenderedImage ri = null;
        try {
            decoder.setInput(inputStream);
            ri = decoder.read(0);
        } catch (IOException ioe) {
            return new WPSException("Unable to decode the image. Expected an image having mimetype = " + mimeType, ioe);
        }
        return ri;
    }

    public static class PNGPPIO extends ImagePPIO {

        public PNGPPIO() {
            super("image/png");
        }

        @Override
        public String getFileExtension() {
            return "png";
        }
    }

    public static class JPEGPPIO extends ImagePPIO {

        public JPEGPPIO() {
            super("image/jpeg");
        }

        @Override
        public String getFileExtension() {
            return "jpeg";
        }
    }
}
