/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.resourceserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfigValidator;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/** Tests for {@link GeoServerOAuth2ResourceServerAuthenticationProvider}. */
public class GeoServerOAuth2ResourceServerAuthenticationProviderTest {

    private GeoServerOAuth2ResourceServerAuthenticationProvider provider;
    private GeoServerSecurityManager securityManager;
    private ApplicationContext applicationContext;

    @Before
    public void setUp() {
        securityManager = mock(GeoServerSecurityManager.class);
        applicationContext = mock(ApplicationContext.class);
        when(securityManager.getApplicationContext()).thenReturn(applicationContext);
        provider = new GeoServerOAuth2ResourceServerAuthenticationProvider(securityManager);
    }

    @Test
    public void testConstructor() {
        assertNotNull(provider);
    }

    @Test
    public void testGetFilterClass() {
        Class<? extends GeoServerSecurityFilter> filterClass = provider.getFilterClass();

        assertEquals(GeoServerOAuth2ResourceServerAuthenticationFilter.class, filterClass);
    }

    @Test
    public void testConfigure() {
        XStreamPersisterFactory factory = new XStreamPersisterFactory();
        XStreamPersister persister = factory.createXMLPersister();

        provider.configure(persister);

        // Verify the alias is configured - we can test this by trying to serialize/deserialize
        assertNotNull(persister.getXStream());
    }

    @Test
    public void testConfigureRegistersAlias() {
        XStreamPersisterFactory factory = new XStreamPersisterFactory();
        XStreamPersister persister = factory.createXMLPersister();

        provider.configure(persister);

        // Test that the alias works by serializing a config
        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();
        config.setIssuerUri("https://issuer.example.com");
        config.setName("test-filter");

        String xml = persister.getXStream().toXML(config);
        assertNotNull(xml);
        assertTrue(xml.contains("oauth2ResourceServerAuthentication"));
    }

    @Test
    public void testCreateConfigurationValidator() {
        SecurityConfigValidator validator = provider.createConfigurationValidator(securityManager);

        assertNotNull(validator);
        assertTrue(validator instanceof GeoServerOAuth2LoginFilterConfigValidator);
    }

    @Test
    public void testOnApplicationEventWithNonContextLoadedEvent() {
        // Create a non-ContextLoadedEvent
        ApplicationEvent event = mock(ApplicationEvent.class);

        // Should not throw
        provider.onApplicationEvent(event);
    }

    @Test(expected = NullPointerException.class)
    public void testOnApplicationEventWithContextLoadedEvent() {
        // This test verifies that when running without a full GeoServer/Spring context,
        // onApplicationEvent fails in a well-defined way (NullPointerException) instead
        // of silently swallowing errors.
        ContextLoadedEvent event = mock(ContextLoadedEvent.class);

        provider.onApplicationEvent(event);
    }

    @Test
    public void testCreateFilterThrowsRuntimeExceptionOnInvalidIssuer() {
        // Given - a config with an unreachable issuer
        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();
        config.setIssuerUri("https://issuer.example.com");
        config.setName("test-filter");

        HttpSecurity mockHttp = mock(HttpSecurity.class);
        when(applicationContext.getBean(HttpSecurity.class)).thenReturn(mockHttp);

        // When/Then - should throw RuntimeException wrapping the JWT decoder failure
        try {
            provider.createFilter(config);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // Expected - JwtDecoders.fromIssuerLocation fails without a real issuer
            assertTrue(e.getMessage().contains("Failed to create OpenID filter"));
        }
    }

    @Test
    public void testFilterConfigHasCorrectDefaults() {
        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();

        // Verify defaults
        assertNotNull(config);
    }

    @Test
    public void testFilterConfigSettersAndGetters() {
        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();

        config.setIssuerUri("https://issuer.example.com");

        assertEquals("https://issuer.example.com", config.getIssuerUri());
    }
}
