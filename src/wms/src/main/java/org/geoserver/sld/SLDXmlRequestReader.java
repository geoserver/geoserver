/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sld;

import java.io.Reader;
import java.util.Map;
import org.geoserver.catalog.Styles;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.map.ProcessStandaloneSLDVisitor;
import org.geotools.styling.StyledLayerDescriptor;

/**
 * Reads
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class SLDXmlRequestReader extends XmlRequestReader {

    private WMS wms;

    public SLDXmlRequestReader(WMS wms) {
        super("http://www.opengis.net/sld", "StyledLayerDescriptor");
        this.wms = wms;
    }

    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("request must be not null");
        }

        GetMapRequest getMap = (GetMapRequest) request;
        StyledLayerDescriptor sld =
                Styles.handler(getMap.getStyleFormat())
                        .parse(reader, getMap.styleVersion(), null, null);

        // process the sld
        sld.accept(new ProcessStandaloneSLDVisitor(wms, getMap));
        // GetMapKvpRequestReader.processStandaloneSld(wms, getMap, sld);

        return getMap;
    }
}
