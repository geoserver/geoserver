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
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.SuppressFBWarnings;

/**
 * Disables defined services on layer by configuration.
 *
 * <p>Default disabled list managed by {@code org.geoserver.service.disabled} system property.
 *
 * @author Fernando Mino, Geosolutions
 */
public class DisabledServiceResourceFilter extends AbstractCatalogFilter implements GeoServerLifecycleHandler {

    /** Property set in context/environment/system for default disabled services. */
    public static String PROPERTY = "org.geoserver.service.disabled";

    protected static List<String> DEFAULT_SERVICE_TYPES;

    private boolean isFilterSubject() {
        return request() != null && request().getService() != null && request().getRequest() != null;
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
        if (DEFAULT_SERVICE_TYPES == null) {
            String globalEnv = GeoServerExtensions.getProperty(PROPERTY);
            if (isNotBlank(globalEnv)) {
                DEFAULT_SERVICE_TYPES = Arrays.asList(globalEnv.split(","));
            } else {
                DEFAULT_SERVICE_TYPES = Collections.emptyList();
            }
        }

        return DEFAULT_SERVICE_TYPES;
    }

    /**
     * Returns a list of disabled service types for the given resource.
     *
     * <p>If {@link ResourceInfo#isServiceConfiguration()} is {@code true} the resource is responsible for providing the
     * list of disabled service types.
     *
     * <p>If {@link ResourceInfo#isServiceConfiguration()} is {@code false} a default list of disabled service types is
     * provided.
     *
     * @return list of disabled service types
     */
    public static List<String> disabledServices(ResourceInfo resource) {
        List<String> disabledServices;
        // if service configuration is enabled get layer's disable services list
        if (resource.isServiceConfiguration()) {
            disabledServices = CollectionUtils.isEmpty(resource.getDisabledServices())
                    ? Collections.emptyList()
                    : resource.getDisabledServices();
        } else {
            // service configuration disabled, get global env default disabled services list
            disabledServices = defaultDisabledServiceTypes();
        }
        return disabledServices;
    }

    @Override
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD") // Spring singleton anyways
    public void onReset() {
        DEFAULT_SERVICE_TYPES = null;
    }

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {}

    @Override
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD") // Spring singleton anyways
    public void onReload() {
        DEFAULT_SERVICE_TYPES = null;
    }
}
