/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.opengis.filter.Filter;

/**
 * Makes sure disabled layers/resources cannot be accessed from outside regardless of the service
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class DisabledResourceFilter extends AbstractCatalogFilter {

    static final Filter ENABLED_FILTER = Predicates.equal("enabled", true);

    private boolean shouldApplyFilter() {
        Request request = Dispatcher.REQUEST.get();
        // for the moment, match any recognized OGC request
        return request != null && request.getService() != null && request.getRequest() != null;
    }

    @Override
    public Filter getSecurityFilter(Class<? extends CatalogInfo> clazz) {
        if (shouldApplyFilter() && LayerInfo.class.isAssignableFrom(clazz)
                || ResourceInfo.class.isAssignableFrom(clazz)) {
            return ENABLED_FILTER;
        } else {
            return Filter.INCLUDE;
        }
    }

    @Override
    public boolean hideLayer(LayerInfo layer) {
        if (shouldApplyFilter()) {
            return !layer.isEnabled();
        }
        return false;
    }

    @Override
    public boolean hideResource(ResourceInfo resource) {
        if (shouldApplyFilter()) {
            return !resource.isEnabled();
        }
        return false;
    }
}
