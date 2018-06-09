/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.jdbcloader.JDBCLoaderProperties;
import org.geoserver.jdbcloader.JDBCLoaderPropertiesFactoryBean;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;

/**
 * Factory to load configuration information for JDBCResourceStore
 *
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
public class JDBCResourceStorePropertiesFactoryBean extends JDBCLoaderPropertiesFactoryBean {

    private static final String PREFIX = "jdbcstore";

    /** DDL scripts copied to <data dir>/jdbcstore/scripts/ on first startup */
    private static final String[] SCRIPTS = {
        "drop.h2.sql", "drop.postgres.sql", "init.h2.sql", "init.postgres.sql"
    };

    private static final String[] SAMPLE_CONFIGS = {
        "jdbcstore.properties.h2", "jdbcstore.properties.postgres"
    };

    public JDBCResourceStorePropertiesFactoryBean(ResourceStore resourceStore) {
        super(resourceStore, PREFIX);
    }

    @Override
    protected JDBCLoaderProperties createConfig() throws IOException {
        return new JDBCResourceStoreProperties(this);
    }

    @Override
    protected String[] getScripts() {
        return SCRIPTS;
    }

    @Override
    protected String[] getSampleConfigurations() {
        return SAMPLE_CONFIGS;
    }

    @Override
    public List<Resource> getFileLocations() throws IOException {
        List<Resource> configurationFiles = new ArrayList<>();

        final Resource scriptsDir = getScriptDir();
        for (String scriptName : getScripts()) {
            configurationFiles.add(scriptsDir.get(scriptName));
        }

        final Resource baseDirectory = getBaseDir();
        for (String sampleConfig : getSampleConfigurations()) {
            configurationFiles.add(baseDirectory.get(sampleConfig));
        }

        return configurationFiles;
    }

    @Override
    public void saveConfiguration(GeoServerResourceLoader resourceLoader) throws IOException {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        for (Resource controlflow : getFileLocations()) {
            Resource targetDir =
                    Files.asResource(
                            resourceLoader.findOrCreateDirectory(
                                    Paths.convert(
                                            loader.getBaseDirectory(),
                                            controlflow.parent().dir())));

            Resources.copy(controlflow.file(), targetDir);
        }
    }

    @Override
    public void loadConfiguration(GeoServerResourceLoader resourceLoader) throws IOException {
        loadProperties(createProperties());
    }
}
