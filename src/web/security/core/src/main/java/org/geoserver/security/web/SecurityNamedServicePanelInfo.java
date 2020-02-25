/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import org.geoserver.platform.ExtensionPriority;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.ComponentInfo;

/**
 * Extension point for configuration panels for named security service classes.
 *
 * @author Justin Deoliveira, OpenGeo
 * @param <C> The configuration class.
 * @param <T> The configuration panel class.
 */
public class SecurityNamedServicePanelInfo<
                C extends SecurityNamedServiceConfig, T extends SecurityNamedServicePanel<C>>
        extends ComponentInfo<T> implements ExtensionPriority {

    String shortTitleKey;
    Class serviceClass;
    Class<C> serviceConfigClass;
    int priority = 10;

    public String getShortTitleKey() {
        return shortTitleKey;
    }

    public void setShortTitleKey(String shortTitleKey) {
        this.shortTitleKey = shortTitleKey;
    }

    public Class getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(Class serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Class<C> getServiceConfigClass() {
        return serviceConfigClass;
    }

    public void setServiceConfigClass(Class<C> serviceConfigClass) {
        this.serviceConfigClass = serviceConfigClass;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
