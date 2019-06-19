/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.api;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wfs.WFSInfo;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public abstract class AbstractHTMLMessageConverter<T> extends AbstractHttpMessageConverter<T> {

    protected final Class binding;
    protected final GeoServer geoServer;
    protected final Class<? extends ServiceInfo> serviceClass;
    protected final FreemarkerTemplateSupport templateSupport;

    public AbstractHTMLMessageConverter(
            Class binding,
            Class<? extends ServiceInfo> serviceClass,
            GeoServerResourceLoader loader,
            GeoServer geoServer) {
        super(MediaType.TEXT_HTML);
        this.binding = binding;
        this.geoServer = geoServer;
        this.serviceClass = serviceClass;
        this.templateSupport = new FreemarkerTemplateSupport(loader);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return binding.isAssignableFrom(clazz);
    }

    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("HTML message conveters can only write HTML");
    }

    @Override
    protected void writeInternal(T value, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        final ResourceInfo ri = getResource(value);
        final String templateName = getTemplateName(value);
        Template template = templateSupport.getTemplate(ri, templateName, serviceClass);

        try {
            HashMap<String, Object> model = setupModel(value);
            template.process(model, new OutputStreamWriter(outputMessage.getBody()));
        } catch (TemplateException e) {
            throw new IOException("Error occured processing HTML template " + templateName, e);
        }
    }

    /**
     * Prepares the model with the usual suspects:
     *
     * <ul>
     *   <li>model: the model object
     *   <li>service: the ServiceInfo object
     *   <li>contact: the contact information
     *   <li>baseURL: the GeoServer baseURL for link construction
     * </ul>
     *
     * @param value
     * @param operation
     * @return
     */
    protected HashMap<String, Object> setupModel(Object value) {
        HashMap<String, Object> model = new HashMap<>();
        model.put("model", value);
        model.put("service", geoServer.getService(WFSInfo.class));
        model.put("contact", geoServer.getGlobal().getSettings().getContact());
        final String baseURL = getBaseURL();
        model.put("baseURL", baseURL);
        addServiceLinkFunctions(value, baseURL, model);
        return model;
    }

    static void addServiceLinkFunctions(Object value, String baseURL, Map<String, Object> model) {
        model.put(
                "serviceLink",
                (TemplateMethodModel)
                        arguments ->
                                ResponseUtils.buildURL(
                                        baseURL,
                                        (String) arguments.get(0),
                                        null,
                                        URLMangler.URLType.SERVICE));
        model.put(
                "resourceLink",
                (TemplateMethodModel)
                        arguments ->
                                ResponseUtils.buildURL(
                                        baseURL,
                                        (String) arguments.get(0),
                                        null,
                                        URLMangler.URLType.RESOURCE));
        model.put(
                "externalLink",
                (TemplateMethodModel)
                        arguments ->
                                ResponseUtils.buildURL(
                                        baseURL,
                                        (String) arguments.get(0),
                                        null,
                                        URLMangler.URLType.EXTERNAL));
    }

    static String getBaseURL() {
        RequestInfo requestInfo = RequestInfo.get();
        if (requestInfo == null) {
            throw new IllegalArgumentException("Cannot extract base URL, RequestInfo is not set");
        }
        return requestInfo.getBaseURL();
    }

    /**
     * Returns the template name to be used for the object to be encoded
     *
     * @param value
     * @return
     */
    protected abstract String getTemplateName(Object value);

    /**
     * Returns the eventual ResourceInfo associated with the
     *
     * @param value
     * @return
     */
    protected abstract ResourceInfo getResource(Object value);
}
