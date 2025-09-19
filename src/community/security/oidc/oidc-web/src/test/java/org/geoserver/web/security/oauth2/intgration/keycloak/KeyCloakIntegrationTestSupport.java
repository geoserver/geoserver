package org.geoserver.web.security.oauth2.intgration.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.io.IOException;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.After;
import org.junit.BeforeClass;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.testcontainers.utility.MountableFile;

public class KeyCloakIntegrationTestSupport extends GeoServerWicketTestSupport {

    static String authServerUrl;
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
            .withCustomCommand("--log-level=DEBUG");

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
