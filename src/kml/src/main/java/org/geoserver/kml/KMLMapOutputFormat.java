/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.kml.builder.StreamingKMLBuilder;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.util.logging.Logging;

/**
 * Handles a GetMap request that spects a map in KML format.
 *
 * @author James Macgill
 */
public class KMLMapOutputFormat implements GetMapOutputFormat {
    /** standard logger */
    protected static final Logger LOGGER = Logging.getLogger(KMLMapOutputFormat.class);

    static final MapProducerCapabilities KML_CAPABILITIES =
            new MapProducerCapabilities(false, false, true, true, null);

    /** Official KML mime type */
    public static final String MIME_TYPE = "application/vnd.google-earth.kml+xml";

    /** Format tweaked to force the generation of per layer network links */
    public static final String NL_KML_MIME_TYPE =
            KMLMapOutputFormat.MIME_TYPE + ";mode=networklink";

    private Set<String> OUTPUT_FORMATS =
            Collections.unmodifiableSet(
                    new HashSet<String>(
                            Arrays.asList(
                                    MIME_TYPE, /* NL_KML_MIME_TYPE, */
                                    "application/vnd.google-earth.kml",
                                    "kml",
                                    "application/vnd.google-earth.kml xml")));

    private WMS wms;

    StreamingKMLBuilder builder = new StreamingKMLBuilder();

    public KMLMapOutputFormat(WMS wms) {
        this.wms = wms;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames() */
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
     * Produce the actual map ready for output.
     *
     * @param mapContent WMSMapContext describing what layers, styles, area of interest etc are to
     *     be used when producing the map.
     * @see GetMapOutputFormat#produceMap(WMSMapContent)
     */
    public KMLMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        // initialize the kml encoding context
        KmlEncodingContext context = new KmlEncodingContext(mapContent, wms, false);

        // build the kml document
        Kml kml = builder.buildKMLDocument(context);

        // return the map
        KMLMap map = new KMLMap(mapContent, context, kml, MIME_TYPE);
        map.setContentDispositionHeader(mapContent, ".kml");
        return map;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return KML_CAPABILITIES;
    }
}
