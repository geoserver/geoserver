/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.SecureCatalogImpl;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class ApplicationListenerImpl implements ApplicationListener {

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextRefreshedEvent) {
            SecureCatalogImpl secureCatalog = GeoServerExtensions.bean(SecureCatalogImpl.class);
            secureCatalog.getResourceAccessManager().buildLayerGroupCache();
        }
    }
}
