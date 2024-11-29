/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import java.util.Map;
import org.geoserver.ogcapi.OGCApiTestSupport;

public class FeaturesTestSupport extends OGCApiTestSupport {

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wfs", "http://www.opengis.net/wfs/3.0");
        namespaces.put("sld", "http://www.opengis.net/sld");
    }
}
