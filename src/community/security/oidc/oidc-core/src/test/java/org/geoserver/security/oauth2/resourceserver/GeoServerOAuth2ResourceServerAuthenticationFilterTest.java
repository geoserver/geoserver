/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.resourceserver;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Tests for {@link GeoServerOAuth2ResourceServerAuthenticationFilter}. */
public class GeoServerOAuth2ResourceServerAuthenticationFilterTest {

    @Test
    public void testConstructor() {
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                new GeoServerOAuth2ResourceServerAuthenticationFilter();

        assertNotNull(filter);
    }

    @Test
    public void testApplicableForHtml() {
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                new GeoServerOAuth2ResourceServerAuthenticationFilter();

        assertTrue(filter.applicableForHtml());
    }

    @Test
    public void testApplicableForServices() {
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                new GeoServerOAuth2ResourceServerAuthenticationFilter();

        assertTrue(filter.applicableForServices());
    }

    @Test
    public void testDoFilter() throws Exception {
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                new GeoServerOAuth2ResourceServerAuthenticationFilter();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Should not throw - just delegates to super
        filter.doFilter(request, response, chain);

        // Verify the chain was called (filter passed through)
        assertNotNull(chain.getRequest());
    }

    @Test
    public void testInitializeFromConfig() throws IOException {
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                new GeoServerOAuth2ResourceServerAuthenticationFilter();

        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();
        config.setName("testFilter");
        config.setClassName(GeoServerOAuth2ResourceServerAuthenticationFilter.class.getName());

        // Should not throw - just logs and delegates to super
        filter.initializeFromConfig(config);
    }
}
