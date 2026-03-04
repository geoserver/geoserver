/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geoserver.wms.map.png.PNGJWriter;
import org.geotools.util.logging.Logging;

/** A Response containing a PngWind Map response */
public class PngWindMapResponse extends RenderedImageMapResponse {

    public PngWindMapResponse(WMS wms) {
        super(PngWindConstants.OUTPUT_FORMATS, wms);
    }
    private static final Logger LOGGER = Logging.getLogger(PngWindMapResponse.class);

    private static final MapProducerCapabilities CAPABILITIES = new MapProducerCapabilities(false, false, false);

    @Override
    public void formatImageOutputStream(RenderedImage image, OutputStream outStream, WMSMapContent mapContent) throws ServiceException, IOException {
        Map<String, Object> metadata = mapContent.getMetadata();
        if (!metadata.containsKey(PngWindConstants.METADATA_CTX_KEY)) {
            throw new ServiceException("No metadata context defined for PngWindMapResponse");
        }

        PngWindRequestContext ctx = (PngWindRequestContext) metadata.get(PngWindConstants.METADATA_CTX_KEY);
        PngWindQuantizer.PngWindQuantizedImage out = PngWindQuantizer.quantize(image, ctx);
        RenderedImage rgb = out.getImage();
        Map<String,String> md = out.getMetadata();
        float quality = (100 - wms.getPngCompression()) / 100.0f;
        image = new PNGJWriter().writePNG(rgb, outStream, quality, mapContent, md);
        RasterCleaner.addImage(image);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing png image ... done!");
        }

    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }
}
