/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_GOOGLE;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REGISTRATION_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.Builder;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;

/**
 * Tests for {@link GeoServerAuthorizationRequestCustomizer}.
 *
 * @author awaterme
 */
@SuppressWarnings("unchecked")
public class GeoServerAuthorizationRequestCustomizerTest {

    private Builder mockBuilder = Mockito.mock(Builder.class);

    private GeoServerOAuth2LoginFilterConfig config;
    private Map<String, Object> attributes = new HashMap<>();
    private Map<String, Object> additionalParams = new HashMap<>();

    private GeoServerAuthorizationRequestCustomizer sut;

    @Before
    public void setUpConfig() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://localhost/geoserver");
        config = new GeoServerOAuth2LoginFilterConfig();
        sut = new GeoServerAuthorizationRequestCustomizer(config);
    }

    @Before
    public void setUpMocks() {
        // when GeoServerAuthorizationRequestCustomizer calls attributes(pCustomizer)
        when(mockBuilder.attributes(any(Consumer.class)))
                .then(
                        // then call the customizer with our "attributes"
                        stub -> {
                            Consumer<Map<String, Object>> l = stub.getArgument(0, Consumer.class);
                            l.accept(attributes);
                            return mockBuilder;
                        });
        // when GeoServerAuthorizationRequestCustomizer calls additionalParameters(pCustomizer)
        when(mockBuilder.additionalParameters(any(Consumer.class)))
                .then(
                        // then call the customizer with our "additionalParameters"
                        stub -> {
                            Consumer<Map<String, Object>> l = stub.getArgument(0, Consumer.class);
                            l.accept(additionalParams);
                            return mockBuilder;
                        });
    }

    /** verifies no PKCE parameters are present when not enabled */
    @Test
    public void testNoPkceWithOidc() {
        // given: request is for OIDC provider
        attributes.put(REGISTRATION_ID, REG_ID_OIDC);

        // when: customizer is invoker, without PKCE enabled
        sut.accept(mockBuilder);

        // then: no PKCE parameters
        assertTrue(additionalParams.isEmpty());
    }

    /** verifies PKCE parameters are present when enabled for OIDC */
    @Test
    public void testPkceWithOidc() {
        // given: request is for OIDC provider
        attributes.put(REGISTRATION_ID, REG_ID_OIDC);

        // when: customizer is invoker, with PKCE enabled
        config.setOidcUsePKCE(true);
        sut.accept(mockBuilder);

        // then: PKCE parameters
        assertTrue(additionalParams.containsKey(PkceParameterNames.CODE_CHALLENGE));
        assertTrue(additionalParams.containsKey(PkceParameterNames.CODE_CHALLENGE_METHOD));
    }

    /** verifiesPKCE parameters are present for Google */
    @Test
    public void testPkceWithGoogle() {
        testPkceForRegistrationId(REG_ID_GOOGLE);
    }

    /** verifiesPKCE parameters are present for GitHub */
    @Test
    public void testPkceWithGitHub() {
        testPkceForRegistrationId(GeoServerOAuth2ClientRegistrationId.REG_ID_GIT_HUB);
    }

    /** verifiesPKCE parameters are present for Microsoft */
    @Test
    public void testPkceWithMs() {
        testPkceForRegistrationId(GeoServerOAuth2ClientRegistrationId.REG_ID_MICROSOFT);
    }

    private void testPkceForRegistrationId(String pRegistrationId) {
        // given: request is for OIDC provider
        attributes.put(REGISTRATION_ID, pRegistrationId);

        // when: customizer is invoker, with PKCE enabled
        config.setOidcUsePKCE(false);
        sut.accept(mockBuilder);

        // then: PKCE parameters
        assertTrue(additionalParams.containsKey(PkceParameterNames.CODE_CHALLENGE));
        assertTrue(additionalParams.containsKey(PkceParameterNames.CODE_CHALLENGE_METHOD));
    }

    /** verifies response_mode is passed as extra parameter if activated */
    @Test
    public void testResponseModeQueryOIDC() {
        // given: request is for OIDC provider
        attributes.put(REGISTRATION_ID, REG_ID_OIDC);

        // when: customizer is invoker, with responseMode=query
        config.setOidcResponseMode("query");
        sut.accept(mockBuilder);

        // then:
        assertEquals("query", additionalParams.get("response_mode"));
    }

    /** verifies no response_mode extra parameter if not activated */
    @Test
    public void testNoResponseModeOIDC() {
        // given: request is for OIDC provider
        attributes.put(REGISTRATION_ID, REG_ID_OIDC);

        // when: customizer is invoker, with responseMode=query
        config.setOidcResponseMode(null);
        sut.accept(mockBuilder);

        // then:
        assertNull(additionalParams.get("response_mode"));
    }

    /** verifies no response_mode extra parameter for Google */
    @Test
    public void testNoResponseModeGoogle() {
        // given: request is for OIDC provider
        attributes.put(REGISTRATION_ID, REG_ID_GOOGLE);

        // when: customizer is invoker, with responseMode=query
        config.setOidcResponseMode("query");
        sut.accept(mockBuilder);

        // then:
        assertNull(additionalParams.get("response_mode"));
    }
}
