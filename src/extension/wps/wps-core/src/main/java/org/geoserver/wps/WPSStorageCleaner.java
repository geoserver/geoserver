/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.IOException;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.ConfigurationException;
import org.geoserver.wcs.response.WCSStorageCleaner;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.util.logging.Logging;

/**
 * Cleans up the temporary storage directory for WPS, as well as the storage process statuses
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WPSStorageCleaner extends TimerTask {
    Logger LOGGER = Logging.getLogger(WCSStorageCleaner.class);

    long expirationDelay;

    WPSResourceManager resourceManager;

    ProcessStatusTracker statusTracker;

    public WPSStorageCleaner(WPSResourceManager resourceManager, ProcessStatusTracker statusTracker)
            throws IOException, ConfigurationException {
        this.resourceManager = resourceManager;
        this.statusTracker = statusTracker;
    }

    @Override
    public void run() {
        try {
            if (resourceManager.getArtifactsStore() == null || expirationDelay == 0) {
                return;
            }

            // ok, now scan for existing files there and clean up those that are too old
            long expirationThreshold = System.currentTimeMillis() - expirationDelay;
            statusTracker.cleanExpiredStatuses(expirationThreshold);
            resourceManager.cleanExpiredResources(expirationThreshold, statusTracker);
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error occurred while trying to clean up " + "old coverages from temp storage",
                    e);
        }
    }

    /**
     * The file expiration delay in milliseconds. A file will be deleted when it's been around more
     * than expirationDelay
     */
    public long getExpirationDelay() {
        return expirationDelay;
    }

    /** Sets the temp file expiration delay */
    public void setExpirationDelay(long expirationDelay) {
        this.expirationDelay = expirationDelay;
    }
}
