/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.IOException;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.decoration.MapDecorationLayout;
import org.geoserver.wms.decoration.MapDecorationLayout.Block;

/**
 * Handles a GetMap request that spects a map in PDF format.
 *
 * @author Pierre-Emmanuel Balageas, ALCER (http://www.alcer.com)
 * @author Simone Giannecchini - GeoSolutions
 * @author Gabriel Roldan
 * @version $Id$
 */
public class PDFMapOutputFormat extends AbstractMapOutputFormat {

    /** the only MIME type this map producer supports */
    static final String MIME_TYPE = "application/pdf";

    /**
     * Default capabilities for OpenLayers format.
     *
     * <p>
     *
     * <ol>
     *   <li>tiled = unsupported
     *   <li>multipleValues = unsupported
     *   <li>paletteSupported = unsupported
     *   <li>transparency = supported
     * </ol>
     */
    private static MapProducerCapabilities CAPABILITIES =
            new MapProducerCapabilities(false, false, false, true, null);

    public static class PDFMap extends org.geoserver.wms.WebMap {

        Block watermark;
        WMS wms;
        MapDecorationLayout layout;

        public PDFMap(final WMSMapContent mapContent) {
            super(mapContent);
        }

        public WMSMapContent getContext() {
            return mapContent;
        }

        /** @param watermark */
        public void setWatermark(Block watermark) {
            this.watermark = watermark;
        }

        /** @param wms */
        public void setWMS(WMS wms) {
            this.wms = wms;
        }

        /** @param findDecorationLayout */
        public void setLayout(MapDecorationLayout findDecorationLayout) {
            this.layout = findDecorationLayout;
        }
    }

    public PDFMapOutputFormat() {
        super(MIME_TYPE);
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent) */
    public PDFMap produceMap(final WMSMapContent mapContent, WMS wms)
            throws ServiceException, IOException {
        this.wms = wms;
        PDFMap result = new PDFMap(mapContent);
        result.setWMS(wms);
        // render the watermark
        MapDecorationLayout.Block watermark = getWatermark(wms.getServiceInfo());
        result.setWatermark(watermark);
        result.setLayout(findDecorationLayout(mapContent.getRequest(), false));
        result.setContentDispositionHeader(mapContent, ".pdf");
        result.setMimeType(MIME_TYPE);
        return result;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return CAPABILITIES;
    }
}
