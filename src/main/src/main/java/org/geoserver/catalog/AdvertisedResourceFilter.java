/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.AbstractCatalogFilter;

/**
 * Filters out the non advertised layers from the
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class AdvertisedResourceFilter extends AbstractCatalogFilter {

    @Override
    public boolean hideLayer(LayerInfo layer) {
        if (!layer.isAdvertised()) {
            return isOgcCapabilitiesRequest();
        } else {
            return hideResource(layer.getResource());
        }
    }

    @Override
    public boolean hideResource(ResourceInfo resource) {
        if (!resource.isAdvertised()) {
            return isOgcCapabilitiesRequest();
        } else {
            return false;
        }
    }

    boolean isOgcCapabilitiesRequest() {
        Request request = Dispatcher.REQUEST.get();
        return request != null && "GetCapabilities".equalsIgnoreCase(request.getRequest());
    }

}
