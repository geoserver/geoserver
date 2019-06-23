/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
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

public class SimpleHTTPMessageConverter<T> extends AbstractHTMLMessageConverter<T> {

    private final String templateName;
    private final Class serviceClass;

    public SimpleHTTPMessageConverter(
            Class binding,
            Class serviceClass,
            GeoServerResourceLoader loader,
            GeoServer geoServer,
            String templateName) {
        super(binding, loader, geoServer);
        this.serviceClass = serviceClass;
        this.templateName = templateName;
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
