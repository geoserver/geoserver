/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Factory for ResourceStore creation. Looks for a resourceStoreImpl bean before falling back to the
 * dataDirectoryResourceStore bean. Used to override ResourceStore implementation if desired.
 */
public class ResourceStoreFactory implements FactoryBean<ResourceStore>, ApplicationContextAware {

    final Logger LOGGER = Logging.getLogger("org.geoserver.platform");

    private ApplicationContext applicationContext;

    @Override
    public ResourceStore getObject() throws Exception {

        ResourceStore resourceStore = null;
        try {
            resourceStore =
                    (ResourceStore)
                            GeoServerExtensions.bean("resourceStoreImpl", applicationContext);
        } catch (NoSuchBeanDefinitionException e) {
            LOGGER.log(
                    Level.FINER,
                    "No resourceStoreImpl beans found, "
                            + "falling back to DataDirectoryResourceStore");
        }
        if (resourceStore == null) {
            resourceStore =
                    (ResourceStore)
                            GeoServerExtensions.bean(
                                    "dataDirectoryResourceStore", applicationContext);
        }

        return resourceStore;
    }

    @Override
    public Class<?> getObjectType() {
        return null;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
