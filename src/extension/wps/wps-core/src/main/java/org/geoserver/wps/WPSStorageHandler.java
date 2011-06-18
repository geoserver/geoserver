/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.ConfigurationException;

import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.util.IOUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.response.WCSStorageCleaner;
import org.geotools.util.logging.Logging;

/**
 * Handles the temporary storage directory for WPS
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class WPSStorageHandler extends TimerTask {
    Logger LOGGER = Logging.getLogger(WCSStorageCleaner.class);

    long expirationDelay;

    private File storage;

    private String baseURL;

    public WPSStorageHandler(GeoServerDataDirectory dataDirectory) throws IOException,
            ConfigurationException {
        // get the temporary storage for WPS
        storage = dataDirectory.findOrCreateDataDir("temp/wps");
    }

    @Override
    public void run() {
        try {
            if (!storage.exists())
                return;

            // ok, now scan for existing files there and clean up those
            // that are too old
            long now = System.currentTimeMillis();
            for (File f : storage.listFiles()) {
                if (expirationDelay > 0 && now - f.lastModified() > (expirationDelay * 1000)) {
                    if (f.isFile()) {
                        f.delete();
                    } else {
                        IOUtils.delete(f);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occurred while trying to clean up "
                    + "old coverages from temp storage", e);
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
     * The file expiration delay in seconds, a file will be deleted when it's been around more than
     * expirationDelay
     * 
     * @return
     */
    public long getExpirationDelay() {
        return expirationDelay;
    }

    public void setExpirationDelay(long expirationDelay) {
        this.expirationDelay = expirationDelay;
    }

    /**
     * Given a file inside the root storage directory returns a URL to retrieve it via
     * the file publisher
     * @param file
     * @return
     * @throws MalformedURLException 
     */
    public URL getURL(File file) throws MalformedURLException {
        // initialize default value for testing
        String baseURL = "http://geoserver/fakeroot";
        if(Dispatcher.REQUEST.get()!= null) {
            baseURL = ResponseUtils.baseURL(Dispatcher.REQUEST.get().getHttpRequest());
        }
        
        String path = "temp/wps/" + storage.toURI().relativize(file.toURI()).getPath();
        return new URL(ResponseUtils.buildURL(baseURL, path, null, URLType.RESOURCE));
    }
}
