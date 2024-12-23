/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geotools.util.logging.Logging;

/**
 * Base class for {@link org.springframework.http.converter.HttpMessageConverter} that encode a HTML document based on a
 * Freemarker template
 *
 * @param <T>
 */
public abstract class AbstractServiceHTMLMessageConverter<T> extends AbstractHTMLMessageConverter<T> {
    static final Logger LOGGER = Logging.getLogger(AbstractServiceHTMLMessageConverter.class);
    protected final Class<?> binding;
    protected final Class<? extends ServiceInfo> serviceConfigurationClass;

    /**
     * Builds a message converter
     *
     * @param binding The bean meant to act as the model for the template
     * @param serviceConfigurationClass The class holding the configuration for the service
     * @param templateSupport A loader used to locate templates
     * @param geoServer The
     */
    public AbstractServiceHTMLMessageConverter(
            Class<?> binding,
            Class<? extends ServiceInfo> serviceConfigurationClass,
            FreemarkerTemplateSupport templateSupport,
            GeoServer geoServer) {
        super(templateSupport, geoServer);
        this.binding = binding;
        this.serviceConfigurationClass = serviceConfigurationClass;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return binding.isAssignableFrom(clazz);
    }

    @Override
    public Class<? extends ServiceInfo> getServiceConfigurationClass() {
        return serviceConfigurationClass;
    }
}
