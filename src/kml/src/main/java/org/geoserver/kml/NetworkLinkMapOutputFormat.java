/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.kml.builder.SimpleNetworkLinkBuilder;
import org.geoserver.kml.builder.SuperOverlayNetworkLinkBuilder;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geotools.util.logging.Logging;

/**
 * TODO: - handle superoverlay and caching
 *
 * @author Andrea Aime - GeoSolutions
 */
public class NetworkLinkMapOutputFormat extends AbstractMapOutputFormat {
    static final Logger LOGGER = Logging.getLogger(NetworkLinkMapOutputFormat.class);

    /** Official KMZ mime type, tweaked to output NetworkLink */
    public static final String KML_MIME_TYPE = KMLMapOutputFormat.MIME_TYPE + ";mode=networklink";

    public static final String KMZ_MIME_TYPE = KMZMapOutputFormat.MIME_TYPE + ";mode=networklink";

    public static final String[] OUTPUT_FORMATS = {KML_MIME_TYPE, KMZ_MIME_TYPE};

    private WMS wms;

    public NetworkLinkMapOutputFormat(WMS wms) {
        super(KML_MIME_TYPE, OUTPUT_FORMATS);
        this.wms = wms;
    }

    /**
     * Initializes the KML encoder. None of the map production is done here, it is done in
     * writeTo(). This way the output can be streamed directly to the output response and not
     * written to disk first, then loaded in and then sent to the response.
     *
     * @param mapContent WMSMapContext describing what layers, styles, area of interest etc are to
     *     be used when producing the map.
     * @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent)
     */
    @SuppressWarnings("rawtypes")
    public KMLMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        GetMapRequest request = mapContent.getRequest();

        // restore normal kml types (no network link mode)
        boolean kmz = false;
        if (NetworkLinkMapOutputFormat.KML_MIME_TYPE.equals(request.getFormat())) {
            request.setFormat(KMLMapOutputFormat.MIME_TYPE);
        } else {
            kmz = true;
            request.setFormat(KMZMapOutputFormat.MIME_TYPE);
        }

        // check the superoverlay modes
        Boolean superoverlay = (Boolean) request.getFormatOptions().get("superoverlay");
        if (superoverlay == null) {
            superoverlay = Boolean.FALSE;
        }

        // build the kml according to the building mode
        Kml kml = null;
        KmlEncodingContext context = new KmlEncodingContext(mapContent, wms, kmz);
        if (superoverlay) {
            kml = new SuperOverlayNetworkLinkBuilder(context).buildKMLDocument();
        } else {
            kml = new SimpleNetworkLinkBuilder(context).buildKMLDocument();
        }

        // build the output map
        String mime = kmz ? KMZMapOutputFormat.MIME_TYPE : KMLMapOutputFormat.MIME_TYPE;
        KMLMap map = new KMLMap(mapContent, null, kml, mime);
        map.setContentDispositionHeader(mapContent, kmz ? ".kmz" : ".kml");
        return map;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return KMLMapOutputFormat.KML_CAPABILITIES;
    }
}
