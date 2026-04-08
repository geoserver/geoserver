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
import org.eclipse.imagen.media.viewer.RenderedImageBrowser;
import org.geoserver.platform.ServiceException;
import org.geoserver.pngwind.config.PngWindConfig;
import org.geoserver.pngwind.config.PngWindConfigurator;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geoserver.wms.map.png.PNGJWriter;
import org.geotools.util.logging.Logging;

/**
 * Map response for PNG-WIND format, which transforms and quantizes the rendered image before encoding it as PNG-WIND.
 */
public class PngWindMapResponse extends RenderedImageMapResponse {

    private static final String RASTER_CHAIN_DEBUG_KEY = "wms.raster.enableRasterChainDebug";
    private static final Boolean RASTER_CHAIN_DEBUG = Boolean.getBoolean(RASTER_CHAIN_DEBUG_KEY);

    public PngWindMapResponse(WMS wms) {
        super(PngWindConstants.OUTPUT_FORMATS, wms);
    }

    private static final Logger LOGGER = Logging.getLogger(PngWindMapResponse.class);

    private static final MapProducerCapabilities CAPABILITIES = new MapProducerCapabilities(false, false, false);

    @Override
    public void formatImageOutputStream(RenderedImage image, OutputStream outStream, WMSMapContent mapContent)
            throws ServiceException, IOException {
        Map<String, Object> metadata = mapContent.getMetadata();
        if (!metadata.containsKey(PngWindConstants.METADATA_CTX_KEY)) {
            throw new ServiceException("No metadata context defined for PngWindMapResponse");
        }

        // Extract the request context from the map content metadata, and use it to transform the image if needed, and
        // then quantize it
        PngWindRequestContext ctx = (PngWindRequestContext) metadata.get(PngWindConstants.METADATA_CTX_KEY);
        PngWindConfig config = PngWindConfigurator.getCurrentConfig();
        PngWindTransform transform = new PngWindTransform(config);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Transforming the image to U/V (if needed)");
        }
        PngWindTransform.PngWindTransformResult result = transform.toUV(image, ctx);
        PngWindQuantizer quantizer = new PngWindQuantizer(config);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Quantizing the U/V image");
        }
        PngWindQuantizer.PngWindQuantizedImage out = quantizer.quantize(result, ctx);
        RenderedImage rgb = out.getImage();
        Map<String, String> md = out.getMetadata();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing png image ...");
        }
        float quality = (100 - wms.getPngCompression()) / 100.0f;
        Map<String, String> rawKvp;
        if (RASTER_CHAIN_DEBUG
                && ((rawKvp = mapContent.getRequest().getRawKvp()) != null)
                && Boolean.parseBoolean(rawKvp.get("showchain"))) {
            RenderedImageBrowser.showChain(rgb, false, false, "PNG-WIND Quantized Image", true);
        }

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
