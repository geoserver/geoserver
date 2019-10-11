/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.catalog.*;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.opengis.filter.Filter;

/**
 * Makes sure disabled layers/resources cannot be accessed from outside regardless of the service
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DisabledResourceFilter extends AbstractCatalogFilter {

    private boolean shouldApplyFilter() {
        Request request = Dispatcher.REQUEST.get();
        // for the moment, match any recognized OGC request
        return request != null && request.getService() != null && request.getRequest() != null;
    }

    @Override
    public Filter getSecurityFilter(Class<? extends CatalogInfo> clazz) {
        if (shouldApplyFilter()) {
            if (LayerInfo.class.isAssignableFrom(clazz)) {
                return Predicates.and(
                        Predicates.equal("enabled", true),
                        Predicates.equal("resource.enabled", true),
                        Predicates.equal("resource.store.enabled", true));
            } else if (ResourceInfo.class.isAssignableFrom(clazz)) {
                return Predicates.and(
                        Predicates.equal("enabled", true), Predicates.equal("store.enabled", true));
            }
        }
        return Filter.INCLUDE;
    }

    @Override
    public boolean hideLayer(LayerInfo layer) {
        if (shouldApplyFilter()) {
            return !layer.enabled();
        }
        return false;
    }

    @Override
    public boolean hideLayerGroup(LayerGroupInfo layerGroup) {
        if (shouldApplyFilter()) {
            return !layerGroup.isEnabled();
        }
        return false;
    }

    @Override
    public boolean hideResource(ResourceInfo resource) {
        if (shouldApplyFilter()) {
            return !resource.enabled();
        }
        return false;
    }
}
