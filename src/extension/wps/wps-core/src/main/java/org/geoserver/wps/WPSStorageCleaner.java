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
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.util.logging.Logging;

/**
 * Cleans up the temporary storage directory for WPS, as well as the stored process statuses.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WPSStorageCleaner extends TimerTask {

    private static final Logger LOGGER = Logging.getLogger(WPSStorageCleaner.class);

    private volatile long expirationDelay;

    // collaborators don’t change after construction
    private final WPSResourceManager resourceManager;
    private final ProcessStatusTracker statusTracker;

    public WPSStorageCleaner(WPSResourceManager resourceManager, ProcessStatusTracker statusTracker)
            throws IOException, ConfigurationException {
        this.resourceManager = resourceManager;
        this.statusTracker = statusTracker;
    }

    @Override
    public void run() {
        try {
            // snapshot the volatile once
            final long delay = this.expirationDelay;
            if (resourceManager.getArtifactsStore() == null || delay == 0L) {
                return;
            }

            // compute expiration threshold and clean
            final long expirationThreshold = System.currentTimeMillis() - delay;
            statusTracker.cleanExpiredStatuses(expirationThreshold);
            resourceManager.cleanExpiredResources(expirationThreshold, statusTracker);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occurred while trying to clean up old WPS resources from temp storage", e);
        }
    }

    /**
     * The file expiration delay in milliseconds. A file will be deleted when it's been around more than
     * {@code expirationDelay}.
     */
    public long getExpirationDelay() {
        return expirationDelay;
    }

    /** Sets the temp file expiration delay (milliseconds). */
    public void setExpirationDelay(long expirationDelay) {
        this.expirationDelay = expirationDelay;
    }
}
