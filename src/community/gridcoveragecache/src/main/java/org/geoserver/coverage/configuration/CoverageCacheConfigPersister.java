/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage.configuration;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

import com.thoughtworks.xstream.XStream;

/**
 * Loads and saves the integrated GWC configuration at {@code <data dir>/gwc-gs.xml}
 * 
 * @author groldan
 * 
 */
public class CoverageCacheConfigPersister {

    private static final Logger LOGGER = Logging.getLogger(CoverageCacheConfigPersister.class);

    static final String COVERAGE_CACHE_CONFIG_FILE = "coverage-cache.xml";

    private final XStreamPersisterFactory persisterFactory;

    private final GeoServerResourceLoader resourceLoader;

    private CoverageCacheConfig config;

    public CoverageCacheConfigPersister(final XStreamPersisterFactory xspf,
            final GeoServerResourceLoader resourceLoader) {
        this.persisterFactory = xspf;
        this.resourceLoader = resourceLoader;
    }

    /**
     * @return the config file or {@code null} if it does not exist
     * @throws IOException
     */
    File findConfigFile() throws IOException {
        final File configFile = resourceLoader.find(COVERAGE_CACHE_CONFIG_FILE);
        return configFile;
    }

    public CoverageCacheConfig getConfiguration() {
        if (config == null) {
            try {
                loadConfig();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return config;
    }

    private synchronized void loadConfig() throws IOException {
        File configFile = findConfigFile();
        checkNotNull(configFile, "coverage cache config file does not exist: ", COVERAGE_CACHE_CONFIG_FILE);

        XStreamPersister xmlPersister = this.persisterFactory.createXMLPersister();
        configureXstream(xmlPersister.getXStream());
        try {
            InputStream in = new FileInputStream(configFile);
            try {
                this.config = xmlPersister.load(in, CoverageCacheConfig.class);
            } finally {
                in.close();
            }
            LOGGER.fine("GWC GeoServer specific configuration loaded from " + COVERAGE_CACHE_CONFIG_FILE);
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error loading GWC GeoServer specific " + "configuration from "
                            + configFile.getAbsolutePath() + ". Applying defaults.", e);
            this.config = new CoverageCacheConfig();
        }
    }

    /**
     * Saves and applies the integrated GWC's GeoServer specific configuration to the
     * {@code <data dir>/gwc-gs.xml} file.
     * 
     * @param config
     * 
     * @throws IOException
     */
    public void save(final CoverageCacheConfig config) throws IOException {
        LOGGER.finer("Saving integrated GWC configuration");
        File tmp = new File(getConfigRoot(), COVERAGE_CACHE_CONFIG_FILE + ".tmp");
        XStreamPersister xmlPersister = this.persisterFactory.createXMLPersister();
        configureXstream(xmlPersister.getXStream());
        OutputStream out = new FileOutputStream(tmp);
        try {
            xmlPersister.save(config, out);
        } finally {
            out.close();
        }
        File configFile = new File(getConfigRoot(), COVERAGE_CACHE_CONFIG_FILE);
        IOUtils.rename(tmp, configFile);
        this.config = config;
        LOGGER.finer("Integrated GWC configuration saved to " + configFile.getAbsolutePath());
    }

    private void configureXstream(XStream xs) {
        xs.alias("CoverageCacheConfig", CoverageCacheConfig.class);
        xs.alias("defaultCachingGridSetIds", HashSet.class);
    }

    private File getConfigRoot() {
        return this.resourceLoader.getBaseDirectory();
    }
}
