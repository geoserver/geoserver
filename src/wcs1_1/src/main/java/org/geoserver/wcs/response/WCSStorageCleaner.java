/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.io.File;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Cleans up the contents of ${GEOSERVER_DATA_DIR}/temp/wcs, by removing all files that
 * have been in the temp folder for too long
 * @author Andrea Aime - TOPP
 *
 */
public class WCSStorageCleaner extends TimerTask {
    Logger LOGGER = Logging.getLogger(WCSStorageCleaner.class);

    long expirationDelay;

    @Override
    public void run() {
        try {
            // first check that temp/wcs is really there in the data dir
            File temp = GeoserverDataDirectory.findConfigDir(GeoserverDataDirectory
                    .getGeoserverDataDirectory(), "temp");
            if (temp == null || !temp.exists())
                return;

            File wcsTemp = new File(temp, "wcs");
            if (!wcsTemp.exists())
                return;

            // ok, now scan for existing files there and clean up those 
            // that are too old
            long now = System.currentTimeMillis();
            for(File f : wcsTemp.listFiles()) {
                if(now - f.lastModified() > (expirationDelay * 1000))
                    f.delete();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occurred while trying to clean up "
                    + "old coverages from temp storage", e);
        }
    }

    /**
     * The file expiration delay in seconds, a file will be deleted when
     * it's been around more than expirationDelay
     * 
     * @return
     */
    public long getExpirationDelay() {
        return expirationDelay;
    }

    public void setExpirationDelay(long expirationDelay) {
        this.expirationDelay = expirationDelay;
    }

}
