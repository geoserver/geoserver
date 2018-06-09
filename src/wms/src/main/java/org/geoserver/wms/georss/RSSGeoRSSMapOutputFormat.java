/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.georss;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.XMLTransformerMap;

public class RSSGeoRSSMapOutputFormat implements GetMapOutputFormat {

    /** the actual mime type for the response header */
    private static String MIME_TYPE = "application/rss+xml";

    static final MapProducerCapabilities RSS_CAPABILITIES =
            new MapProducerCapabilities(false, false, false, true, null);

    /** format names/aliases */
    public static final Set<String> FORMAT_NAMES;

    static {
        String[] FORMATS = {"application/rss+xml", "rss", "application/rss xml"};
        Set<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(Arrays.asList(FORMATS));
        FORMAT_NAMES = Collections.unmodifiableSet(names);
    }

    private WMS wms;

    public RSSGeoRSSMapOutputFormat(WMS wms) {
        this.wms = wms;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#getMimeType() */
    public String getMimeType() {
        return MIME_TYPE;
    }

    /** @see GetMapProducer#getOutputFormatNames() */
    public Set<String> getOutputFormatNames() {
        return FORMAT_NAMES;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent) */
    public XMLTransformerMap produceMap(WMSMapContent map) throws ServiceException, IOException {

        RSSGeoRSSTransformer tx = new RSSGeoRSSTransformer(wms);
        GetMapRequest request = map.getRequest();

        String geometryEncoding = (String) request.getFormatOptions().get("encoding");
        if ("gml".equals(geometryEncoding)) {
            tx.setGeometryEncoding(GeoRSSTransformerBase.GeometryEncoding.GML);
        } else if ("latlong".equals(geometryEncoding)) {
            tx.setGeometryEncoding(GeoRSSTransformerBase.GeometryEncoding.LATLONG);
        } else {
            tx.setGeometryEncoding(GeoRSSTransformerBase.GeometryEncoding.SIMPLE);
        }

        Charset encoding = wms.getCharSet();
        tx.setEncoding(encoding);

        XMLTransformerMap result = new XMLTransformerMap(map, tx, map, getMimeType());

        // REVISIT: is was setting "inline; filename=geoserver.xml", now it's gonna be the requested
        // layer names, is it ok?
        result.setContentDispositionHeader(map, ".xml");
        return result;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return RSS_CAPABILITIES;
    }
}
