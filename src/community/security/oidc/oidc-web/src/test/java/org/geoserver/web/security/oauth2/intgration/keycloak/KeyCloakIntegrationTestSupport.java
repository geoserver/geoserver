/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.intgration.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.io.IOException;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.After;
import org.junit.BeforeClass;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.testcontainers.utility.MountableFile;

/**
 * Spins up a pre-configured Keycloak docker container.
 *
 * <p>administration is done through the main (master) realm. Login as geoserver/geoserver.
 *
 * <p>You can connect to the keycloak if you put a breakpoint in after keycloakContainer.start(). Use `docker ps`.
 *
 * <p>Please see the README.md in the `resources/org/geoserver/web/security/oauth2/intgration/keycloak` directory for
 * more information on how to change the keycloak configuration.
 */
public class KeyCloakIntegrationTestSupport extends GeoServerWicketTestSupport {

    /** base URL for the keycloak container (http://localhost:RANDOM_PORT). */
    static String authServerUrl;

    /** keycloak container setup. We bring in 2 realms - master-realm and gs-realm. */
    static KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:26.1")
            .withCopyToContainer(
                    MountableFile.forClasspathResource(
                            "org/geoserver/web/security/oauth2/login/keycloak/master-realm.json"),
                    "/opt/keycloak//data/import/master-realm.json")
            .withCopyToContainer(
                    MountableFile.forClasspathResource(
                            "org/geoserver/web/security/oauth2/login/keycloak/gs-realm-realm.json"),
                    "/opt/keycloak//data/import/gs-realm-realm.json")
            .withVerboseOutput()
            .withCustomCommand("--log-level=DEBUG"); // useful to see what's going on.  use `docker logs <container>`

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
        keycloakContainer.start();
        authServerUrl = keycloakContainer.getAuthServerUrl();
    }

    @After
    public void clear() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }
}
