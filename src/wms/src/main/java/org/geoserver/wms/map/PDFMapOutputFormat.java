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

/**
 * Handles a GetMap request that expects a map in PDF format.
 *
 * @author Pierre-Emmanuel Balageas, ALCER (http://www.alcer.com)
 * @author Simone Giannecchini - GeoSolutions
 * @author Gabriel Roldan
 */
public class PDFMapOutputFormat extends AbstractMapOutputFormat {
    public PDFMapOutputFormat() {
        super(MIME_TYPE);
    }

    public PDFMapOutputFormat(WMS wms) {
        super(MIME_TYPE);
        this.wms = wms;
    }
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
            new MapProducerCapabilities(false, false, true);

    public static class PDFMap extends org.geoserver.wms.WebMap {

        public MapDecorationLayout layout;

        public PDFMap(final WMSMapContent mapContent) {
            super(mapContent);
        }

        public WMSMapContent getContext() {
            return mapContent;
        }
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent) */
    @Override
    public PDFMap produceMap(final WMSMapContent mapContent) throws ServiceException, IOException {

        PDFMap result = new PDFMap(mapContent);
        // TODO - can we find out if we are tiled here?
        MapDecorationLayout layout = findDecorationLayout(mapContent.getRequest(), false);
        result.layout = layout;
        result.setContentDispositionHeader(mapContent, ".pdf");
        result.setMimeType(MIME_TYPE);
        return result;
    }

    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return CAPABILITIES;
    }
}
