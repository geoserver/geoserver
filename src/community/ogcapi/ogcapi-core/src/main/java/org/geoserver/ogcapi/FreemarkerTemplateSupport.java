/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
 * LocalWorkspace}.
 *
 * <p>Located in workspace using service landingPage prefix, or obtained from jar:
 *
 * <ul>
 *   <li>ogc/features/landingPage.ftl
 * </ul>
 */
@Component
public class FreemarkerTemplateSupport {

    private static final Map<Class<?>, Configuration> configurationCache =
            new SoftValueHashMap<>(10);

    private final GeoServerResourceLoader resourceLoader;

    ClassTemplateLoader rootLoader = new ClassTemplateLoader(FreemarkerTemplateSupport.class, "");

    static DirectTemplateFeatureCollectionFactory FC_FACTORY =
            new DirectTemplateFeatureCollectionFactory();

    public FreemarkerTemplateSupport(GeoServerResourceLoader loader) {
        this.resourceLoader = loader;
    }

    /**
     * Returns the template for the specified feature type. Looking up templates is pretty
     * expensive, so we cache templates by feature type and template.
     */
    public Template getTemplate(ResourceInfo resource, String templateName, Class<?> clazz)
            throws IOException {
        GeoServerTemplateLoader templateLoader =
                new GeoServerTemplateLoader(clazz, resourceLoader) {
                    @Override
                    public Object findTemplateSource(String path) throws IOException {
                        Object source = null;

                        APIService service = clazz.getAnnotation(APIService.class);
                        if (service != null) {
                            source = super.findTemplateSource(service.landingPage() + "/" + path);
                        }

                        if (source == null) {
                            source = super.findTemplateSource(path);
                        }

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
            return t;
        }
    }

    Configuration getTemplateConfiguration(Class<?> clazz) {
        return configurationCache.computeIfAbsent(
                clazz,
                k -> {
                    Configuration cfg = TemplateUtils.getSafeConfiguration();
                    cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
                    cfg.setObjectWrapper(new FeatureWrapper(FC_FACTORY));
                    return cfg;
                });
    }

    /**
     * Processes a template and returns the result as a string
     *
     * @param template The template to process
     * @param model The model to be applied
     * @param writer The writer receiving the template output
     * @param charset The charset to use for the output
     */
    public void processTemplate(
            Template template, Map<String, Object> model, Writer writer, Charset charset)
            throws IOException {
        try {
            Environment env = template.createProcessingEnvironment(model, writer, null);
            env.setOutputEncoding(charset.name());
            env.process();
        } catch (TemplateException e) {
            throw new IOException("Error occured processing template " + template.getName(), e);
        }
    }

    /**
     * Processes a template and returns the result as a string
     *
     * @param resource The resource reference used to lookup templates in the data dir
     * @param templateName The template name
     * @param referenceClass The reference class for classpath template loading
     * @param model The model to be applied
     * @param writer The writer receiving the template output
     */
    public void processTemplate(
            ResourceInfo resource,
            String templateName,
            Class<?> referenceClass,
            Map<String, Object> model,
            Writer writer,
            Charset charset)
            throws IOException {
        Template template = getTemplate(resource, templateName, referenceClass);
        processTemplate(template, model, writer, charset);
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
            Class<?> referenceClass,
            Map<String, Object> model,
            Charset charset)
            throws IOException {
        StringWriter sw = new StringWriter();
        processTemplate(resource, templateName, referenceClass, model, sw, charset);
        return sw.toString();
    }
}
