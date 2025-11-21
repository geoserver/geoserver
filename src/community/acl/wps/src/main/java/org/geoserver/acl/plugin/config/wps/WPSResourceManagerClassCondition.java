/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.config.wps;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Spring without spring boot equivalent to {@code @ConditionalOnClass(WPSResourceManager.class)}, so the
 * {@code gs-acl-plugin-wps} module can be on the vanilla GeoServer classpath when the geoserver WPS extension is not
 * installed.
 */
public class WPSResourceManagerClassCondition implements ConfigurationCondition {

    private static ClassLoader classLoader;

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.PARSE_CONFIGURATION;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            classLoader().loadClass("org.geoserver.wps.resource.WPSResourceManager");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private ClassLoader classLoader() {
        if (null == classLoader) {
            classLoader = getClass().getClassLoader();
        }
        return classLoader;
    }

    @VisibleForTesting
    public static void classLoader(ClassLoader cl) {
        classLoader = cl;
    }
}
