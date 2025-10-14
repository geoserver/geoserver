/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.template;

import static freemarker.ext.beans.BeansWrapper.EXPOSE_SAFE;
import static org.geoserver.template.GeoServerMemberAccessPolicy.DEFAULT_ACCESS;
import static org.geoserver.template.GeoServerMemberAccessPolicy.LIMIT_ACCESS;

import freemarker.core.TemplateClassResolver;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.MemberAccessPolicy;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import freemarker.template.utility.ClassUtil;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for Freemarker template configuration
 *
 * @author Kevin Smith, Boundless
 */
public class TemplateUtils {
    /** Reference feature version for Freemarker templates */
    public static final Version FM_VERSION = Configuration.VERSION_2_3_0;

    /** Classes that should not be resolved in Freemarker templates */
    private static final Collection<String> ILLEGAL_FREEMARKER_CLASSES = Arrays.asList(
            freemarker.template.utility.ObjectConstructor.class.getName(),
            freemarker.template.utility.Execute.class.getName(),
            "freemarker.template.utility.JythonRuntime");

    /** Classes that should be resolved in Freemarker templates, even if they would not be by default */
    private static final Collection<String> LEGAL_FREEMARKER_CLASSES = Arrays.asList();

    /** Get a Freemarker configuration that is safe against malicious templates */
    public static Configuration getSafeConfiguration() {
        return getSafeConfiguration(null, null, null);
    }

    /**
     * Get a Freemarker configuration that is safe against malicious templates
     *
     * @param wrapper the wrapper to be modified; a new {@link DefaultObjectWrapper} will be created if this is
     *     {@code null}
     * @param policy the access policy to be used; a default access policy will be assigned if this is {@code null}
     *     based on the specified {@code exposureLevel}
     * @param exposureLevel should be one of {@link BeansWrapper#EXPOSE_SAFE},
     *     {@link BeansWrapper#EXPOSE_PROPERTIES_ONLY} or {@link BeansWrapper#EXPOSE_NOTHING}, {@code EXPOSE_SAFE} is
     *     the default if this is {@code null}
     */
    public static Configuration getSafeConfiguration(
            BeansWrapper wrapper, MemberAccessPolicy policy, Integer exposureLevel) {
        Configuration templateConfig = new Configuration(FM_VERSION);
        templateConfig.setDefaultEncoding(StandardCharsets.UTF_8.name());
        templateConfig.setNewBuiltinClassResolver((name, env, template) -> {
            if (ILLEGAL_FREEMARKER_CLASSES.stream().anyMatch(name::equals)) {
                throw new TemplateException("Class %s is not allowed in Freemarker templates".formatted(name), env);
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
        templateConfig.setObjectWrapper(getSafeWrapper(wrapper, policy, exposureLevel));
        return templateConfig;
    }

    /**
     * Get a Freemarker object wrapper that is safe against malicious templates
     *
     * @param wrapper the wrapper to be modified; a new {@link DefaultObjectWrapper} will be created if this is
     *     {@code null}
     * @param policy the access policy to be used; a default access policy will be assigned if this is {@code null}
     *     based on the specified {@code exposureLevel}
     * @param exposureLevel should be one of {@link BeansWrapper#EXPOSE_SAFE},
     *     {@link BeansWrapper#EXPOSE_PROPERTIES_ONLY} or {@link BeansWrapper#EXPOSE_NOTHING}, {@code EXPOSE_SAFE} is
     *     the default if this is {@code null}
     */
    public static BeansWrapper getSafeWrapper(BeansWrapper wrapper, MemberAccessPolicy policy, Integer exposureLevel) {
        wrapper = wrapper != null ? wrapper : new DefaultObjectWrapper(FM_VERSION);
        int level = exposureLevel != null ? exposureLevel : EXPOSE_SAFE;
        wrapper.setMemberAccessPolicy(policy != null ? policy : (level <= EXPOSE_SAFE ? DEFAULT_ACCESS : LIMIT_ACCESS));
        wrapper.setExposureLevel(level);
        return wrapper;
    }
}
