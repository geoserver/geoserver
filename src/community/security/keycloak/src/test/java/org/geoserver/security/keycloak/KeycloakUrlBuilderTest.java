/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class KeycloakUrlBuilderTest extends TestCase {

    @Test
    public void testUserByName() {
        KeycloakUrlBuilder builder = new KeycloakUrlBuilder("testRealm", "http://keycloak/auth");
        String result = builder.userByName("test username").build();
        Assert.assertFalse(result.contains(" "));
        Assert.assertTrue(result.endsWith("username"));
    }
}
