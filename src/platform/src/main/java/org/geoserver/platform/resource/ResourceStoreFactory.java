/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ResourceStoreFactory implements FactoryBean<ResourceStore>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override public ResourceStore getObject() throws Exception {

        ResourceStore resourceStore = (ResourceStore) GeoServerExtensions.bean(
                "resourceStoreImpl", applicationContext);
        if (resourceStore == null) {
            resourceStore = GeoServerExtensions.bean(
                    DataDirectoryResourceStore.class, applicationContext);
        }

        return resourceStore;
    }

    @Override public Class<?> getObjectType() {
        return null;
    }

    @Override public boolean isSingleton() {
        return true;
    }

    @Override public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }
}
