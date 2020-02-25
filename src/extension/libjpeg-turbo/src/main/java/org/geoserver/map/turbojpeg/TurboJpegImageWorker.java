/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.map.turbojpeg;

import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegImageWriteParam;
import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegImageWriter;
import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegImageWriterSpi;
import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;
import it.geosolutions.imageio.utilities.ImageOutputStreamAdapter2;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.spi.ImageOutputStreamSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;
import org.geotools.util.logging.Logging;

/**
 * Specific subclass of {@link ImageWorker} for writing JPEG images using libjpeg-turbo.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
final class TurboJpegImageWorker extends ImageWorker {

    static final String ERROR_LIB_MESSAGE =
            "The TurboJpeg native library hasn't been loaded: Skipping";

    static final String ERROR_FILE_MESSAGE = "The specified input file can't be read: Skipping";

    /** Is the libjpeg-turbo available? * */
    private static final TurboJpegImageWriterSpi TURBO_JPEG_SPI = new TurboJpegImageWriterSpi();

    /** The logger to use for this class. */
    private static final Logger LOGGER = Logging.getLogger(TurboJpegImageWorker.class);

    public TurboJpegImageWorker() {
        super();
    }

    public TurboJpegImageWorker(File input) throws IOException {
        super(input);
    }

    public TurboJpegImageWorker(RenderedImage image) {
        super(image);
    }

    /**
     * Writes outs the image contained into this {@link ImageWorker} as a JPEG using the provided
     * destination , compression and compression rate.
     *
     * <p>The destination object can be anything providing that we have an {@link
     * ImageOutputStreamSpi} that recognizes it.
     *
     * @param destination where to write the internal {@link #image} as a JPEG.
     * @param compressionRate percentage of compression.
     * @throws IOException In case an error occurs during the search for an {@link
     *     ImageOutputStream} or during the eoncding process.
     */
    public final void writeTurboJPEG(final OutputStream destination, final float compressionRate)
            throws IOException {

        if (!TurboJpegUtilities.isTurboJpegAvailable()) {
            throw new IllegalStateException(ERROR_LIB_MESSAGE);
        }

        // Reformatting this image for jpeg.
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Encoding input image to write out as JPEG using .");

        // go to component color model if needed
        ColorModel cm = image.getColorModel();
        forceComponentColorModel(false, true, true);
        final boolean hasAlpha = cm.hasAlpha();
        cm = image.getColorModel();

        // rescale to 8 bit
        rescaleToBytes();
        cm = image.getColorModel();

        // remove transparent band
        final int numBands = image.getSampleModel().getNumBands();
        if (hasAlpha) {
            final int requestedBands = numBands - 1;
            if (ImageUtilities.isMediaLibAvailable()) {
                retainBands(requestedBands);
            } else if (getNumBands() > requestedBands) {
                removeAlpha(requestedBands);
            }
        }

        // Getting a writer.
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Creating a TURBO JPEG writer and configuring it.");

        final TurboJpegImageWriter writer =
                (TurboJpegImageWriter) TURBO_JPEG_SPI.createWriterInstance();
        // Compression is available on both lib
        TurboJpegImageWriteParam iwp = (TurboJpegImageWriteParam) writer.getDefaultWriteParam();
        final ImageOutputStreamAdapter2 outStream = new ImageOutputStreamAdapter2(destination);
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionType("JPEG");
        iwp.setCompressionQuality(compressionRate); // We can control quality here.

        if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Writing image out...");

        try {
            writer.setOutput(outStream);
            writer.write(null, new IIOImage(image, null, null), iwp);
        } finally {
            try {
                writer.dispose();
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            }
            try {
                outStream.close();
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            }
        }
    }

    /** Remove the alpha band */
    private void removeAlpha(int requestedBands) {
        // Retrieving/Setting the ImageLayout
        final RenderingHints hints = getRenderingHints();
        ImageLayout layout = null;
        if (hints.containsKey(JAI.KEY_IMAGE_LAYOUT)) {
            layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
        } else {
            layout = new ImageLayout();
            hints.put(JAI.KEY_IMAGE_LAYOUT, layout);
        }

        // Forcing the colormodel with noAlpha
        final ColorModel colorModel =
                new ComponentColorModel(
                        ColorSpace.getInstance(
                                requestedBands == 3 ? ColorSpace.CS_sRGB : ColorSpace.CS_GRAY),
                        false,
                        false,
                        Transparency.OPAQUE,
                        DataBuffer.TYPE_BYTE);
        SampleModel sm =
                colorModel.createCompatibleSampleModel(image.getWidth(), image.getHeight());
        layout.setSampleModel(sm);

        // Forcing the output format to remove the alpha Band
        ImageWorker worker = new ImageWorker(image);
        worker.setRenderingHints(hints);
        image = worker.format(DataBuffer.TYPE_BYTE).getRenderedImage();
    }
}
