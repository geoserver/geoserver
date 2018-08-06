/*
 *  (c) 2018 Open Source Geospatial Foundation - all rights reserved
 *  * This code is licensed under the GPL 2.0 license, available at the root
 *  * application directory.
 *
 */
package org.geoserver.wfs3.response;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Response;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs3.BaseRequest;
import org.geoserver.wfs3.GetFeatureType;

public abstract class AbstractHTMLResponse extends Response {

    private static Configuration templateConfig = TemplateUtils.getSafeConfiguration();

    protected final GeoServer geoServer;
    private FreemarkerTemplateSupport templateSupport;

    public AbstractHTMLResponse(
            Class<?> binding, GeoServerResourceLoader loader, GeoServer geoServer) {
        super(binding, BaseRequest.HTML_MIME);
        this.geoServer = geoServer;
        this.templateSupport = new FreemarkerTemplateSupport(loader, geoServer);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return BaseRequest.HTML_MIME;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        final ResourceInfo ri = getResource(value);
        final String templateName = getTemplateName(value);
        Template template = templateSupport.getTemplate(ri, templateName);

        try {
            HashMap<String, Object> model = setupModel(value, operation);
            template.process(model, new OutputStreamWriter(output));
        } catch (TemplateException e) {
            throw new IOException("Error occured processing HTML template " + templateName, e);
        }
    }

    /**
     * Prepares the model with the usual suspects:
     *
     * <ul>
     *   <li>model: the model object
     *   <li>service: the WFSInfo object
     *   <li>contact: the contact information
     *   <li>baseURL: the GeoServer baseURL for link construction
     * </ul>
     *
     * @param value
     * @param operation
     * @return
     */
    protected HashMap<String, Object> setupModel(Object value, Operation operation) {
        HashMap<String, Object> model = new HashMap<>();
        model.put("model", value);
        model.put("service", geoServer.getService(WFSInfo.class));
        model.put("contact", geoServer.getGlobal().getSettings().getContact());
        model.put("baseURL", getBaseURL(operation));
        addServiceLinkFunctions(value, operation, model);
        return model;
    }

    static void addServiceLinkFunctions(
            Object value, Operation operation, Map<String, Object> model) {
        model.put(
                "serviceLink",
                (TemplateMethodModel)
                        arguments ->
                                ResponseUtils.buildURL(
                                        getBaseURL(operation),
                                        (String) arguments.get(0),
                                        null,
                                        URLMangler.URLType.SERVICE));
        model.put(
                "resourceLink",
                (TemplateMethodModel)
                        arguments ->
                                ResponseUtils.buildURL(
                                        getBaseURL(operation),
                                        (String) arguments.get(0),
                                        null,
                                        URLMangler.URLType.RESOURCE));
        model.put(
                "externalLink",
                (TemplateMethodModel)
                        arguments ->
                                ResponseUtils.buildURL(
                                        getBaseURL(operation),
                                        (String) arguments.get(0),
                                        null,
                                        URLMangler.URLType.EXTERNAL));
    }

    static String getBaseURL(Operation operation) {
        Object firstParam = operation.getParameters()[0];
        if (firstParam instanceof BaseRequest) {
            BaseRequest request = (BaseRequest) firstParam;
            return request.getBaseUrl();
        } else if (firstParam instanceof GetFeatureType) {
            return ((GetFeatureType) firstParam).getBaseUrl();
        }
        throw new IllegalArgumentException("Cannot extract base URL from " + firstParam);
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
