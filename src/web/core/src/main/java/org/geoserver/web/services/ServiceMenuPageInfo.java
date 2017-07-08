/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.services;

import org.geoserver.config.ServiceInfo;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.MenuPageInfo;

/**
 * MenuPageInfo for OGC service configuration pages.
 *
 * @author David Winslow <dwinslow@opengeo.org>
 */
@SuppressWarnings("serial")
public class ServiceMenuPageInfo<T extends GeoServerBasePage> extends MenuPageInfo<T> {

    Class<? extends ServiceInfo> serviceClass;

    public Class<? extends ServiceInfo> getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(Class<? extends ServiceInfo> serviceClass) {
        this.serviceClass = serviceClass;
    }
}
