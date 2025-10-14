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

public class AtomGeoRSSMapOutputFormat implements GetMapOutputFormat {
    /** mime type */
    public static String MIME_TYPE = "application/atom+xml";

    static final MapProducerCapabilities ATOM_CAPABILITIES = new MapProducerCapabilities(false, false, true);

    /** format names/aliases */
    public static final Set<String> FORMAT_NAMES;

    static {
        String[] FORMATS = {MIME_TYPE, "atom", "application/atom xml"};
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(Arrays.asList(FORMATS));
        FORMAT_NAMES = Collections.unmodifiableSet(names);
    }

    private WMS wms;

    public AtomGeoRSSMapOutputFormat(WMS wms) {
        this.wms = wms;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#getMimeType() */
    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    /** @see GetMapOutputFormat#getOutputFormatNames() */
    @Override
    public Set<String> getOutputFormatNames() {
        return FORMAT_NAMES;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent) */
    @Override
    public XMLTransformerMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {

        AtomGeoRSSTransformer tx = new AtomGeoRSSTransformer(wms);
        GetMapRequest request = mapContent.getRequest();

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

        XMLTransformerMap result = new XMLTransformerMap(mapContent, tx, mapContent, getMimeType());
        return result;
    }

    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return ATOM_CAPABILITIES;
    }
}
