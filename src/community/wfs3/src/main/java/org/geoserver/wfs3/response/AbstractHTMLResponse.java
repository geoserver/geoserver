/*
 *  (c) 2018 Open Source Geospatial Foundation - all rights reserved
 *  * This code is licensed under the GPL 2.0 license, available at the root
 *  * application directory.
 *
 */
package org.geoserver.wfs3.response;

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
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs3.BaseRequest;
import org.geotools.util.Converters;

public abstract class AbstractHTMLResponse extends Response {

    protected final GeoServer geoServer;
    private FreemarkerTemplateSupport templateSupport;

    public AbstractHTMLResponse(
            Class<?> binding, GeoServerResourceLoader loader, GeoServer geoServer) {
        super(binding, BaseRequest.HTML_MIME);
        this.geoServer = geoServer;
        this.templateSupport = new FreemarkerTemplateSupport(loader);
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
        String baseURL = Converters.convert(OwsUtils.get(firstParam, "baseUrl"), String.class);
        if (baseURL == null) {
            throw new IllegalArgumentException("Cannot extract base URL from " + firstParam);
        }
        return baseURL;
    }

    /** Returns the template name to be used for the object to be encoded */
    protected abstract String getTemplateName(Object value);

    /** Returns the eventual ResourceInfo associated with the */
    protected abstract ResourceInfo getResource(Object value);

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return getFileName(value, operation) + ".html";
    }

    /** The name of the file for the response */
    protected abstract String getFileName(Object value, Operation operation);
}
