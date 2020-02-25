/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.restupload;

import java.io.IOException;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Class used for cleaning the resources in the temporary directory after that the "expirationDelay"
 * has passed
 *
 * @author Nicola Lagomarsini
 */
public class ResumableUploadResourceCleaner extends TimerTask {
    /** LOGGER class */
    private Logger LOGGER = Logging.getLogger(ResumableUploadResourceCleaner.class);

    /** Parameter indicating the cleaning period in milliseconds */
    private Long expirationDelay;

    /** Manager handling all the REST upload operations */
    private ResumableUploadResourceManager resourceManager;

    public ResumableUploadResourceCleaner(
            ResumableUploadResourceManager resourceManager, Long expirationDelay)
            throws IOException {
        this.resourceManager = resourceManager;
        this.expirationDelay = expirationDelay;
    }

    @Override
    public void run() {
        try {
            if (!resourceManager.hasAnyResource() || expirationDelay == 0) {
                return;
            }

            long expirationThreshold = System.currentTimeMillis() - expirationDelay;
            resourceManager.cleanExpiredResources(expirationThreshold);
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

    /**
     * Sets the file expiration delay
     *
     */
    public void setExpirationDelay(long expirationDelay) {
        this.expirationDelay = expirationDelay;
    }
}
