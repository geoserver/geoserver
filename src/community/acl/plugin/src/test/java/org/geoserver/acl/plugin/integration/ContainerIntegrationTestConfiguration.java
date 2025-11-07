/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.integration;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.GeoServerTestApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

/**
 * Loaded from {@literal applicationContext-test.xml}, contributes {@link Environment} and {@link PropertyResolver}, for
 * some reason {@link GeoServerTestApplicationContext} from {@link GeoServerSystemTestSupport} does not.
 */
@Configuration
public class ContainerIntegrationTestConfiguration {

    @Bean
    public Environment springApplicationContextEnvironment(ApplicationContext c) {
        return c.getEnvironment();
    }

    @Bean
    public PropertyResolver springApplicationContextPropertyResolver(ApplicationContext c) {
        return c.getEnvironment();
    }
}
