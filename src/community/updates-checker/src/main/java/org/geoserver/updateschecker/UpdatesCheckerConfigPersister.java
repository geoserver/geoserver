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
 * Reads and writes {@link UpdatesCheckerConfig} to {@code <data_dir>/updates-checker/updates-checker.xml}.
 *
 * <p>Uses GeoServer's generic {@link FileWatcher} to cache the parsed config in memory and only re-read it when its
 * last-modified time actually changes, instead of re-parsing the file on every call. {@link #getConfig()} always
 * returns a fresh {@linkplain UpdatesCheckerConfig#copy() copy} of the cached instance, never the cached instance
 * itself: callers (in particular {@code UpdatesCheckerPage}'s admin form) mutate the returned object in place before an
 * explicit {@link #save}, and sharing the live cached instance would let a concurrent viewer observe another admin's
 * half-edited, unsaved settings.
 */
public class UpdatesCheckerConfigPersister {

    private static final Logger LOGGER = Logging.getLogger(UpdatesCheckerService.LOGGER_NAME);

    private static final String CONFIG_PATH = "updates-checker/updates-checker.xml";

    private final SecureXStream xStream;

    private final FileWatcher<UpdatesCheckerConfig> watcher;

    private UpdatesCheckerConfig cache;

    public UpdatesCheckerConfigPersister(GeoServerDataDirectory dataDirectory) {
        this.xStream = new SecureXStream();
        this.xStream.alias("updatesChecker", UpdatesCheckerConfig.class);
        this.xStream.allowTypes(new Class[] {UpdatesCheckerConfig.class});
        // tolerate fields renamed/removed across module versions (e.g. an old on-disk config written
        // before a field rename) instead of failing the whole read and silently reverting to defaults -
        // same approach core GeoServer config persisters use (XStreamPersister, GWCConfigPersister)
        this.xStream.ignoreUnknownElements();

        this.watcher = new FileWatcher<>(dataDirectory.get(CONFIG_PATH)) {
            @Override
            protected UpdatesCheckerConfig parseFileContents(InputStream in) {
                return (UpdatesCheckerConfig) xStream.fromXML(in);
            }
        };
    }

    public synchronized UpdatesCheckerConfig getConfig() {
        if (cache == null || watcher.isModified()) {
            try {
                UpdatesCheckerConfig loaded = watcher.read();
                cache = loaded != null ? loaded : new UpdatesCheckerConfig();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, UpdatesCheckerMessages.base("log.configReadFailed"), e);
                cache = new UpdatesCheckerConfig();
            }
        }
        return cache.copy();
    }

    public synchronized void save(UpdatesCheckerConfig config) {
        Resource resource = watcher.getResource();
        try (OutputStream out = resource.out()) {
            xStream.toXML(config, out);
            // avoid an unnecessary reload next time getConfig() is called - the file just written
            // already matches what we're about to cache
            watcher.setKnownLastModified(resource.lastmodified());
            cache = config.copy();
        } catch (Exception e) {
            throw new RuntimeException(UpdatesCheckerMessages.base("log.configSaveFailed"), e);
        }
    }
}
