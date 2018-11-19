/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ServiceResourceUtil implements ApplicationContextAware {

    public static final String BEAN_ID = "serviceResourceUtil";

    protected ApplicationContext context;

    public ServiceResourceUtil() {}

    public List<String> getLayerVotedServices(ResourceInfo resource) {
        List<ServiceResourceVoter> voters =
                GeoServerExtensions.extensions(ServiceResourceVoter.class, context);
        return voters.stream()
                .filter(v -> !v.hideService(resource))
                .map(v -> v.serviceName())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}
