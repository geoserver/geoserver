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

import freemarker.template.TemplateMethodModel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wfs.WFSInfo;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

public abstract class AbstractHTMLMessageConverter<T> extends AbstractHttpMessageConverter<T> {

    protected final Class binding;
    protected final GeoServer geoServer;
    protected final FreemarkerTemplateSupport templateSupport;

    public AbstractHTMLMessageConverter(
            Class binding, GeoServerResourceLoader loader, GeoServer geoServer) {
        super(MediaType.TEXT_HTML);
        this.binding = binding;
        this.geoServer = geoServer;
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
        addLinkFunctions(baseURL, model);
        return model;
    }

    /**
     * Adds the <code>serviceLink</code>, <code>serviceLink</code> and <code>externalLinks</code>
     * functions to the model, for usage in the tempalte
     *
     * @param baseURL
     * @param model
     */
    public static void addLinkFunctions(String baseURL, Map<String, Object> model) {
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

    protected String getBaseURL() {
        RequestInfo requestInfo = RequestInfo.get();
        if (requestInfo == null) {
            throw new IllegalArgumentException("Cannot extract base URL, RequestInfo is not set");
        }
        return requestInfo.getBaseURL();
    }

    /** Purges iterators that might have been used when walking over GeoTools features */
    protected void purgeIterators() {
        FreemarkerTemplateSupport.FC_FACTORY.purge();
    }
}
