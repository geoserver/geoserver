/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;

/**
 * Disables defined services on layer by configuration.
 *
 * <p>Default disabled list managed by {@code org.geoserver.service.disabled} system property.
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
            return disabledServices.stream()
                    .anyMatch(serviceType -> StringUtils.equalsIgnoreCase(service, serviceType));
        }
        return false;
    }

    private static List<String> defaultDisabledServiceTypes() {
        List<String> list = null;
        String globalEnv = GeoServerExtensions.getProperty("org.geoserver.service.disabled");
        if (isNotBlank(globalEnv)) {
            list = Arrays.asList(globalEnv.split(","));
        }
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * Returns a list of disabled service types for the given resource.
     *
     * <p>If {@link ResourceInfo#isServiceConfiguration()} is {@code true} the resource is
     * responsible for providing the list of disabled service types.
     *
     * <p>If {@link ResourceInfo#isServiceConfiguration()} is {@code false} a default list of
     * disabled service types is provided.
     *
     * @return list of disabled service types
     */
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
            disabledServices = defaultDisabledServiceTypes();
        }
        return disabledServices;
    }
}
