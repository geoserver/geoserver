/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.template.DirectTemplateFeatureCollectionFactory;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;
import org.geotools.util.SoftValueHashMap;

/** Support class that locates the templates based on the current collection */
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
     * Returns the template for the specified collection.
     *
     * @param collection The collection for which this template is being loaded. Will make it look
     *     for a <code>templateName-collection.ftl</code> template first, falling back on <code>
     *     templateName.ftl</code> if not found
     * @param templateName The name of the template, without extension
     * @param clazz The reference class for classpath relative look-ups
     */
    public Template getTemplate(String collection, String templateName, Class<?> clazz)
            throws IOException {
        GeoServerTemplateLoader templateLoader =
                new GeoServerTemplateLoader(clazz, resourceLoader) {
                    @Override
                    public Object findTemplateSource(String path) throws IOException {
                        Object source = null;

                        source = super.findTemplateSource("os-eo/" + path);

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

        WorkspaceInfo ws = LocalWorkspace.get();
        if (ws != null) {
            templateLoader.setWorkspace(ws);
        }

        // Configuration is not thread safe
        Configuration configuration = getTemplateConfiguration(clazz);
        synchronized (configuration) {
            configuration.setTemplateLoader(templateLoader);
            if (collection != null) {
                Template t =
                        configuration.getTemplate(
                                templateName + "-" + collection + ".ftl",
                                null,
                                null,
                                null,
                                true,
                                true);
                if (t != null) return t;
            }
            return configuration.getTemplate(templateName + ".ftl");
        }
    }

    Configuration getTemplateConfiguration(Class<?> clazz) {
        return configurationCache.computeIfAbsent(
                clazz,
                k -> {
                    Configuration cfg = TemplateUtils.getSafeConfiguration();
                    cfg.setObjectWrapper(new FeatureWrapper(FC_FACTORY));
                    cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
                    return cfg;
                });
    }
}
