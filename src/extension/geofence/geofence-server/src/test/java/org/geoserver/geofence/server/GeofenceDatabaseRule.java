/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server;

import org.geofence.core.db.GeofenceTestDatabase;
import org.junit.Assume;
import org.junit.rules.ExternalResource;

/**
 * Points {@code GEOFENCE_DATASOURCE_FILE} at a Testcontainers Postgres, skipping the whole test class when Docker is
 * unavailable (Windows CI, for one) rather than failing it.
 *
 * <p>Declare it as a {@code @ClassRule} so it runs before GeoServer refreshes the application context.
 */
public class GeofenceDatabaseRule extends ExternalResource {

    @Override
    protected void before() {
        Assume.assumeTrue("Docker is required for the GeoFence database", GeofenceTestDatabase.isDockerAvailable());
        GeofenceTestDatabase.configureAsDatasourceOverride();
    }
}
