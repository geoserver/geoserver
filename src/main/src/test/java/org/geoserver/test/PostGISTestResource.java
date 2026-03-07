/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Assume;
import org.junit.rules.ExternalResource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostGISTestResource extends ExternalResource {

    private PostgreSQLContainer postgisContainer;

    @Override
    public void before() throws Throwable {
        Assume.assumeTrue(
                "Docker is required for PostGIS tests",
                org.testcontainers.DockerClientFactory.instance().isDockerAvailable());

        DockerImageName image =
                DockerImageName.parse("postgis/postgis:18-3.6-alpine").asCompatibleSubstituteFor("postgres");
        // since it's a lightweight image used only for tests, we disable some durability features in exchange for speed
        postgisContainer = new PostgreSQLContainer(image)
                .withCommand(
                        "postgres", "-c", "fsync=off", "-c", "synchronous_commit=off", "-c", "full_page_writes=off");
        postgisContainer.start();
    }

    @Override
    public void after() {
        if (postgisContainer != null) {
            postgisContainer.stop();
        }
    }

    public Map<String, Serializable> getConnectionParameters() {
        Map<String, Serializable> params = new HashMap<>();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
        params.put(PostgisNGDataStoreFactory.HOST.key, "localhost");
        params.put(PostgisNGDataStoreFactory.DATABASE.key, postgisContainer.getDatabaseName());
        params.put(PostgisNGDataStoreFactory.PORT.key, postgisContainer.getMappedPort(5432));
        params.put(PostgisNGDataStoreFactory.USER.key, postgisContainer.getUsername());
        params.put(PostgisNGDataStoreFactory.PASSWD.key, postgisContainer.getPassword());
        return params;
    }

    public JDBCDataStore createDataStore() throws IOException {
        return (JDBCDataStore) DataStoreFinder.getDataStore(getConnectionParameters());
    }

    public PostgreSQLContainer getContainer() {
        return postgisContainer;
    }
}
