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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs3.BaseRequest;

public abstract class AbstractHTMLResponse extends Response {

    private static Configuration templateConfig = new Configuration();

    private final GeoServerResourceLoader resoureLoader;
    private final GeoServer geoServer;

    public AbstractHTMLResponse(
            Class<?> binding, GeoServerResourceLoader loader, GeoServer geoServer)
            throws IOException {
        super(binding, BaseRequest.HTML_MIME);
        this.resoureLoader = loader;
        this.geoServer = geoServer;
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
        Template template = getTemplate(ri, templateName);

        try {
            HashMap<String, Object> model = new HashMap<>();
            model.put("model", value);
            model.put("service", geoServer.getService(WFSInfo.class));
            model.put("contact", geoServer.getGlobal().getSettings().getContact());
            model.put("baseURL", getBaseURL(value, operation));
            template.process(model, new OutputStreamWriter(output));
        } catch (TemplateException e) {
            throw new IOException("Error occured processing HTML template " + templateName, e);
        }
    }

    protected String getBaseURL(Object value, Operation operation) {
        BaseRequest request = (BaseRequest) operation.getParameters()[0];
        return request.getBaseUrl();
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
    /**
     * Returns the template for the specified feature type. Looking up templates is pretty
     * expensive, so we cache templates by feture type and template.
     */
    protected Template getTemplate(ResourceInfo resource, String templateName) throws IOException {
        // otherwise, build a loader and do the lookup
        GeoServerTemplateLoader templateLoader =
                new GeoServerTemplateLoader(getClass(), resoureLoader);
        if (resource != null) {
            templateLoader.setResource(resource);
        } else {
            WorkspaceInfo ws = LocalWorkspace.get();
            if (ws != null) {
                templateLoader.setWorkspace(ws);
            }
        }

        // Configuration is not thread safe
        synchronized (templateConfig) {
            templateConfig.setTemplateLoader(templateLoader);
            Template t = templateConfig.getTemplate(templateName);
            t.setEncoding("UTF-8");
            return t;
        }
    }
}
