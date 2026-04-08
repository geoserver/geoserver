/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/** Tests for {@link GeoServerOAuth2LoginCustomizers}. */
public class GeoServerOAuth2LoginCustomizersTest {

    // ==================== HttpSecurityCustomizer tests ====================

    @Test
    public void testHttpSecurityCustomizerIsConsumer() {
        // Verify the interface extends Consumer
        assertTrue(
                "HttpSecurityCustomizer should extend Consumer",
                Consumer.class.isAssignableFrom(GeoServerOAuth2LoginCustomizers.HttpSecurityCustomizer.class));
    }

    @Test
    public void testHttpSecurityCustomizerCanBeImplementedAsLambda() {
        AtomicBoolean called = new AtomicBoolean(false);

        GeoServerOAuth2LoginCustomizers.HttpSecurityCustomizer customizer = http -> {
            called.set(true);
        };

        HttpSecurity mockHttp = mock(HttpSecurity.class);
        customizer.accept(mockHttp);

        assertTrue("Customizer should have been called", called.get());
    }

    @Test
    public void testHttpSecurityCustomizerReceivesHttpSecurity() {
        AtomicReference<HttpSecurity> received = new AtomicReference<>();

        GeoServerOAuth2LoginCustomizers.HttpSecurityCustomizer customizer = received::set;

        HttpSecurity mockHttp = mock(HttpSecurity.class);
        customizer.accept(mockHttp);

        assertNotNull("Should have received HttpSecurity", received.get());
        assertEquals("Should receive the same HttpSecurity instance", mockHttp, received.get());
    }

    @Test
    public void testHttpSecurityCustomizerAsMethodReference() {
        HttpSecurityCapture capture = new HttpSecurityCapture();

        GeoServerOAuth2LoginCustomizers.HttpSecurityCustomizer customizer = capture::capture;

        HttpSecurity mockHttp = mock(HttpSecurity.class);
        customizer.accept(mockHttp);

        assertTrue("Method reference should work", capture.wasCalled());
    }

    // ==================== ClientRegistrationCustomizer tests ====================

    @Test
    public void testClientRegistrationCustomizerIsConsumer() {
        assertTrue(
                "ClientRegistrationCustomizer should extend Consumer",
                Consumer.class.isAssignableFrom(GeoServerOAuth2LoginCustomizers.ClientRegistrationCustomizer.class));
    }

    @Test
    public void testClientRegistrationCustomizerCanBeImplementedAsLambda() {
        AtomicBoolean called = new AtomicBoolean(false);

        GeoServerOAuth2LoginCustomizers.ClientRegistrationCustomizer customizer = registration -> {
            called.set(true);
        };

        ClientRegistration registration = createTestClientRegistration();
        customizer.accept(registration);

        assertTrue("Customizer should have been called", called.get());
    }

    @Test
    public void testClientRegistrationCustomizerReceivesClientRegistration() {
        AtomicReference<ClientRegistration> received = new AtomicReference<>();

        GeoServerOAuth2LoginCustomizers.ClientRegistrationCustomizer customizer = received::set;

        ClientRegistration registration = createTestClientRegistration();
        customizer.accept(registration);

        assertNotNull("Should have received ClientRegistration", received.get());
        assertEquals("Should receive the same ClientRegistration instance", registration, received.get());
    }

    @Test
    public void testClientRegistrationCustomizerCanAccessRegistrationProperties() {
        AtomicReference<String> clientIdRef = new AtomicReference<>();

        GeoServerOAuth2LoginCustomizers.ClientRegistrationCustomizer customizer =
                registration -> clientIdRef.set(registration.getClientId());

        ClientRegistration registration = createTestClientRegistration();
        customizer.accept(registration);

        assertEquals("test-client-id", clientIdRef.get());
    }

    // ==================== FilterBuilderCustomizer tests ====================

    @Test
    public void testFilterBuilderCustomizerIsConsumer() {
        assertTrue(
                "FilterBuilderCustomizer should extend Consumer",
                Consumer.class.isAssignableFrom(GeoServerOAuth2LoginCustomizers.FilterBuilderCustomizer.class));
    }

    @Test
    public void testFilterBuilderCustomizerCanBeImplementedAsLambda() {
        AtomicBoolean called = new AtomicBoolean(false);

        GeoServerOAuth2LoginCustomizers.FilterBuilderCustomizer customizer = builder -> {
            called.set(true);
        };

        GeoServerOAuth2LoginAuthenticationFilterBuilder mockBuilder =
                mock(GeoServerOAuth2LoginAuthenticationFilterBuilder.class);
        customizer.accept(mockBuilder);

        assertTrue("Customizer should have been called", called.get());
    }

