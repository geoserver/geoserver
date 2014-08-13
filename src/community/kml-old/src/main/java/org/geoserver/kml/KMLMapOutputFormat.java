/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
 * Handles a GetMap request that spects a map in KML format.
 * 
 * @author James Macgill
 */
public class KMLMapOutputFormat implements GetMapOutputFormat {
    /** standard logger */
    protected static final Logger LOGGER = Logging.getLogger(KMLMapOutputFormat.class);
    
    static final MapProducerCapabilities KML_CAPABILITIES = new MapProducerCapabilities(false, false, true, true, null);

    /**
     * Official KML mime type
     */
    public static final String MIME_TYPE = "application/vnd.google-earth.kml+xml";

    private Set<String> OUTPUT_FORMATS = Collections.unmodifiableSet(new HashSet<String>(Arrays
            .asList(MIME_TYPE, "application/vnd.google-earth.kml", "kml",
                    "application/vnd.google-earth.kml xml")));

    private WMS wms;

    public KMLMapOutputFormat(WMS wms) {
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

        KMLTransformer transformer = new KMLTransformer(wms);

        // TODO: use GeoServer.isVerbose() to determine if we should indent?
        transformer.setIndentation(3);
        Charset encoding = wms.getCharSet();
        transformer.setEncoding(encoding);

        XMLTransformerMap map = new XMLTransformerMap(mapContent, transformer, mapContent,
                MIME_TYPE);
        map.setContentDispositionHeader(mapContent, ".kml");
        return map;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return KML_CAPABILITIES;
    }
}
