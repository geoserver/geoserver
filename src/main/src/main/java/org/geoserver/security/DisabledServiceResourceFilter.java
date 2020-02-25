/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;

/**
 * Disables defined services on layer by configuration
 *
 * @author Fernando Mino, Geosolutions
 */
public class DisabledServiceResourceFilter extends AbstractCatalogFilter {

    private boolean isFilterSubject() {
        return request() != null
                && request().getService() != null
                && request().getRequest() != null;
    }

    private Request request() {
        return Dispatcher.REQUEST.get();
    }

    @Override
    public boolean hideLayer(LayerInfo layer) {
        return hideResource(layer.getResource());
    }

    @Override
    public boolean hideResource(ResourceInfo resource) {
        if (isFilterSubject()) {
            List<String> disabledServices = disabledServices(resource);
            // if any disabled service match with current service -> hide resource
            String service = request().getService();
            return disabledServices
                    .stream()
                    .anyMatch(s -> service.toLowerCase().equals(s.trim().toLowerCase()));
        }
        return false;
    }

    private static List<String> defaultDisabledServices() {
        List<String> list = null;
        String globalEnv = GeoServerExtensions.getProperty("org.geoserver.service.disabled");
        if (isNotBlank(globalEnv)) {
            list = Arrays.asList(globalEnv.split(","));
        }
        return list == null ? Collections.emptyList() : list;
    }

    /** Returns a list of disabled Services for the given resource */
    public static List<String> disabledServices(ResourceInfo resource) {
        List<String> disabledServices;
        // if service configuration is enabled get layer's disable services list
        if (resource.isServiceConfiguration()) {
            disabledServices =
                    CollectionUtils.isEmpty(resource.getDisabledServices())
                            ? Collections.emptyList()
                            : resource.getDisabledServices();
        } else {
            // service configuration disabled, get global env default disabled services list
            disabledServices = defaultDisabledServices();
        }
        return disabledServices;
    }
}
