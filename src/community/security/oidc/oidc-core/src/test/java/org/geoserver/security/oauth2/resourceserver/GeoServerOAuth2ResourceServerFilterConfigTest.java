/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.resourceserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests for {@link GeoServerOAuth2ResourceServerFilterConfig}. */
public class GeoServerOAuth2ResourceServerFilterConfigTest {

    @Test
    public void testDefaultConstructor() {
        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();

        assertNull(config.getIssuerUri());
        assertFalse(config.allowUnSecureLogging);
    }

    @Test
    public void testIssuerUri() {
        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();

        String issuerUri = "https://example.com/issuer";
        config.setIssuerUri(issuerUri);

        assertEquals(issuerUri, config.getIssuerUri());
    }

    @Test
    public void testIssuerUriWithNull() {
        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();

        config.setIssuerUri("https://example.com/issuer");
        config.setIssuerUri(null);

        assertNull(config.getIssuerUri());
    }

    @Test
    public void testAllowUnSecureLogging() {
        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();

        assertFalse(config.allowUnSecureLogging);

        config.allowUnSecureLogging = true;
        assertTrue(config.allowUnSecureLogging);
    }
}
