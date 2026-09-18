/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Whether GeoFence is usable right now, shared by both {@link RuleReaderServiceFactory} instances.
 *
 * <p>Shared rather than per-factory: the access manager asks the frontend, so registering a source on the backend alone
 * left a cache-less frontend still serving grants.
 */
public class RuleReaderAvailability {

    private static final Logger LOGGER = Logging.getLogger(RuleReaderAvailability.class);

    /** While non-empty, deny-all is served; each entry reports whether its source has become usable again. */
    private final Map<String, BooleanSupplier> pendingRecoveries = new ConcurrentHashMap<>();

    /** Serves deny-all until {@code recovery} reports {@code source} usable again; every source must recover. */
    public void denyUntilRecovered(String source, BooleanSupplier recovery) {
        pendingRecoveries.put(source, recovery);
    }

    /** Whether rule readers may be served. Retries pending sources once per call, so recovery needs no reload. */
    public boolean isAvailable() {
        if (pendingRecoveries.isEmpty()) {
            return true;
        }
        pendingRecoveries.entrySet().removeIf(entry -> entry.getValue().getAsBoolean());
        if (pendingRecoveries.isEmpty()) {
            LOGGER.log(Level.INFO, "GeoFence rule reader recovered; resuming normal access");
            return true;
        }
        return false;
    }
}
