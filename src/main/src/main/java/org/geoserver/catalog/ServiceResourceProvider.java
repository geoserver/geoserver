/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Singleton Bean for to provide filtered services by resource type
 *
 * @author Fernando Miño - Geosolutions
 */
public class ServiceResourceProvider implements ApplicationContextAware {

    private ApplicationContext context;
    private GeoServer geoServer;

    public ServiceResourceProvider(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    /** Returns a list of available services for a resource */
    public List<String> getServicesForResource(ResourceInfo resource) {
        List<String> services = servicesList();
        List<ServiceResourceVoter> voters =
                GeoServerExtensions.extensions(ServiceResourceVoter.class, context);
        return services.stream()
                .filter(s -> !isServiceHidden(resource, s, voters))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Returns a list of available services for a layer name */
    public List<String> getServicesForLayerName(String layerName) {
        ResourceInfo resource = geoServer.getCatalog().getLayerByName(layerName).getResource();
        return getServicesForResource(resource);
    }

    private boolean isServiceHidden(
            ResourceInfo resource, String serviceName, List<ServiceResourceVoter> voters) {
        return voters.stream().anyMatch(v -> v.hideService(serviceName, resource));
    }

    private List<String> servicesList() {
        return geoServer
                .getServices()
                .stream()
                .map(si -> si.getName())
                .collect(Collectors.toList());
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}
