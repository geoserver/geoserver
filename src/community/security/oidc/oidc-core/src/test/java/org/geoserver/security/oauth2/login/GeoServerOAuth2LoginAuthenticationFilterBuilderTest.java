/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.oauth2.login;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.reflect.FieldUtils.readField;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_GIT_HUB;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_GOOGLE;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST;

import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.oauth2.common.ConfidentialLogger;
import org.geoserver.security.oauth2.spring.GeoServerAuthorizationRequestCustomizer;
import org.geoserver.security.oauth2.spring.GeoServerOidcConfigurableTokenValidator;
import org.geoserver.security.oauth2.spring.GeoServerOidcIdTokenDecoderFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer.AuthorizationEndpointConfig;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer.TokenEndpointConfig;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer.UserInfoEndpointConfig;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Tests {@link GeoServerOAuth2LoginAuthenticationFilterBuilder}
 *
 * @author awaterme
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class GeoServerOAuth2LoginAuthenticationFilterBuilderTest {

    private GeoServerOAuth2LoginFilterConfig configuration;

    private GeoServerSecurityManager mockSecurityManager;
    private HttpSecurity mockHttp;
    private ApplicationEventPublisher mockEventPublisher;
    private GeoServerOidcIdTokenDecoderFactory mockTokenDecoderFactory;
    private OAuth2LoginConfigurer mockOAuth2LoginConfigurer;

    private final UserInfoEndpointConfig mockUserInfoConfig = mock(UserInfoEndpointConfig.class);
    private final AuthorizationEndpointConfig mockAuthorizationConfig = mock(AuthorizationEndpointConfig.class);
    private final TokenEndpointConfig mockTokenConfig = mock(TokenEndpointConfig.class);

    private final GeoServerOAuth2LoginAuthenticationFilterBuilder sut =
            new GeoServerOAuth2LoginAuthenticationFilterBuilder();

    @Before
    public void setupDependencies() throws Exception {

        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://localhost/geoserver");

        configuration = new GeoServerOAuth2LoginFilterConfig();
        mockSecurityManager = mock(GeoServerSecurityManager.class);
        mockHttp = mock(HttpSecurity.class);
        mockEventPublisher = mock(ApplicationEventPublisher.class);
        mockTokenDecoderFactory = mock(GeoServerOidcIdTokenDecoderFactory.class);
        mockOAuth2LoginConfigurer = mock(OAuth2LoginConfigurer.class);

        // Support chaining on the top-level configurer methods that the builder uses
        when(mockOAuth2LoginConfigurer.clientRegistrationRepository(any())).thenReturn(mockOAuth2LoginConfigurer);
        when(mockOAuth2LoginConfigurer.authorizedClientRepository(any())).thenReturn(mockOAuth2LoginConfigurer);
        when(mockOAuth2LoginConfigurer.authorizedClientService(any())).thenReturn(mockOAuth2LoginConfigurer);
        when(mockOAuth2LoginConfigurer.loginProcessingUrl(any())).thenReturn(mockOAuth2LoginConfigurer);

        // For the lambda-based (non-deprecated) endpoint customizers, apply the customizer to our mocks
        when(mockOAuth2LoginConfigurer.userInfoEndpoint(any(Customizer.class)))
                .thenAnswer((Answer<OAuth2LoginConfigurer>) inv -> {
                    Customizer<UserInfoEndpointConfig> c = inv.getArgument(0);
                    c.customize(mockUserInfoConfig);
                    return mockOAuth2LoginConfigurer;
                });

        when(mockOAuth2LoginConfigurer.authorizationEndpoint(any(Customizer.class)))
                .thenAnswer((Answer<OAuth2LoginConfigurer>) inv -> {
                    Customizer<AuthorizationEndpointConfig> c = inv.getArgument(0);
                    c.customize(mockAuthorizationConfig);
                    return mockOAuth2LoginConfigurer;
                });

        when(mockOAuth2LoginConfigurer.tokenEndpoint(any(Customizer.class)))
                .thenAnswer((Answer<OAuth2LoginConfigurer>) inv -> {
                    Customizer<TokenEndpointConfig> c = inv.getArgument(0);
                    c.customize(mockTokenConfig);
                    return mockOAuth2LoginConfigurer;
                });

        // When oauth2Login(customizer) is invoked, we run the customizer against our mock configurer
        when(mockHttp.oauth2Login(any())).thenAnswer(stub -> {
            Customizer<OAuth2LoginConfigurer<HttpSecurity>> callback = stub.getArgument(0, Customizer.class);
            callback.customize(mockOAuth2LoginConfigurer);
            return mockHttp;
        });
    }

    private void assignDependencies() {
        sut.setConfiguration(configuration);
        sut.setSecurityManager(mockSecurityManager);
        sut.setHttp(mockHttp);
        sut.setEventPublisher(mockEventPublisher);
        sut.setTokenDecoderFactory(mockTokenDecoderFactory);
    }

    /** when called with incomplete required dependencies: fail */
    @Test(expected = IllegalArgumentException.class)
    public void testCheckMissingDeps() {
        sut.build();
    }

    /** when called with dependencies, without active provider: no exceptions */
    @Test
    public void testNoProviderActive() throws Exception {
        // given:
        assignDependencies();

        // when: called without provider active
        GeoServerOAuth2LoginAuthenticationFilter lFilter = sut.build();

        // then: no exception, no filters
        assertNotNull(lFilter);
        assertEquals(0, lFilter.getNestedFilters().size());
        assertNull(lFilter.getLogoutSuccessHandler());

        // further build is not permitted
        try {
            sut.build();
            fail("Exception IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * Verifies that the expected calls on the spring configuration API have occurred and filter chain is reconstructed
     * as expected.
     *
     * @throws Exception
     */
    @Test
    public void testFilterConstructionWithGoogle() throws Exception {
        // given
        assignDependencies();

        ClientRegistrationRepository lRepo = mock(ClientRegistrationRepository.class);
        OAuth2AuthorizedClientService lService = mock(OAuth2AuthorizedClientService.class);
        Filter f0 = mock(Filter.class);
        Filter f1 = new OAuth2AuthorizationRequestRedirectFilter(lRepo);
        Filter f2 = new OAuth2LoginAuthenticationFilter(lRepo, lService);
        Filter f3 = new RequestCacheAwareFilter();
        Filter f4 = mock(Filter.class);
        List<Filter> lFilters = Arrays.asList(f0, f1, f2, f3, f4);

        // * http returns a "complete" spring chain, here with some mock filters
        when(mockHttp.build()).thenReturn(new DefaultSecurityFilterChain(mock(RequestMatcher.class), lFilters));

        // * Google is active and setup
        configuration.setGoogleEnabled(true);
        configuration.setGoogleClientId("myClientId");
        configuration.setGoogleClientSecret("myClientSecret");

        // * skip GS login is enabled
        configuration.setEnableRedirectAuthenticationEntryPoint(true);

        // * unsecure logging is active
        configuration.setOidcAllowUnSecureLogging(true);
        ConfidentialLogger.setEnabled(false);

        // when: building filter
        GeoServerOAuth2LoginAuthenticationFilter lFilter = sut.build();

        // then
        // * logout handler must be in place
        assertNotNull(lFilter.getLogoutSuccessHandler());

        // * relevant filters are extracted
        assertEquals(4, lFilter.getNestedFilters().size());
        assertNotNull(sut.getRedirectToProviderFilter());
        Assert.assertSame(f1, lFilter.getNestedFilters().get(0));
        Assert.assertSame(f2, lFilter.getNestedFilters().get(1));
        Assert.assertSame(f3, lFilter.getNestedFilters().get(2));
        Assert.assertSame(
                sut.getRedirectToProviderFilter(), lFilter.getNestedFilters().get(3));

        // * configuration API has been called, and entire build process is through without nulls
        verify(mockTokenDecoderFactory, times(1)).setGeoServerOAuth2LoginFilterConfig(isNotNull());
        verify(mockHttp, times(1)).build();
        verify(mockOAuth2LoginConfigurer, times(1)).clientRegistrationRepository(isNotNull());
        verify(mockOAuth2LoginConfigurer, times(1)).authorizedClientRepository(isNotNull());
        verify(mockOAuth2LoginConfigurer, times(1)).authorizedClientService(isNotNull());
        verify(mockUserInfoConfig, times(1)).userService(isNotNull());
        verify(mockUserInfoConfig, times(1)).oidcUserService(isNotNull());
        verify(mockAuthorizationConfig, times(1)).authorizationRequestResolver(isNotNull());
        verify(mockTokenConfig, times(1)).accessTokenResponseClient(isNotNull());

        // * events have been published
        verify(mockEventPublisher, times(4)).publishEvent(any(OAuth2LoginButtonEnablementEvent.class));

        // * google client is setup as expected
        ClientRegistrationRepository lClientRepo = sut.getClientRegistrationRepository();
        assertNotNull(lClientRepo);
        ClientRegistration lGoogleReg = lClientRepo.findByRegistrationId(REG_ID_GOOGLE);
        assertNotNull(lGoogleReg);
        assertEquals("myClientId", lGoogleReg.getClientId());
        assertEquals("myClientSecret", lGoogleReg.getClientSecret());

        assertTrue(ConfidentialLogger.isEnabled());
    }

    /**
     * Tests OIDC client construction and verifies configured settings and GeoServer customizers are in place.
     *
     * @throws Exception
     */
    @Test
    public void testOidcConstruction() throws Exception {
        // given
        assignDependencies();

        // * OIDC is used, with the respective settings
        configuration.setOidcEnabled(true);

        configuration.setOidcClientId("myId");
        configuration.setOidcClientSecret("mySecret");
        configuration.setOidcUserNameAttribute("myAttr");
        configuration.setOidcRedirectUri("myRedirectUri");
        configuration.setOidcScopes("myScopes");

        configuration.setOidcDiscoveryUri("myDiscoveryUrik");
        configuration.setOidcTokenUri("myTokenUri");
        configuration.setOidcAuthorizationUri("myAuthorizationUri");
        configuration.setOidcUserInfoUri("myUserInfoUri");
        configuration.setOidcJwkSetUri("https://myJwkSetUri");
        configuration.setOidcLogoutUri("myLogoutUri");

        configuration.setOidcEnforceTokenValidation(false);
        configuration.setOidcUsePKCE(true);
        configuration.setOidcResponseMode("query");
        configuration.setOidcAuthenticationMethodPostSecret(true);
        configuration.setOidcAllowUnSecureLogging(false);

        // * filter construction is tested in testFilterConstructionWithGoogle()
        when(mockHttp.build())
                .thenReturn(new DefaultSecurityFilterChain(mock(RequestMatcher.class), new ArrayList<>()));

        // * builder uses real factory
        sut.setTokenDecoderFactory(new GeoServerOidcIdTokenDecoderFactory());

        // * confidential logger is enabled before
        ConfidentialLogger.setEnabled(true);

        // when
        // * filter is constructed
        GeoServerOAuth2LoginAuthenticationFilter lFilter = sut.build();

        // then
        // * filter was created
        assertNotNull(lFilter);

        // * settings where transmitted
        ClientRegistrationRepository lClientRepo = sut.getClientRegistrationRepository();
        assertNotNull(lClientRepo);

        ClientRegistration lReg = lClientRepo.findByRegistrationId(REG_ID_OIDC);
        assertNotNull(lReg);

        assertEquals("myId", lReg.getClientId());
        assertEquals("mySecret", lReg.getClientSecret());
        assertEquals("myAttr", lReg.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());
        assertEquals("myRedirectUri", lReg.getRedirectUri());
        assertEquals(singleton("myScopes"), lReg.getScopes());
        assertEquals("myTokenUri", lReg.getProviderDetails().getTokenUri());
        assertEquals("myAuthorizationUri", lReg.getProviderDetails().getAuthorizationUri());
        assertEquals(
                "myUserInfoUri", lReg.getProviderDetails().getUserInfoEndpoint().getUri());
        assertEquals("https://myJwkSetUri", lReg.getProviderDetails().getJwkSetUri());
        assertEquals(
                "myLogoutUri",
                lReg.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint"));

        GeoServerOidcIdTokenDecoderFactory lTokenDecoderFactory = sut.getTokenDecoderFactory();
        assertNotNull(lTokenDecoderFactory);

        JwtDecoder lDecoder = lTokenDecoderFactory.createDecoder(lReg);
        Object lValidatorObject = readField(lDecoder, "jwtValidator", true);
        assertNotNull(lValidatorObject);
        assertEquals(GeoServerOidcConfigurableTokenValidator.class, lValidatorObject.getClass());
        GeoServerOidcConfigurableTokenValidator lValidator = (GeoServerOidcConfigurableTokenValidator) lValidatorObject;

        // * enforceTokenValidation is addressed by validator
        Assert.assertSame(configuration, lValidator.getConfiguration());

        // * PKCE, extra request parameters (response mode)
        DefaultOAuth2AuthorizationRequestResolver lResolver = sut.getAuthorizationRequestResolver();
        assertNotNull(lResolver);
        Object lCustomizerObject = readField(lResolver, "authorizationRequestCustomizer", true);
        assertNotNull(lCustomizerObject);
        assertEquals(GeoServerAuthorizationRequestCustomizer.class, lCustomizerObject.getClass());

        // * authentication method post secret
        assertEquals(CLIENT_SECRET_POST, lReg.getClientAuthenticationMethod());

        // * insecure logging
        assertFalse(ConfidentialLogger.isEnabled());
    }

    /**
     * Verifies that the expected calls on the spring configuration API have occurred and filter chain is reconstructed
     * as expected.
     *
     * @throws Exception
     */
    @Test
    public void testFilterConstructionWithFurtherProviders() throws Exception {
        // given
        assignDependencies();

        ClientRegistrationRepository lRepo = mock(ClientRegistrationRepository.class);
        OAuth2AuthorizedClientService lService = mock(OAuth2AuthorizedClientService.class);
        Filter f0 = mock(Filter.class);
        Filter f1 = new OAuth2AuthorizationRequestRedirectFilter(lRepo);
        Filter f2 = new OAuth2LoginAuthenticationFilter(lRepo, lService);
        Filter f3 = new RequestCacheAwareFilter();
        Filter f4 = mock(Filter.class);
        List<Filter> lFilters = Arrays.asList(f0, f1, f2, f3, f4);

        // * http returns a "complete" spring chain, here with some mock filters
        when(mockHttp.build()).thenReturn(new DefaultSecurityFilterChain(mock(RequestMatcher.class), lFilters));

        // * GitHub is active and setup
        configuration.setGitHubEnabled(true);
        configuration.setGitHubClientId("ghClientId");
        configuration.setGitHubClientSecret("ghClientSecret");

        // * Microsoft is active and setup
        configuration.setMsEnabled(true);
        configuration.setMsClientId("msClientId");
        configuration.setMsClientSecret("msClientSecret");

        // when: building filter
        sut.build();

        // * clients are setup as expected
        ClientRegistrationRepository lClientRepo = sut.getClientRegistrationRepository();
        assertNotNull(lClientRepo);
        ClientRegistration lClientReg = lClientRepo.findByRegistrationId(REG_ID_GIT_HUB);
        assertNotNull(lClientReg);
        assertEquals("ghClientId", lClientReg.getClientId());
        assertEquals("ghClientSecret", lClientReg.getClientSecret());

        lClientReg = lClientRepo.findByRegistrationId(REG_ID_MICROSOFT);
        assertNotNull(lClientReg);
        assertEquals("msClientId", lClientReg.getClientId());
        assertEquals("msClientSecret", lClientReg.getClientSecret());
    }
}
