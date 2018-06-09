/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.ows.XmlObjectEncodingResponse;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geotools.filter.v1_1.OGC;
import org.geotools.xlink.XLINK;
import org.geotools.xml.Encoder;

/**
 * A response designed to encode a specific object into XML
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CSWObjectEncodingResponse extends XmlObjectEncodingResponse {

    public CSWObjectEncodingResponse(
            Class<?> binding, String elementName, Class<?> xmlConfiguration) {
        super(binding, elementName, xmlConfiguration);
    }

    @Override
    protected Map<String, String> getSchemaLocations() {
        Map<String, String> locations = new HashMap<String, String>();
        locations.put(
                "http://www.opengis.net/cat/csw/2.0.2",
                "http://schemas.opengis.net/csw/2.0.2/CSW-discovery.xsd");
        return locations;
    }

    @Override
    protected void configureEncoder(
            Encoder encoder, String elementName, Class<?> xmlConfiguration) {
        encoder.setNamespaceAware(true);
        encoder.getNamespaces().declarePrefix("ows", OWS.NAMESPACE);
        encoder.getNamespaces().declarePrefix("ogc", OGC.NAMESPACE);
        encoder.getNamespaces().declarePrefix("gml", "http://www.opengis.net/gml");
        encoder.getNamespaces().declarePrefix("gmd", "http://www.isotc211.org/2005/gmd");
        encoder.getNamespaces().declarePrefix("xlink", XLINK.NAMESPACE);
    }
}
