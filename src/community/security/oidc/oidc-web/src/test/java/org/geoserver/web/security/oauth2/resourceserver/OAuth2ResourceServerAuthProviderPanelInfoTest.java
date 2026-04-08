/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.resourceserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.security.oauth2.resourceserver.GeoServerOAuth2ResourceServerAuthenticationFilter;
import org.geoserver.security.oauth2.resourceserver.GeoServerOAuth2ResourceServerFilterConfig;
import org.junit.Test;

/** Tests for {@link OAuth2ResourceServerAuthProviderPanelInfo}. */
public class OAuth2ResourceServerAuthProviderPanelInfoTest {

    @Test
    public void testConstructor() {
        OAuth2ResourceServerAuthProviderPanelInfo info = new OAuth2ResourceServerAuthProviderPanelInfo();

        assertNotNull(info);
    }

    @Test
    public void testComponentClass() {
        OAuth2ResourceServerAuthProviderPanelInfo info = new OAuth2ResourceServerAuthProviderPanelInfo();

        assertEquals(OAuth2ResourceServerAuthProviderPanel.class, info.getComponentClass());
    }

    @Test
    public void testServiceClass() {
        OAuth2ResourceServerAuthProviderPanelInfo info = new OAuth2ResourceServerAuthProviderPanelInfo();

        assertEquals(GeoServerOAuth2ResourceServerAuthenticationFilter.class, info.getServiceClass());
    }

    @Test
    public void testServiceConfigClass() {
        OAuth2ResourceServerAuthProviderPanelInfo info = new OAuth2ResourceServerAuthProviderPanelInfo();

        assertEquals(GeoServerOAuth2ResourceServerFilterConfig.class, info.getServiceConfigClass());
    }
}
