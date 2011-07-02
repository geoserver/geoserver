/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.geosearch.rest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.XMLTransformerMap;
import org.geotools.util.logging.Logging;

/**
 * {@link GetMapOutputFormat} local to this module used to set up a
 * {@link KMLMetadataDocumentTransformer} as the result of a locally made GetMap request
 */
class KMLMetadataDocumentMapOutputFormat implements GetMapOutputFormat {
    /** standard logger */
    protected static final Logger LOGGER = Logging
            .getLogger(KMLMetadataDocumentMapOutputFormat.class);
    
    static final MapProducerCapabilities KML_CAPABILITIES = new MapProducerCapabilities(false, false, true, true);

    /**
     * Official KML mime type
     */
    public static final String MIME_TYPE = "application/vnd.google-earth.kml+xml;mode=metadata";

    private Set<String> OUTPUT_FORMATS = Collections.unmodifiableSet(Collections
            .singleton(MIME_TYPE));

    private WMS wms;

    public KMLMetadataDocumentMapOutputFormat(WMS wms) {
        this.wms = wms;
    }

    /**
     * @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames()
     */
    public Set<String> getOutputFormatNames() {
        return OUTPUT_FORMATS;
    }

    /**
     * @return {@code "application/vnd.google-earth.kml+xml"}
     * @see org.geoserver.wms.GetMapOutputFormat#getMimeType()
     */
    public String getMimeType() {
        return MIME_TYPE;
    }

    /**
     * Produce the actual map ready for outputing.
     * 
     * @param map
     *            WMSMapContext describing what layers, styles, area of interest etc are to be used
     *            when producing the map.
     * 
     * @see GetMapOutputFormat#produceMap(WMSMapContent)
     */
    public XMLTransformerMap produceMap(WMSMapContent mapContent) throws ServiceException,
            IOException {

        KMLMetadataDocumentTransformer transformer = new KMLMetadataDocumentTransformer(wms);

        transformer.setIndentation(2);
        Charset encoding = wms.getCharSet();
        transformer.setEncoding(encoding);

        XMLTransformerMap map = new XMLTransformerMap(mapContent, transformer, mapContent,
                MIME_TYPE);

        return map;
    }
    
    public MapProducerCapabilities getCapabilities(String format) {
        return KML_CAPABILITIES;
    }
}
