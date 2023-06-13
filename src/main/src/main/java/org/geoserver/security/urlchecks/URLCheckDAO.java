/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecks;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/** This class manages persistence of {@link URLChecksConfiguration} in the Data Directory */
public class URLCheckDAO {

    static final Logger LOGGER = Logging.getLogger(URLCheckDAO.class.getCanonicalName());
    protected static final String CONFIG_FILE_NAME = "urlchecks.xml";

    private FileWatcher<URLChecksConfiguration> configurationWatcher;

    private XStreamPersister xp;

    URLChecksConfiguration configuration;

    /** Saves the checks, without altering the rest of the configuration */
    public void saveChecks(List<AbstractURLCheck> checks) throws IOException {
        URLChecksConfiguration config = getConfiguration();
        config.setChecks(checks);
        saveConfiguration(config);
    }

    /** A runnable that can throw an exception */
    private interface ThrowingRunnable {

        void run() throws IOException;
    }

    public URLCheckDAO(GeoServerDataDirectory dd, XStreamPersisterFactory xstreamPersisterFactory) {
        xp = xstreamPersisterFactory.createXMLPersister();
        XStream xs = xp.getXStream();
        xs.alias("regex", RegexURLCheck.class);
        xs.alias("checks", URLChecksConfiguration.class);
        xs.allowTypeHierarchy(AbstractURLCheck.class);
        xs.addImplicitCollection(URLChecksConfiguration.class, "checks", AbstractURLCheck.class);

        configurationWatcher = new URLChecksWatcher(dd);
    }

    /**
     * Executes a {@link ThrowingRunnable} on the configuration file, locking it to avoid concurrent
     * access
     *
     * @param action the action to execute
     * @throws IOException
     */
    private void configurationAction(ThrowingRunnable action) throws IOException {
        Resource resource = configurationWatcher.getResource();
        Resource.Lock lock = resource.lock();
        try {
            action.run();
        } finally {
            lock.release();
        }
    }

    public void saveConfiguration(URLChecksConfiguration configuration) throws IOException {
        configurationAction(
                () -> {
                    try (OutputStream fos = configurationWatcher.getResource().out()) {
                        xp.save(configuration, fos);
                    }
                });
        this.configuration = configuration;
    }

    public void save(AbstractURLCheck check) {
        try {
            URLChecksConfiguration config = getConfiguration();
            // tolerant, either add if missing, or replace in position, otherwise
            List<AbstractURLCheck> checks = new ArrayList<>(config.getChecks());
            if (!checks.stream().anyMatch(c -> c.getName().equals(check.getName()))) {
                checks.add(check);
            } else {
                checks =
                        checks.stream()
                                .map(c -> c.getName().equals(check.getName()) ? check : c)
                                .collect(Collectors.toList());
            }
            config.setChecks(checks);
            saveConfiguration(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the configuration read from the file, or a default one if the file does not exist
     *
     * @return the URL check configuration
     * @throws IOException if an error occurs while reading the configuration
     */
    public URLChecksConfiguration getConfiguration() throws IOException {
        if (configurationWatcher.isModified() || this.configuration == null) {
            configurationAction(
                    () ->
                            this.configuration =
                                    Optional.ofNullable(configurationWatcher.read())
                                            .orElseGet(() -> new URLChecksConfiguration()));
        }

        return this.configuration;
    }

    /**
     * Returns the list of URL checks from the storage
     *
     * @return the list of URL checks from the storage
     * @throws IOException
     */
    public List<AbstractURLCheck> getChecks() throws IOException {
        return getConfiguration().getChecks();
    }

    /**
     * Returns a URL check from the storage with the same name
     *
     * @param name the name of the check to retrieve
     * @return the check with the same name, or null if no check with the same name exists
     */
    public AbstractURLCheck getCheckByName(String name) throws IOException {
        return getChecks().stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }

    /**
     * Adds a URL check in the storage, checking there are no two checks with the same name
     *
     * @param check the check to add
     */
    public void add(AbstractURLCheck check) throws IOException {
        configurationAction(
                () -> {
                    URLChecksConfiguration config = getConfiguration();
                    if (config.getChecks().stream()
                            .anyMatch(c -> c.getName().equals(check.getName())))
                        throw new IllegalArgumentException(
                                "A URL check with the same name already exists: "
                                        + check.getName());
                    List<AbstractURLCheck> checks = new ArrayList<>(config.getChecks());
                    checks.add(check);
                    config.setChecks(checks);
                    saveConfiguration(configuration);
                });
    }

    /**
     * Removes a URL check from the storage, checking there is a check with the same name
     *
     * @param name the name of the check to remove
     */
    public void removeByName(String name) throws IOException {
        configurationAction(
                () -> {
                    URLChecksConfiguration config = getConfiguration();
                    List<AbstractURLCheck> checks = config.getChecks();
                    if (checks.stream().noneMatch(c -> c.getName().equals(name)))
                        throw new IllegalArgumentException(
                                "No URL check with the specified name exists: " + name);
                    checks.removeIf(c -> c.getName().equals(name));
                    saveConfiguration(configuration);
                });
    }

    /**
     * Resets the DAO, forcing it to reload the configuration from disk the next time it is accessed
     */
    public void reset() {
        this.configuration = null;
        this.configurationWatcher.setKnownLastModified(Long.MIN_VALUE);
    }

    /**
     * Returns the enabled status of the URL checks
     *
     * @return the enabled status of the URL checks
     * @throws IOException if an error occurs while reading the configuration
     */
    public boolean isEnabled() throws IOException {
        return getConfiguration().isEnabled();
    }

    public void setEnabled(boolean enabled) throws IOException {
        URLChecksConfiguration config = getConfiguration();
        config.setEnabled(enabled);
        saveConfiguration(config);
    }

    /** File watcher for the URL checks configuration file */
    private class URLChecksWatcher extends FileWatcher<URLChecksConfiguration> {
        public URLChecksWatcher(GeoServerDataDirectory dd) {
            super(dd.getSecurity(URLCheckDAO.CONFIG_FILE_NAME));
        }

        @Override
        protected URLChecksConfiguration parseFileContents(InputStream in) throws IOException {
            return xp.load(in, URLChecksConfiguration.class);
        }
    }
}
