/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.geoserver.config.GeoServer;
import org.geotools.feature.FeatureCollection;

public class DefaultOpenSearchEoService implements OpenSearchEoService {

    GeoServer geoServer;

    public DefaultOpenSearchEoService(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public OSEODescription description(OSEODescriptionRequest request) {
        // TODO: provide a list of searchable parameters based on the chosen collection
        return new OSEODescription(request, geoServer.getService(OSEOInfo.class), geoServer.getGlobal());
    }

    @Override
    public FeatureCollection search(SearchRequest request) {
        throw new UnsupportedOperationException();
    }

}
