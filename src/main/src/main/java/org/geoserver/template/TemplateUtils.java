/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.template;

import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.utility.ClassUtil;
import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for Freemarker template configuration
 *
 * @author Kevin Smith, Boundless
 */
public class TemplateUtils {
    /** Classes that should not be resolved in Freemarker templates */
    private static final Collection<String> ILLEGAL_FREEMARKER_CLASSES =
            Arrays.asList(
                    freemarker.template.utility.ObjectConstructor.class.getName(),
                    freemarker.template.utility.Execute.class.getName(),
                    "freemarker.template.utility.JythonRuntime");

    /**
     * Classes that should be resolved in Freemarker templates, even if they would not be by default
     */
    private static final Collection<String> LEGAL_FREEMARKER_CLASSES = Arrays.asList();

    /** Get a Freemarker configuration that is safe against malicious templates */
    public static Configuration getSafeConfiguration() {
        Configuration config = new Configuration();
        config.setNewBuiltinClassResolver(
                (name, env, template) -> {
                    if (ILLEGAL_FREEMARKER_CLASSES.stream().anyMatch(name::equals)) {
                        throw new TemplateException(
                                String.format(
                                        "Class %s is not allowed in Freemarker templates", name),
                                env);
                    }
                    if (LEGAL_FREEMARKER_CLASSES.stream().anyMatch(name::equals)) {
                        try {
                            ClassUtil.forName(name);
                        } catch (ClassNotFoundException e) {
                            throw new TemplateException(e, env);
                        }
                    }

                    return TemplateClassResolver.SAFER_RESOLVER.resolve(name, env, template);
                });
        return config;
    }
}
