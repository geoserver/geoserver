/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.restconfig.client;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assume;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * JUnit Rule that manages a GeoServer container for integration tests.
 *
 * <p>This class starts a GeoServer Docker container using Testcontainers and provides convenient access to the
 * container's API URL and credentials.
 *
 * <p>Usage example:
 *
 * <pre>
 * public @Rule GeoServerContainer geoserver = new GeoServerContainer();
 * </pre>
 */
@Slf4j
public class GeoServerContainer extends ExternalResource {

    private static final String GEOSERVER_IMAGE = "docker.osgeo.org/geoserver:3.0.x";
    private static final int GEOSERVER_PORT = 8080;
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "geoserver";

    private GenericContainer<?> container;

    private boolean canPullImage(String imageName) {
        try {
            org.testcontainers.DockerClientFactory.lazyClient()
                    .pullImageCmd(imageName)
                    .start()
                    .awaitCompletion();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void before() throws Throwable {
        Assume.assumeTrue(
                "Docker is required for GeoServer integration tests",
                org.testcontainers.DockerClientFactory.instance().isDockerAvailable());
        Assume.assumeTrue("Skipping: Docker image not available — " + GEOSERVER_IMAGE, canPullImage(GEOSERVER_IMAGE));

        log.info("Starting GeoServer container from image: {}", GEOSERVER_IMAGE);

        container = new GenericContainer<>(DockerImageName.parse(GEOSERVER_IMAGE))
                .withExposedPorts(GEOSERVER_PORT)
                .waitingFor(
                        Wait.forHttp("/geoserver/web/wicket/resource/org.geoserver.web.GeoServerBasePage/img/logo.png")
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofSeconds(120)));

        container.start();

        log.info("GeoServer container started. API URL: {}", getApiUrl());
    }

    @Override
    protected void after() {
        if (container != null) {
            log.info("Stopping GeoServer container");
            container.stop();
        }
    }

    /** @return the base URL for the GeoServer REST API */
    public String getApiUrl() {
        return String.format(
                "http://%s:%d/geoserver/rest", container.getHost(), container.getMappedPort(GEOSERVER_PORT));
    }

    /** @return the base URL for the GeoServer web interface */
    public String getWebUrl() {
        return String.format(
                "http://%s:%d/geoserver/web", container.getHost(), container.getMappedPort(GEOSERVER_PORT));
    }

    /** @return the admin username */
    public String getAdminUser() {
        return ADMIN_USER;
    }

    /** @return the admin password */
    public String getAdminPassword() {
        return ADMIN_PASSWORD;
    }

    /** @return true if the container is running */
    public boolean isRunning() {
        return container != null && container.isRunning();
    }
}
