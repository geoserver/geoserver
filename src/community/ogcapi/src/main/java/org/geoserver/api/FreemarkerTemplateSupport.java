/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import freemarker.cache.ClassTemplateLoader;
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

/**
 * Support class that locates the templates based on the current response and eventual {@link
 * LocalWorkspace}
 */
public class FreemarkerTemplateSupport {

    private static Configuration templateConfig = TemplateUtils.getSafeConfiguration();

    private final GeoServerResourceLoader resoureLoader;

    ClassTemplateLoader rootLoader = new ClassTemplateLoader(FreemarkerTemplateSupport.class, "");

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
    public Template getTemplate(ResourceInfo resource, String templateName, Class<?> clazz)
            throws IOException {
        GeoServerTemplateLoader templateLoader =
                new GeoServerTemplateLoader(clazz, resoureLoader) {
                    @Override
                    public Object findTemplateSource(String path) throws IOException {
                        Object source = super.findTemplateSource(path);
                        if (source == null) {
                            source = rootLoader.findTemplateSource(path);

                            // wrap the source in a source that maintains the orignial path
                            if (source != null) {
                                return new ClassTemplateSource(path, source);
                            }
                        }

                        return source;
                    }
                };

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
