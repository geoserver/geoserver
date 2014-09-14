/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.IOException;
import java.nio.charset.Charset;

import org.geoserver.kml.KMZMapResponse.KMZMap;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.AbstractMapOutputFormat;

/**
 * Handles a GetMap request that spects a map in KMZ format.
 * 
 * KMZ files are a zipped KML file. The KML file must have an emcompasing <document> or <folder>
 * element. So if you have many different placemarks or ground overlays, they all need to be
 * contained within one <document> element, then zipped up and sent off with the extension "kmz".
 * 
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $
 * @author $Author: Brent Owens
 * @author Justin Deoliveira
 * 
 */
public class KMZMapOutputFormat extends AbstractMapOutputFormat {
    /**
     * Official KMZ mime type
     */
    static final String MIME_TYPE = "application/vnd.google-earth.kmz";

    public static final String[] OUTPUT_FORMATS = { MIME_TYPE,
            "application/vnd.google-earth.kmz+xml", "kmz", "application/vnd.google-earth.kmz xml" };

    private WMS wms;

    public KMZMapOutputFormat(WMS wms) {
        super(MIME_TYPE, OUTPUT_FORMATS);
        this.wms = wms;
    }

    /**
     * Initializes the KML encoder. None of the map production is done here, it is done in
     * writeTo(). This way the output can be streamed directly to the output response and not
     * written to disk first, then loaded in and then sent to the response.
     * 
     * @param mapContent
     *            WMSMapContext describing what layers, styles, area of interest etc are to be used
     *            when producing the map.
     * @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent)
     */
    public KMZMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        KMLTransformer transformer = new KMLTransformer(wms);
        transformer.setKmz(true);
        Charset encoding = wms.getCharSet();
        transformer.setEncoding(encoding);
        // TODO: use GeoServer.isVerbose() to determine if we should indent?
        transformer.setIndentation(3);

        KMZMap map = new KMZMap(mapContent, transformer, MIME_TYPE);
        map.setContentDispositionHeader(mapContent, ".kmz");

        return map;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return KMLMapOutputFormat.KML_CAPABILITIES;
    }
}
