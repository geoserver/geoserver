/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

/**
 * Bean name generator that prefixes geogig in front of the default bean name to avoid naming
 * conflicts between geogig and geoserver beans.
 */
public class GeogigBeanNameGenerator extends AnnotationBeanNameGenerator {

    public GeogigBeanNameGenerator() {
        super();
    }

    @Override
    public String generateBeanName(BeanDefinition arg0, BeanDefinitionRegistry arg1) {
        return "geogig" + super.generateBeanName(arg0, arg1);
    }
}
