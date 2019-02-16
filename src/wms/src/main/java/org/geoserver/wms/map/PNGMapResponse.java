/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.JAIInfo;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.png.PNGJWriter;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

/**
 * Handles a GetMap request that spects a map in GIF format.
 *
 * @author Simone Giannecchini
 * @author Didier Richard
 * @version $Id
 */
public class PNGMapResponse extends RenderedImageMapResponse {
    /** Logger */
    private static final Logger LOGGER = Logging.getLogger(PNGMapResponse.class);

    private static final String MIME_TYPE = "image/png";

    private static final String MIME_TYPE_8BIT = "image/png; mode=8bit";

    private static final String[] OUTPUT_FORMATS = {MIME_TYPE, MIME_TYPE_8BIT, "image/png8"};

    /** The two quantizers available for PNG images */
    public enum QuantizeMethod {
        Octree,
        MedianCut
    };

    /**
     * Default capabilities for PNG format.
     *
     * <p>
     *
     * <ol>
     *   <li>tiled = supported
     *   <li>multipleValues = unsupported
     *   <li>paletteSupported = supported
     *   <li>transparency = supported
     * </ol>
     */
    private static MapProducerCapabilities CAPABILITIES =
            new MapProducerCapabilities(true, false, true, true, null);

    public PNGMapResponse(WMS wms) {
        super(OUTPUT_FORMATS, wms);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        GetMapRequest request = (GetMapRequest) operation.getParameters()[0];
        if (request.getFormat().contains("8")) {
            return MIME_TYPE_8BIT;
        } else {
            return MIME_TYPE;
        }
    }

    /**
     * Transforms the rendered image into the appropriate format, streaming to the output stream.
     *
     * @see RasterMapOutputFormat#formatImageOutputStream(RenderedImage, OutputStream)
     */
    public void formatImageOutputStream(
            RenderedImage image, OutputStream outStream, WMSMapContent mapContent)
            throws ServiceException, IOException {
        // /////////////////////////////////////////////////////////////////
        //
        // Reformatting this image for png
        //
        // /////////////////////////////////////////////////////////////////
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing png image ...");
        }

        // check to see if we have to see a translucent or bitmask quantizer
        image = applyPalette(image, mapContent, f -> f != null && f.contains("png8"), true);
        float quality = (100 - wms.getPngCompression()) / 100.0f;
        JAIInfo.PngEncoderType encoder = wms.getPNGEncoderType();
        if (encoder == JAIInfo.PngEncoderType.PNGJ) {
            image = new PNGJWriter().writePNG(image, outStream, quality, mapContent);
            RasterCleaner.addImage(image);
        } else {
            Boolean PNGNativeAcc = (encoder == JAIInfo.PngEncoderType.NATIVE);
            SampleModel sm = image.getSampleModel();
            int numBits = sm.getSampleSize(0);
            // png acceleration only works on 2 bit and 8 bit images, crashes on 4 bits
            boolean nativeAcceleration =
                    PNGNativeAcc.booleanValue() && !(numBits > 1 && numBits < 8);
            ImageWorker iw = new ImageWorker(image);
            iw.writePNG(outStream, "FILTERED", quality, nativeAcceleration, false);
            RasterCleaner.addImage(iw.getRenderedImage());
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing png image ... done!");
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }

    @Override
    public String getExtension(RenderedImage image, WMSMapContent mapContent) {
        return "png";
    }
}
