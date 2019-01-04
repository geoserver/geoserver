/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/** Configure the geogig content negotiation strategy. */
@Configuration
public class GeogigSpringConfig {

    @Bean
    GeogigContentNegotiationStrategy geogigContentNegotiationStrategy() {
        return new GeogigContentNegotiationStrategy();
    }

    static final String FILTER_MULTIPART_RESOLVER_NAME = "geogigFilterMultipartResolver";

    @Bean(name = FILTER_MULTIPART_RESOLVER_NAME)
    MultipartResolver geogigFilterMultipartResolver() {
        return new CommonsMultipartResolver();
    }

    @Bean
    GeogigMultipartFilter geogigMultipartFilter() {
        return new GeogigMultipartFilter();
    }
}
