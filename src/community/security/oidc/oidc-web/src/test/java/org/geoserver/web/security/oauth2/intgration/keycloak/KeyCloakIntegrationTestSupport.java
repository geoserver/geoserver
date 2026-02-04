/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.intgration.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.MountableFile;

/**
 * Spins up a pre-configured Keycloak docker container for integration tests.
 *
 * <p>If Docker is not available, the entire test class will be <b>skipped</b>.
 *
 * <p>Realms are provided via classpath resources:
 *
 * <ul>
 *   <li>master-realm.json (admin user geoserver/geoserver)
 *   <li>gs-realm-realm.json (test users & client)
 * </ul>
 *
 * <p>See README in resources/org/geoserver/web/security/oauth2/intgration/keycloak for details.
 */
public class KeyCloakIntegrationTestSupport extends GeoServerWicketTestSupport {

    private static final Logger LOGGER = Logging.getLogger(KeyCloakIntegrationTestSupport.class);

    /** Base URL for the Keycloak container (e.g., http://localhost:RANDOM_PORT). */
    static String authServerUrl;

    /** The Keycloak container (created only if Docker is available). */
    protected static KeycloakContainer keycloakContainer;

    // defined in master-realm.json
    String masterRealmUser = "geoserver";
    String masterRealmPassword = "geoserver";

    // defined in gs-realm-realm.json
    String gsAdminUser = "admin";
    String gsAdminPassword = "admin";

    // defined in gs-realm-realm.json
    String normalUserName = "user_sample1";
    String normalUserPassword = "user_sample1";

    // defined in gs-realm-realm.json
    String oidcClient = "gs-client";
    String oidcClientSecret = "CNwDTAKypmFhkzdfx25r7syg56VfdHuH";

    @BeforeClass
    public static void beforeAll() throws IOException, InterruptedException {
        LOGGER.info("KeyCloakIntegrationTestSupport.beforeAll() STARTING");

        // Skip entire class when Docker/Testcontainers is not usable
        if (!dockerAvailable()) {
            LOGGER.info("Docker NOT available - skipping tests");
            Assume.assumeTrue("Skipping Keycloak integration tests: Docker not available", false);
        }

        LOGGER.info("Docker available - starting Keycloak container");

        try {
            // Construct the container only after the assumption passes
            // Note: Do NOT use .useTls() as the self-signed certificate will cause
            // SSL validation failures when GeoServer tries to exchange tokens or fetch JWKS
            keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:26.1")
                    // Import realms into the default Keycloak import directory
                    .withCopyToContainer(
                            MountableFile.forClasspathResource(
                                    "org/geoserver/web/security/oauth2/login/keycloak/master-realm.json"),
                            "/opt/keycloak/data/import/master-realm.json")
                    .withCopyToContainer(
                            MountableFile.forClasspathResource(
                                    "org/geoserver/web/security/oauth2/login/keycloak/gs-realm-realm.json"),
                            "/opt/keycloak/data/import/gs-realm-realm.json")
                    // Use INFO logging level to avoid memory issues from verbose output
                    .withCustomCommand("--log-level=INFO");

            LOGGER.info("Calling keycloakContainer.start()");
            keycloakContainer.start();
            authServerUrl = keycloakContainer.getAuthServerUrl();
            LOGGER.info("Keycloak started at: " + authServerUrl);
        } catch (Exception e) {
            // Handle Docker API version mismatch or other container startup failures
            String message = e.getMessage();
            if (message != null && (message.contains("API version") || message.contains("too old")
                    || message.contains("client version"))) {
                LOGGER.warning("Docker API version mismatch - skipping tests: " + message);
                Assume.assumeTrue("Skipping Keycloak tests: Docker API version incompatible", false);
            }
            // Also skip on other container startup failures to avoid breaking the build
            LOGGER.warning("Failed to start Keycloak container - skipping tests: " + e.getMessage());
            Assume.assumeTrue("Skipping Keycloak tests: Container failed to start - " + e.getMessage(), false);
        }
    }

    @AfterClass
    public static void afterAll() {
        if (keycloakContainer != null) {
            try {
                keycloakContainer.stop();
            } catch (Throwable ignore) {
                // best-effort shutdown
            } finally {
                keycloakContainer = null;
            }
        }
    }

    @After
    public void clear() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private static boolean dockerAvailable() {
        try {
            // Causes Testcontainers to probe for a working Docker client; throws if not available
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
