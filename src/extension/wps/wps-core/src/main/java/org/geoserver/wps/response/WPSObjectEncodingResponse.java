/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.response;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.ows.XmlObjectEncodingResponse;

/**
 * A response designed to encode a specific object into XML
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WPSObjectEncodingResponse extends XmlObjectEncodingResponse {

    public WPSObjectEncodingResponse(Class binding, String elementName, Class xmlConfiguration) {
        super(binding, elementName, xmlConfiguration);
    }

    @Override
    protected Map<String, String> getSchemaLocations() {
        Map<String, String> locations = new HashMap<String, String>();
        locations.put(
                "http://www.opengis.net/wps/1.0.0",
                "http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd");
        return locations;
    }
}
