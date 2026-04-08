/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/** Tests for {@link AllowedAuthenticationProviderClasses}. */
public class AllowedAuthenticationProviderClassesTest {

    @After
    public void clearProperty() {
        System.clearProperty(AllowedAuthenticationProviderClasses.ALLOWED_LIST_PROP);
    }

    @Test
    public void defaultsIncludeWebServiceBodyResponseClasses() {
        AllowedAuthenticationProviderClasses allowList = AllowedAuthenticationProviderClasses.load();

        assertTrue(allowList.allows("org.geoserver.security.WebServiceBodyResponseSecurityProvider"));
        assertTrue(allowList.allows("org.geoserver.security.WebServiceBodyResponseSecurityProviderConfig"));
    }

    @Test
    public void prefixWildcardAllowsPackageClasses() {
        System.setProperty(AllowedAuthenticationProviderClasses.ALLOWED_LIST_PROP, "com.acme.security.*");
        AllowedAuthenticationProviderClasses allowList = AllowedAuthenticationProviderClasses.load();

        assertTrue(allowList.allows("com.acme.security.CustomProvider"));
        assertTrue(allowList.allows("com.acme.security.config.CustomProviderConfig"));
        assertFalse(allowList.allows("com.acme.other.CustomProvider"));
    }

    @Test
    public void nonTrailingWildcardIsHandledAsExactToken() {
        String token = "com.acme.*.security.CustomProvider";
        System.setProperty(AllowedAuthenticationProviderClasses.ALLOWED_LIST_PROP, token);
        AllowedAuthenticationProviderClasses allowList = AllowedAuthenticationProviderClasses.load();

        assertTrue(allowList.allows(token));
        assertFalse(allowList.allows("com.acme.dynamic.security.CustomProvider"));
    }
}
