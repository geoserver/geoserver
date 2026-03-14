/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.services;

import org.geoserver.config.ServiceInfo;
import org.geoserver.web.ComponentInfo;

/**
 * Extension point for plugins to contribute additional panels to the various service admin pages.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AdminPagePanelInfo extends ComponentInfo<AdminPagePanel> {

    protected Class<? extends ServiceInfo> serviceClass;

    protected String specificServiceType;


    /**
     * ServiceInfo, example {@code WFSInfo}, used for configuration.
     * @return ServiceInfo panel uses for configuration
     */
    public Class<? extends ServiceInfo> getServiceClass() {
        return serviceClass;
    }

    /**
     * ServiceInfo, example {@code WFSInfo}, panel is designed to configure.
     * @param serviceClass ServiceInfo panel uses for configuration
     */
    public void setServiceClass(Class<? extends ServiceInfo> serviceClass) {
        this.serviceClass = serviceClass;
    }

    /**
     * The specific service type, example {@code WFS} or {@code Features}, panel configures.
     * @return The specific service type
     */
    public String getSpecificServiceType() {
        return specificServiceType;
    }

    /**
     * The specific service type, example {@code WFS} or {@code Features}, panel configures.
     * @param specificServiceType The specific service type
     */
    public void setSpecificServiceType(String specificServiceType) {
        this.specificServiceType = specificServiceType;
    }
}
