/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/**
 * Reads and writes {@link Config} as {@code <data_dir>/updates-checker/updates-checker.xml}, re-reading it only when
 * the file changes. {@link #getConfig()} returns a copy, because the settings page edits it in place before saving.
 */
public class ConfigPersister {

    private static final Logger LOGGER = Logging.getLogger(UpdatesCheckerService.LOGGER_NAME);

    private static final String CONFIG_PATH = "updates-checker/updates-checker.xml";

    private final SecureXStream xStream;

    private final FileWatcher<Config> watcher;

    private Config cache;

    public ConfigPersister(GeoServerDataDirectory dataDirectory) {
        this.xStream = new SecureXStream();
        this.xStream.alias("updatesChecker", Config.class);
        this.xStream.allowTypes(new Class[] {Config.class});
        // a file written by another version of the module may have other fields
        this.xStream.ignoreUnknownElements();

        this.watcher = new FileWatcher<>(dataDirectory.get(CONFIG_PATH)) {
            @Override
            protected Config parseFileContents(InputStream in) {
                return (Config) xStream.fromXML(in);
            }
        };
    }

    public synchronized Config getConfig() {
        if (cache == null || watcher.isModified()) {
            try {
                Config loaded = watcher.read();
                cache = loaded != null ? loaded : new Config();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, Messages.base("log.configReadFailed"), e);
                cache = new Config();
            }
        }
        return cache.copy();
    }

    public synchronized void save(Config config) {
        Resource resource = watcher.getResource();
        try (OutputStream out = resource.out()) {
            xStream.toXML(config, out);
            watcher.setKnownLastModified(resource.lastmodified());
            cache = config.copy();
        } catch (Exception e) {
            throw new RuntimeException(Messages.base("log.configSaveFailed"), e);
        }
    }
}
