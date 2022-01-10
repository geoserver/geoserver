/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind.util;

import java.awt.*;
import java.awt.image.RasterFormatException;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.media.jai.*;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;
import javax.media.jai.registry.RenderedRegistryMode;
import org.geotools.image.TransfertRectIter;
import org.geotools.metadata.i18n.LoggingKeys;
import org.geotools.metadata.i18n.Loggings;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.operation.TransformException;

/**
 * An image operation that translates pixels in a raster to a different value. The intended use case
 * is to recode no data values to a different value (e.g. translate -32768 to -9999), but the
 * operation is not limited to recoding no data.
 *
 * @author Parker Abercrombie
 */
public class RecodeRaster extends PointOpImage {
    private static final Logger LOGGER = Logging.getLogger(RecodeRaster.class);

    /** The operation name. */
    public static final String OPERATION_NAME = "org.geotools.RecodeNoData";

    /** No data value in the source image. */
    private final double srcVal;

    /** No data value in the target image. Pixels equal to srcVal will be recoded to this value. */
    private final double destVal;

    public RecodeRaster(
            final RenderedImage image,
            final double srcVal,
            final double destVal,
            final RenderingHints hints) {
        super(image, (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT), hints, false);

        this.srcVal = srcVal;
        this.destVal = destVal;
        permitInPlaceOperation();
    }

    @Override
    protected void computeRect(
            final PlanarImage[] sources, final WritableRaster dest, final Rectangle destRect) {
        final PlanarImage source = sources[0];
        final Rectangle bounds = destRect.intersection(source.getBounds());
        if (!destRect.equals(bounds)) {
            // TODO: Check if this case occurs sometime, and fill pixel values if it does.
            //       If it happen to occurs, we will need to fix other GeoTools operations
            //       as well.
            Logging.getLogger(TransformException.class)
                    .warning("Bounds mismatch: " + destRect + " and " + bounds);
        }
        WritableRectIter iterator = RectIterFactory.createWritable(dest, bounds);

        // TODO: Detect if source and destination rasters are the same. If they are
        //       the same, we should skip this block. Iteration will then be faster.
        iterator = TransfertRectIter.create(RectIterFactory.create(source, bounds), iterator);

        if (!iterator.finishedBands()) {
            do {
                recode(iterator);
            } while (!iterator.nextBandDone());
        }
    }

    private void recode(WritableRectIter iterator) throws RasterFormatException {
        iterator.startLines();
        if (!iterator.finishedLines()) {
            do {
                iterator.startPixels();
                if (!iterator.finishedPixels()) {
                    do {
                        double value = iterator.getSampleDouble();
                        if (value == srcVal) {
                            iterator.setSample(destVal);
                        } else {
                            iterator.setSample(value);
                        }
                    } while (!iterator.nextPixelDone());
                }
            } while (!iterator.nextLineDone());
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    ////////                                                                 ////////
    ////////           REGISTRATION OF "Recode" IMAGE OPERATION              ////////
    ////////                                                                 ////////
    /////////////////////////////////////////////////////////////////////////////////

    /**
     * The operation descriptor for the "RecodeNoData" operation. This operation translates pixels
     * equal to the no data value to a different value.
     */
    private static final class Descriptor extends OperationDescriptorImpl {
        /** Construct the descriptor. */
        public Descriptor() {
            super(
                    new String[][] {
                        {"GlobalName", OPERATION_NAME},
                        {"LocalName", OPERATION_NAME},
                        {"Vendor", "Geotools 2"},
                        {"Description", "Translate pixels from one value to another."},
                        {"DocURL", "http://www.geotools.org/"},
                        {"Version", "1.0"}
                    },
                    new String[] {RenderedRegistryMode.MODE_NAME},
                    1,
                    new String[] {"srcVal", "destVal"}, // Argument names
                    new Class[] {Number.class, Number.class}, // Argument classes
                    new Object[] {NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT}, // Default values
                    null // No restriction on valid parameter values.
                    );
        }
    }

    /**
     * The {@link java.awt.image.renderable.RenderedImageFactory} for the {@code "RecodeNoData"}
     * operation.
     */
    private static final class CRIF extends CRIFImpl {
        /**
         * Creates a {@link RenderedImage} representing the results of an imaging operation for a
         * given {@link ParameterBlock} and {@link RenderingHints}.
         */
        @Override
        public RenderedImage create(final ParameterBlock param, final RenderingHints hints) {
            final RenderedImage image = (RenderedImage) param.getSource(0);
            final Number srcVal = (Number) param.getObjectParameter(0);
            final Number destVal = (Number) param.getObjectParameter(1);

            return new RecodeRaster(image, srcVal.doubleValue(), destVal.doubleValue(), hints);
        }
    }

    /**
     * Register the "RecodeNoData" image operation to the operation registry of the specified JAI
     * instance.
     */
    public static void register(final JAI jai) {
        final OperationRegistry registry = jai.getOperationRegistry();
        try {
            registry.registerDescriptor(new Descriptor());
            registry.registerFactory(
                    RenderedRegistryMode.MODE_NAME, OPERATION_NAME, "geotools.org", new CRIF());
        } catch (IllegalArgumentException exception) {
            final LogRecord record =
                    Loggings.format(
                            Level.SEVERE,
                            LoggingKeys.CANT_REGISTER_JAI_OPERATION_$1,
                            OPERATION_NAME);
            record.setSourceMethodName("<classinit>");
            record.setThrown(exception);
            record.setLoggerName(LOGGER.getName());
            LOGGER.log(record);
        }
    }
}
