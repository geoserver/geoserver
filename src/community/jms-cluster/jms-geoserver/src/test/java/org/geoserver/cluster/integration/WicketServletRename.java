/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.integration;

import java.util.UUID;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Allows us to instantiate several GeoServer instances by attributing a different name to every
 * wicket servlet.
 */
public class WicketServletRename implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        beanFactory
                .getBeanDefinition("wicket")
                .getPropertyValues()
                .getPropertyValue("servletName")
                .setConvertedValue(UUID.randomUUID().toString());
    }
}
