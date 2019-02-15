/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.IOException;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.template.DirectTemplateFeatureCollectionFactory;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;

public class FreemarkerTemplateSupport {

    private static Configuration templateConfig = TemplateUtils.getSafeConfiguration();

    private final GeoServerResourceLoader resoureLoader;

    static DirectTemplateFeatureCollectionFactory FC_FACTORY =
            new DirectTemplateFeatureCollectionFactory();

    static {
        // initialize the template engine, this is static to maintain a cache of templates being
        // loaded
        templateConfig = TemplateUtils.getSafeConfiguration();
        templateConfig.setObjectWrapper(new FeatureWrapper(FC_FACTORY));
    }

    public FreemarkerTemplateSupport(GeoServerResourceLoader loader) {
        this.resoureLoader = loader;
    }

    /**
     * Returns the template for the specified feature type. Looking up templates is pretty
     * expensive, so we cache templates by feture type and template.
     */
    Template getTemplate(ResourceInfo resource, String templateName) throws IOException {
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
