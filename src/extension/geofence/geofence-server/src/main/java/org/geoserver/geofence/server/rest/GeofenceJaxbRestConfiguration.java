/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.rest;

import java.util.List;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlRootElement;
import org.geotools.util.logging.Logging;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link WebMvcConfigurer} that makes sure {@link Jaxb2RootElementHttpMessageConverter} is present
 * in order to handle JAXB annotated classes.
 *
 * <p>{@link WebMvcConfigurationSupport#addDefaultHttpMessageConverters()} will not add a {@link
 * Jaxb2RootElementHttpMessageConverter} if Jackson2 is in the classpath, and will add a {@code
 * MappingJackson2XmlHttpMessageConverter} instead, rendering the JAXB2 annotations in Geofence's
 * REST object model useless. On the other hand, {@link Jaxb2RootElementHttpMessageConverter} will
 * only engage for {@link XmlRootElement @XmlRootElement} annotated message payloads.
 *
 * @since 2.22.1
 */
@Component
public class GeofenceJaxbRestConfiguration implements WebMvcConfigurer {

    private static final Logger LOGGER =
            Logging.getLogger(Jaxb2RootElementHttpMessageConverter.class);

    /**
     * Makes sure {@link Jaxb2RootElementHttpMessageConverter} is available and has higher priority
     * than {@code MappingJackson2XmlHttpMessageConverter}, otherwise it'll be picked up and the
     * JAXB annotations won't be respected.
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(Jaxb2RootElementHttpMessageConverter.class::isInstance);
        converters.add(0, new Jaxb2RootElementHttpMessageConverter());
        LOGGER.info("JAXB message converter added to handle Geofence message payloads");
    }
}
