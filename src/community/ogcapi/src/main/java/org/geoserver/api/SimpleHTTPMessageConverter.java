/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
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
            GeoServerResourceLoader loader,
            GeoServer geoServer,
            String templateName) {
        super(binding, serviceConfigurationClass, loader, geoServer);
        this.templateName = templateName;
        this.serviceClass = serviceClass;
    }

    @Override
    protected void writeInternal(T value, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Template template = templateSupport.getTemplate(null, templateName, serviceClass);

        try {
            HashMap<String, Object> model = setupModel(value);
            template.process(model, new OutputStreamWriter(outputMessage.getBody()));
        } catch (TemplateException e) {
            throw new IOException("Error occured processing HTML template " + templateName, e);
        }
    }
}