    @Test
    public void testFilterBuilderCustomizerReceivesFilterBuilder() {
        AtomicReference<GeoServerOAuth2LoginAuthenticationFilterBuilder> received = new AtomicReference<>();

        GeoServerOAuth2LoginCustomizers.FilterBuilderCustomizer customizer = received::set;

        GeoServerOAuth2LoginAuthenticationFilterBuilder mockBuilder =
                mock(GeoServerOAuth2LoginAuthenticationFilterBuilder.class);
        customizer.accept(mockBuilder);

        assertNotNull("Should have received FilterBuilder", received.get());
        assertEquals("Should receive the same FilterBuilder instance", mockBuilder, received.get());
    }

    // ==================== Chaining/Composition tests ====================

    @Test
    public void testHttpSecurityCustomizerAndThen() {
        CallOrderTracker callOrder = new CallOrderTracker();
        CallOrderTracker firstCallOrder = new CallOrderTracker();
        CallOrderTracker secondCallOrder = new CallOrderTracker();

        GeoServerOAuth2LoginCustomizers.HttpSecurityCustomizer first =
                http -> firstCallOrder.set(callOrder.incrementAndGet());

        GeoServerOAuth2LoginCustomizers.HttpSecurityCustomizer second =
                http -> secondCallOrder.set(callOrder.incrementAndGet());

        // Use andThen from Consumer interface
        Consumer<HttpSecurity> chained = first.andThen(second);

        HttpSecurity mockHttp = mock(HttpSecurity.class);
        chained.accept(mockHttp);

        assertEquals("First customizer should be called first", 1, firstCallOrder.get());
        assertEquals("Second customizer should be called second", 2, secondCallOrder.get());
    }

    @Test
    public void testClientRegistrationCustomizerAndThen() {
        CallOrderTracker callOrder = new CallOrderTracker();
        CallOrderTracker firstCallOrder = new CallOrderTracker();
        CallOrderTracker secondCallOrder = new CallOrderTracker();

        GeoServerOAuth2LoginCustomizers.ClientRegistrationCustomizer first =
                reg -> firstCallOrder.set(callOrder.incrementAndGet());

        GeoServerOAuth2LoginCustomizers.ClientRegistrationCustomizer second =
                reg -> secondCallOrder.set(callOrder.incrementAndGet());

        Consumer<ClientRegistration> chained = first.andThen(second);

        ClientRegistration registration = createTestClientRegistration();
        chained.accept(registration);

        assertEquals("First customizer should be called first", 1, firstCallOrder.get());
        assertEquals("Second customizer should be called second", 2, secondCallOrder.get());
    }

    @Test
    public void testFilterBuilderCustomizerAndThen() {
        CallOrderTracker callOrder = new CallOrderTracker();
        CallOrderTracker firstCallOrder = new CallOrderTracker();
        CallOrderTracker secondCallOrder = new CallOrderTracker();

        GeoServerOAuth2LoginCustomizers.FilterBuilderCustomizer first =
                builder -> firstCallOrder.set(callOrder.incrementAndGet());

        GeoServerOAuth2LoginCustomizers.FilterBuilderCustomizer second =
                builder -> secondCallOrder.set(callOrder.incrementAndGet());

        Consumer<GeoServerOAuth2LoginAuthenticationFilterBuilder> chained = first.andThen(second);

        GeoServerOAuth2LoginAuthenticationFilterBuilder mockBuilder =
                mock(GeoServerOAuth2LoginAuthenticationFilterBuilder.class);
        chained.accept(mockBuilder);

        assertEquals("First customizer should be called first", 1, firstCallOrder.get());
        assertEquals("Second customizer should be called second", 2, secondCallOrder.get());
    }

    // ==================== Helper classes and methods ====================

    private static class HttpSecurityCapture {
        private boolean called = false;

        public void capture(HttpSecurity http) {
            called = true;
        }

        public boolean wasCalled() {
            return called;
        }
    }

    /** Simple counter for tracking call order in tests. */
    private static class CallOrderTracker {
        private int value = 0;

        public int incrementAndGet() {
            return ++value;
        }

        public int get() {
            return value;
        }

        public void set(int value) {
            this.value = value;
        }
    }

    private ClientRegistration createTestClientRegistration() {
        return ClientRegistration.withRegistrationId("test-registration")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/oauth2/authorize")
                .tokenUri("https://example.com/oauth2/token")
                .build();
    }
}
