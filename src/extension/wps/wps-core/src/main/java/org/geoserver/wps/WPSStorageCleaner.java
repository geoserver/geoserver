/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.ConfigurationException;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.response.WCSStorageCleaner;
import org.geotools.util.logging.Logging;

/**
 * Cleans up the temporary storage directory for WPS
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class WPSStorageCleaner extends TimerTask {
    Logger LOGGER = Logging.getLogger(WCSStorageCleaner.class);

    long expirationDelay;

    private File storage;

    private String baseURL;

    Set<File> lockedFiles = Collections.newSetFromMap(new ConcurrentHashMap<File, Boolean>());

    public WPSStorageCleaner(GeoServerDataDirectory dataDirectory) throws IOException,
            ConfigurationException {
        // get the temporary storage for WPS
        storage = dataDirectory.findOrCreateDir("temp/wps");
    }

    @Override
    public void run() {
        try {
            if (!storage.exists())
                return;

            // ok, now scan for existing files there and clean up those
            // that are too old
            long now = System.currentTimeMillis();
            cleanupDirectory(storage, now);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occurred while trying to clean up "
                    + "old coverages from temp storage", e);
        }
    }

    /**
     * Recursively cleans up files that are too old
     * 
     * @param directory
     * @param now
     * @throws IOException
     */
    private void cleanupDirectory(File directory, long now) throws IOException {
        for (File f : directory.listFiles()) {
            // skip locked files, someone is downloading them
            if (lockedFiles.contains(f)) {
                continue;
            }
            // cleanup directories recursively
            if (f.isDirectory()) {
                cleanupDirectory(f, now);
                // make sure we delete the directory only if enough time elapsed, since
                // it might have been just created to store some wps outputs
                if (f.list().length == 0 && f.lastModified() > expirationDelay) {
                    f.delete();
                }
            } else {
                if (expirationDelay > 0 && now - f.lastModified() > expirationDelay) {
                    if (f.isFile()) {
                        f.delete();
                    }
                }
            }
        }
    }

    /**
     * Returns the storage directory for WPS
     * 
     * @return
     */
    public File getStorage() {
        return storage;
    }

    /**
     * The file expiration delay in milliseconds. A file will be deleted when it's been around more
     * than expirationDelay
     * 
     * @return
     */
    public long getExpirationDelay() {
        return expirationDelay;
    }

    /**
     * Sets the temp file expiration delay
     * @param expirationDelay
     */
    public void setExpirationDelay(long expirationDelay) {
        this.expirationDelay = expirationDelay;
    }

    /**
     * Given a file inside the root storage directory returns a URL to retrieve it via the file
     * publisher
     * 
     * @param file
     * @return
     * @throws MalformedURLException
     */
    public URL getURL(File file) throws MalformedURLException {
        // initialize default value for testing
        String baseURL = "http://geoserver/fakeroot";
        if (Dispatcher.REQUEST.get() != null) {
            baseURL = ResponseUtils.baseURL(Dispatcher.REQUEST.get().getHttpRequest());
        }

        String path = "temp/wps/" + storage.toURI().relativize(file.toURI()).getPath();
        return new URL(ResponseUtils.buildURL(baseURL, path, null, URLType.RESOURCE));
    }

    /**
     * Locks a file that is being accessed, preventing it from being deleted
     * 
     * @param file
     */
    public void lock(File file) {
        this.lockedFiles.add(file);
    }

    /**
     * Unlocks a previously locked file, making it eligible for expiration again
     * 
     * @param file
     */
    public void unlock(File file) {
        this.lockedFiles.remove(file);
    }
}
