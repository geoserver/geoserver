/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.template.DirectTemplateFeatureCollectionFactory;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;
import org.geotools.util.SoftValueHashMap;
import org.springframework.stereotype.Component;

/**
 * Support class that locates the templates based on the current response and eventual {@link
 * LocalWorkspace}
 */
@Component
public class FreemarkerTemplateSupport {

    private static Map<Class, Configuration> configurationCache =
            new SoftValueHashMap<Class, Configuration>(10);

    private final GeoServerResourceLoader resoureLoader;

    ClassTemplateLoader rootLoader = new ClassTemplateLoader(FreemarkerTemplateSupport.class, "");

    static DirectTemplateFeatureCollectionFactory FC_FACTORY =
            new DirectTemplateFeatureCollectionFactory();

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

                            // wrap the source in a source that maintains the original path
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
        Configuration configuration = getTemplateConfiguration(clazz);
        synchronized (configuration) {
            configuration.setTemplateLoader(templateLoader);
            Template t = configuration.getTemplate(templateName);
            t.setEncoding("UTF-8");
            return t;
        }
    }

    Configuration getTemplateConfiguration(Class clazz) {
        return configurationCache.computeIfAbsent(
                clazz,
                k -> {
                    Configuration cfg = TemplateUtils.getSafeConfiguration();
                    cfg.setObjectWrapper(new FeatureWrapper(FC_FACTORY));
                    return cfg;
                });
    }

    /**
     * Processes a template and returns the result as a string
     *
     * @param resource The resource reference used to lookup templates in the data dir
     * @param templateName The template name
     * @param referenceClass The reference class for classpath template loading
     * @param model The model to be applied
     * @param write The writer receiving the template output
     */
    public void processTemplate(
            ResourceInfo resource,
            String templateName,
            Class referenceClass,
            Map<String, Object> model,
            Writer writer)
            throws IOException {
        Template template = getTemplate(resource, templateName, referenceClass);

        try {
            template.process(model, writer);
        } catch (TemplateException e) {
            throw new IOException("Error occured processing template " + templateName, e);
        }
    }

    /**
     * Processes a template and returns the result as a string
     *
     * @param resource The resource reference used to lookup templates in the data dir
     * @param templateName The template name
     * @param referenceClass The reference class for classpath template loading
     * @param model The model to be applied
     */
    public String processTemplate(
            ResourceInfo resource,
            String templateName,
            Class referenceClass,
            Map<String, Object> model)
            throws IOException {
        StringWriter sw = new StringWriter();
        processTemplate(resource, templateName, referenceClass, model, sw);
        return sw.toString();
    }
}
