/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import java.io.IOException;
import java.io.InputStream;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.InputStreamResource;

/**
 * *
 *
 * <p>Loads additional beans from xml file in resource store.
 *
 * @author Niels Charlier
 */
public class ConfigurationBeansLoader implements BeanDefinitionRegistryPostProcessor {

    private ResourceStore resourceStore;

    public ConfigurationBeansLoader(ResourceStore resourceStore) {
        this.resourceStore = resourceStore;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {

        Resource res = resourceStore.get("/taskmanager/taskManager-applicationContext.xml");

        try {
            if (!Resources.exists(res)) {
                IOUtils.copy(
                        getClass().getResourceAsStream("/taskManager-applicationContext.xml"),
                        res.out());
            }

            try (InputStream in = res.in()) {
                XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
                reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
                reader.loadBeanDefinitions(new InputStreamResource(in));
            }
        } catch (IOException e) {
            throw new ApplicationContextException(
                    "Failed to load taskmanager's application context: ", e);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {}
}
