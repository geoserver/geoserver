/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static com.google.common.base.Preconditions.checkNotNull;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;

/**
 * Loads and saves the integrated GWC configuration at {@code <data dir>/gwc-gs.xml}
 *
 * @author groldan
 */
public class GWCConfigPersister {

    private static final Logger LOGGER = Logging.getLogger(GWCConfigPersister.class);

    static final String GWC_CONFIG_FILE = "gwc-gs.xml";

    private final XStreamPersisterFactory persisterFactory;

    private final GeoServerResourceLoader resourceLoader;

    private GWCConfig config;

    public GWCConfigPersister(
            final XStreamPersisterFactory xspf, final GeoServerResourceLoader resourceLoader) {
        this.persisterFactory = xspf;
        this.resourceLoader = resourceLoader;
    }

    /** @return the config file or {@code null} if it does not exist */
    Resource findConfigFile() throws IOException {
        final Resource configFile = resourceLoader.get(GWC_CONFIG_FILE);
        return configFile;
    }

    public GWCConfig getConfig() {
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
        Resource configFile = findConfigFile();
        checkNotNull(configFile, "gwc config file does not exist: ", GWC_CONFIG_FILE);

        XStreamPersister xmlPersister = this.persisterFactory.createXMLPersister();
        configureXstream(xmlPersister.getXStream());
        try {
            try (InputStream in = configFile.in()) {
                this.config = xmlPersister.load(in, GWCConfig.class);
            }
            LOGGER.fine("GWC GeoServer specific configuration loaded from " + GWC_CONFIG_FILE);
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error loading GWC GeoServer specific "
                            + "configuration from "
                            + configFile.path()
                            + ". Applying defaults.",
                    e);
            this.config = new GWCConfig();
        }
    }

    /**
     * Saves and applies the integrated GWC's GeoServer specific configuration to the {@code <data
     * dir>/gwc-gs.xml} file.
     */
    public void save(final GWCConfig config) throws IOException {
        LOGGER.finer("Saving integrated GWC configuration");
        Resource tmp = getConfigRoot().get(GWC_CONFIG_FILE + ".tmp");
        XStreamPersister xmlPersister = this.persisterFactory.createXMLPersister();
        configureXstream(xmlPersister.getXStream());
        OutputStream out = tmp.out();
        try {
            xmlPersister.save(config, out);
        } finally {
            out.close();
        }
        Resource configFile = getConfigRoot().get(GWC_CONFIG_FILE);
        tmp.renameTo(configFile);
        this.config = config;
        LOGGER.finer("Integrated GWC configuration saved to " + configFile.path());
    }

    private void configureXstream(XStream xs) {
        xs.alias("GeoServerGWCConfig", GWCConfig.class);
        xs.alias("defaultCachingGridSetIds", HashSet.class);
        xs.alias("defaultCoverageCacheFormats", HashSet.class);
        xs.alias("defaultVectorCacheFormats", HashSet.class);
        xs.alias("defaultOtherCacheFormats", HashSet.class);
        xs.alias("InnerCacheConfiguration", CacheConfiguration.class);
        xs.allowTypes(new Class[] {GWCConfig.class, CacheConfiguration.class});
    }

    private Resource getConfigRoot() {
        return this.resourceLoader.get(Paths.BASE);
    }
}
