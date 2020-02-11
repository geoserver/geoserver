/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import org.geoserver.config.GeoServer;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * A converter used when the {@link HTMLResponseBody} is found, to simply apply a template to the
 * object returned by the controller method
 *
 * @param <T>
 */
public class SimpleHTTPMessageConverter<T> extends AbstractHTMLMessageConverter<T> {

    private final String templateName;
    private final Class serviceClass;

    /**
     * Builds a message converter
     *
     * @param binding The bean meant to act as the model for the template
     * @param serviceConfigurationClass The class holding the configuration for the service
     * @param serviceClass The controller class, used to lookup the default templates
     * @param loader A loader used to locate templates in the GeoServer data directory
     * @param geoServer The
     */
    public SimpleHTTPMessageConverter(
            Class binding,
            Class serviceConfigurationClass,
            Class serviceClass,
            FreemarkerTemplateSupport support,
            GeoServer geoServer,
            String templateName) {
        super(binding, serviceConfigurationClass, support, geoServer);
        this.templateName = templateName;
        this.serviceClass = serviceClass;
    }

    @Override
    protected void writeInternal(T value, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        HashMap<String, Object> model = setupModel(value);
        templateSupport.processTemplate(
                null,
                templateName,
                serviceClass,
                model,
                new OutputStreamWriter(outputMessage.getBody()));
    }
}
