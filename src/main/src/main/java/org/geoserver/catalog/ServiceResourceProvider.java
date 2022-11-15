/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Singleton Bean for to provide filtered service type by resource.
 *
 * <p>See {@link ServiceInfo#getType()}.
 *
 * @author Fernando Miño - Geosolutions
 */
public class ServiceResourceProvider implements ApplicationContextAware {

    private ApplicationContext context;
    private GeoServer geoServer;

    public ServiceResourceProvider(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    /**
     * List of available service types for a resource.
     *
     * <p>This list checks resource compatibility, using {@link ServiceResourceVoter}. The list
     * should be checked against {@link ResourceInfo#getDisabledServices()} if the user has disabled
     * any specific service types as provided by {@link
     * org.geoserver.security.DisabledServiceResourceFilter#disabledServices(ResourceInfo)}.
     *
     * @return list of service types for a resource
     */
    public List<String> getServicesForResource(ResourceInfo resource) {
        List<String> services = servicesList();
        List<ServiceResourceVoter> voters =
                GeoServerExtensions.extensions(ServiceResourceVoter.class, context);
        return services.stream()
                .filter(s -> !isServiceHidden(resource, s, voters))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * List of available service types for a layer name.
     *
     * @return list of service types for a layer name.
     */
    public List<String> getServicesForLayerName(String layerName) {
        ResourceInfo resource = geoServer.getCatalog().getLayerByName(layerName).getResource();
        return getServicesForResource(resource);
    }

    /**
     * Check if service type should be hidden for the provided resource
     *
     * @param resource Resource
     * @param serviceType Type to check with voters
     * @param voters Voters available to check resource compatibility with service
     * @return True if any voter declares resource incompatible with the service type
     */
    private boolean isServiceHidden(
            ResourceInfo resource, String serviceType, List<ServiceResourceVoter> voters) {
        return voters.stream().anyMatch(v -> v.hideService(serviceType, resource));
    }

    /**
     * List of service types registered across the application.
     *
     * @return list of all service types
     */
    private List<String> servicesList() {
        return geoServer.getServices().stream()
                .map(si -> si.getType())
                .collect(Collectors.toList());
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}
