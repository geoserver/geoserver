/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.configuration;

import java.io.IOException;
import javax.annotation.PostConstruct;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.JMSConfigurationExt;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Configuration class used to override the default config dir (GEOSERVER_DATA_DIR/cluster/)
 *
 * @author carlo cancellieri - GeoSolutions SAS
 */
public final class ConfigDirConfiguration implements JMSConfigurationExt {

    @Autowired GeoServerResourceLoader loader;

    public static final String CONFIGDIR_KEY = "CLUSTER_CONFIG_DIR";

    /** Override the global config dir */
    @PostConstruct
    private void init() throws IOException {
        // check for override
        Resource baseDir = null;
        final String baseDirPath = JMSConfiguration.getOverride(CONFIGDIR_KEY);
        // if no override try to load from the GeoServer loader
        if (baseDirPath != null) {
            baseDir = Resources.fromPath(baseDirPath);
        } else {
            baseDir = loader.get("cluster");
        }
        JMSConfiguration.setConfigPathDir(baseDir);
    }

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        config.putConfiguration(CONFIGDIR_KEY, JMSConfiguration.getConfigPathDir().toString());
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        return config.override(CONFIGDIR_KEY, JMSConfiguration.getConfigPathDir().toString());
    }
}
