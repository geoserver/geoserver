/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeycloakUrlBuilderTest {

    @Test
    public void testUserByName() {
        KeycloakUrlBuilder builder = new KeycloakUrlBuilder("testRealm", "http://keycloak/auth");
        String result = builder.userByName("test username").build();
        assertFalse(result.contains(" "));
        assertTrue(result.endsWith("username"));
    }
}
