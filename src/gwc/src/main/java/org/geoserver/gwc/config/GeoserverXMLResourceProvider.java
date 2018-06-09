/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.Filter;
import org.geoserver.util.IOUtils;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.ConfigurationResourceProvider;
import org.geowebcache.config.XMLFileResourceProvider;
import org.geowebcache.storage.DefaultStorageFinder;

public class GeoserverXMLResourceProvider implements ConfigurationResourceProvider {

    private static Log LOGGER = LogFactory.getLog(GeoserverXMLResourceProvider.class);

    static final String GEOWEBCACHE_CONFIG_DIR_PROPERTY =
            XMLFileResourceProvider.GWC_CONFIG_DIR_VAR;

    static final String GEOWEBCACHE_CACHE_DIR_PROPERTY = DefaultStorageFinder.GWC_CACHE_DIR;

    public static final String DEFAULT_CONFIGURATION_DIR_NAME = "gwc";

    /** Location of the configuration file */
    private final Resource configDirectory;

    /** Name of the configuration file */
    private final String configFileName;

    private String templateLocation;

    public GeoserverXMLResourceProvider(
            String providedConfigDirectory, String configFileName, ResourceStore resourceStore)
            throws ConfigurationException {
        this.configFileName = configFileName;
        this.configDirectory = inferConfigDirectory(resourceStore, providedConfigDirectory);
        LOGGER.info(
                String.format(
                        "Will look for '%s' in directory '%s'.",
                        configFileName, configDirectory.dir().getAbsolutePath()));
    }

    public GeoserverXMLResourceProvider(
            final String configFileName, final ResourceStore resourceStore)
            throws ConfigurationException {
        this(null, configFileName, resourceStore);
    }

    /**
     * Helper method that infers the directory that contains or will contain GWC configuration.
     * First we will check if a specific location was set using properties GEOWEBCACHE_CONFIG_DIR
     * and GEOWEBCACHE_CACHE_DIR, then we will check if a location was provided and then fallback on
     * the default location.
     */
    private static Resource inferConfigDirectory(
            ResourceStore resourceStore, String providedConfigDirectory) {
        // check if a specific location was provided using a context property otherwise use the
        // provided directory
        String configDirectoryPath =
                findFirstDefined(GEOWEBCACHE_CONFIG_DIR_PROPERTY, GEOWEBCACHE_CACHE_DIR_PROPERTY)
                        .orElse(providedConfigDirectory);
        // if the configuration directory stills not defined we use the default location
        if (configDirectoryPath == null) {
            configDirectoryPath = DEFAULT_CONFIGURATION_DIR_NAME;
        }
        // instantiate a resource for the configuration directory
        File configurationDirectory = new File(configDirectoryPath);
        if (configurationDirectory.isAbsolute()) {
            return Resources.fromPath(configurationDirectory.getAbsolutePath());
        }
        // configuration directory path is relative to geoserver data directory
        return resourceStore.get(configDirectoryPath);
    }

    /**
     * Returns an {@link Optional} containing the value of the first defined property, or an empty
     * {@code Optional} if no property is defined in the current context.
     */
    private static Optional<String> findFirstDefined(String... propertiesNames) {
        for (String propertyName : propertiesNames) {
            // looks the property using GeoServer extensions mechanism
            String propertyValue = GeoServerExtensions.getProperty(propertyName);
            if (propertyValue != null) {
                // this property is defined so let's use is value
                LOGGER.debug(
                        String.format(
                                "Property '%s' is set with value '%s'.",
                                propertyName, propertyValue));
                return Optional.of(propertyValue);
            }
        }
        // no property is defined
        return Optional.empty();
    }

    public Resource getConfigDirectory() {
        return configDirectory;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    @Override
    public InputStream in() throws IOException {
        return findOrCreateConfFile().in();
    }

    @Override
    public OutputStream out() throws IOException {
        return findOrCreateConfFile().out();
    }

    @Override
    public void backup() throws IOException {
        backUpConfig(findOrCreateConfFile());
    }

    @Override
    public String getId() {
        return configDirectory.path();
    }

    @Override
    public void setTemplate(final String templateLocation) {
        this.templateLocation = templateLocation;
    }

    private Resource findConfigFile() throws IOException {
        return configDirectory.get(configFileName);
    }

    public String getLocation() throws IOException {
        return findConfigFile().path();
    }

    private Resource findOrCreateConfFile() throws IOException {
        Resource xmlFile = findConfigFile();

        if (Resources.exists(xmlFile)) {
            LOGGER.info("Found configuration file in " + configDirectory.path());
        } else if (templateLocation != null) {
            LOGGER.warn(
                    "Found no configuration file in config directory, will create one at '"
                            + xmlFile.path()
                            + "' from template "
                            + getClass().getResource(templateLocation).toExternalForm());
            // grab template from classpath
            try {
                IOUtils.copy(getClass().getResourceAsStream(templateLocation), xmlFile.out());
            } catch (IOException e) {
                throw new IOException("Error copying template config to " + xmlFile.path(), e);
            }
        }

        return xmlFile;
    }

    private void backUpConfig(final Resource xmlFile) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss").format(new Date());
        String backUpFileName = "geowebcache_" + timeStamp + ".bak";
        Resource parentFile = xmlFile.parent();

        LOGGER.debug("Backing up config file " + xmlFile.name() + " to " + backUpFileName);

        List<Resource> previousBackUps =
                Resources.list(
                        parentFile,
                        new Filter<Resource>() {
                            public boolean accept(Resource res) {
                                if (configFileName.equals(res.name())) {
                                    return false;
                                }
                                if (res.name().startsWith(configFileName)
                                        && res.name().endsWith(".bak")) {
                                    return true;
                                }
                                return false;
                            }
                        });

        final int maxBackups = 10;
        if (previousBackUps.size() > maxBackups) {
            Collections.sort(
                    previousBackUps,
                    new Comparator<Resource>() {

                        @Override
                        public int compare(Resource o1, Resource o2) {
                            return (int) (o1.lastmodified() - o2.lastmodified());
                        }
                    });
            Resource oldest = previousBackUps.get(0);
            LOGGER.debug(
                    "Deleting oldest config backup "
                            + oldest
                            + " to keep a maximum of "
                            + maxBackups
                            + " backups.");
            oldest.delete();
        }

        Resource backUpFile = parentFile.get(backUpFileName);
        IOUtils.copy(xmlFile.in(), backUpFile.out());
        LOGGER.debug("Config backup done");
    }

    @Override
    public boolean hasInput() {
        try {
            return Resources.exists(findOrCreateConfFile());
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean hasOutput() {
        return true;
    }
}
