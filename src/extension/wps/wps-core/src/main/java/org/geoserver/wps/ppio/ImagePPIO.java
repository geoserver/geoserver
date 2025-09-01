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
import org.eclipse.imagen.media.codec.ImageDecoder;
import org.eclipse.imagen.media.codec.ImageEncoder;
import org.eclipse.imagen.media.codecimpl.JPEGImageDecoder;
import org.eclipse.imagen.media.codecimpl.JPEGImageEncoder;
import org.eclipse.imagen.media.codecimpl.PNGImageDecoder;
import org.eclipse.imagen.media.codecimpl.PNGImageEncoder;
import org.geoserver.wps.WPSException;

public abstract class ImagePPIO extends BinaryPPIO {

    protected ImagePPIO(final String mimeType) {
        super(RenderedImage.class, RenderedImage.class, mimeType);
    }

    public abstract ImageEncoder getEncoder(OutputStream os);

    public abstract ImageDecoder getDecoder(InputStream os);

    @Override
    public void encode(Object value, OutputStream outputStream) throws Exception {
        RenderedImage renderedImage = (RenderedImage) value;
        ImageEncoder encoder = getEncoder(outputStream);
        encoder.encode(renderedImage);
    }

    @Override
    public Object decode(InputStream inputStream) throws Exception {
        ImageDecoder decoder = getDecoder(inputStream);
        RenderedImage ri = null;
        try {
            ri = decoder.decodeAsRenderedImage();
        } catch (IOException ioe) {
            WPSException wpse =
                    new WPSException("Unable to decode the image. Expected an image having mimetype = " + mimeType);
            wpse.initCause(ioe);
            throw wpse;
        }
        return ri;
    }

    public static class PNGPPIO extends ImagePPIO {

        public PNGPPIO() {
            super("image/png");
        }

        @Override
        public final ImageEncoder getEncoder(OutputStream outputStream) {
            return new PNGImageEncoder(outputStream, null);
        }

        @Override
        public final ImageDecoder getDecoder(InputStream inputStream) {
            return new PNGImageDecoder(inputStream, null);
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
        public final ImageDecoder getDecoder(InputStream inputStream) {
            return new JPEGImageDecoder(inputStream, null);
        }

        @Override
        public ImageEncoder getEncoder(OutputStream outputStream) {
            return new JPEGImageEncoder(outputStream, null);
        }

        @Override
        public String getFileExtension() {
            return "jpeg";
        }
    }
}
