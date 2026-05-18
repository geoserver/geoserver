/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import static java.time.Instant.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.filter.GeoServerRoleResolvers.DefaultResolverContext;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.UserProfilePropertyNames;
import org.geoserver.security.oauth2.common.GeoServerOAuth2UserServices.GeoServerOAuth2UserService;
import org.geoserver.security.oauth2.common.GeoServerOAuth2UserServices.GeoServerOidcUserService;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** Tests for {@link GeoServerOAuth2UserServices} */
public class GeoServerOAuth2UserServicesTest {

    private static final String USER_NAME_ATTR = "principal";

    private HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    private GeoServerSecurityManager securityManager = mock(GeoServerSecurityManager.class);
    private GeoServerRoleConverter roleConverter = mock(GeoServerRoleConverter.class);
    private RoleSource roleSource = OpenIdRoleSource.AccessToken;
    private ClientRegistration clientRegistration;
    private TestOAuth2UserService mockOAuth2UserService = new TestOAuth2UserService();
    private TestOidcUserService mockOidcService = new TestOidcUserService();
    private GeoServerOAuth2RoleResolver mockRoleResolver = mock(GeoServerOAuth2RoleResolver.class);
    private GeoServerUserGroupService mockUserGroupService = mock(GeoServerUserGroupService.class);

    private Supplier<HttpServletRequest> requestSupplier = () -> mockRequest;
    private DefaultResolverContext resolverContext =
            new DefaultResolverContext(securityManager, "default", "default", null, roleConverter, roleSource);

    private GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

    @Before
    public void setUp() throws IOException {
        clientRegistration = ClientRegistration.withRegistrationId("oidc")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientId("client-id")
                .clientSecret("client-secret")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/oauth2/authorize")
                .tokenUri("https://example.com/oauth2/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName(USER_NAME_ATTR)
                .jwkSetUri("https://example.com/jwks")
                .issuerUri("https://example.com")
                .scope("openid", "profile", "email")
                .build();
        when(securityManager.loadUserGroupService("default")).thenReturn(mockUserGroupService);
    }

    /**
     * Verifies that the OAuth2 user service keeps the resolved Spring authorities and returns a GeoServer-backed
     * principal.
     */
    @Test
    public void testGeoServerOAuth2UserService() throws IOException {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OAuth2UserRequest lUserRequest = new OAuth2UserRequest(clientRegistration, lAccessToken);

        GeoServerOAuth2UserService sut = new GeoServerOAuth2UserService(resolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOAuth2UserService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        DefaultOAuth2User lDelgatesUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE1")),
                Map.of(USER_NAME_ATTR, "james", "attr1", "value1"),
                USER_NAME_ATTR);
        List<GeoServerRole> lResolversRoles = List.of(new GeoServerRole("R1"), new GeoServerRole("R2"));

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(lResolversRoles);
        mockOAuth2UserService.user = lDelgatesUser;
        OAuth2User lUser = sut.loadUser(lUserRequest);

        // then
        assertNotNull(lUser);
        assertTrue(lUser instanceof OAuth2User);
        assertThat(lUser.getAuthorities(), containsInAnyOrder(equalTo("R1"), equalTo("R2")));
    }

    /**
     * Verifies that OAuth2 claim values are copied into GeoServer user properties when no user/group store entry is
     * available.
     */
    @Test
    public void testGeoServerOAuth2UserServicePropertiesFromClaims() throws IOException {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OAuth2UserRequest lUserRequest = new OAuth2UserRequest(clientRegistration, lAccessToken);

        GeoServerOAuth2UserService sut = new GeoServerOAuth2UserService(resolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOAuth2UserService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        DefaultOAuth2User lDelgatesUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE1")),
                Map.of(
                        USER_NAME_ATTR,
                        "james",
                        "given_name",
                        "James",
                        "family_name",
                        "Bond",
                        "preferred_username",
                        "jbond",
                        "email",
                        "james@example.com"),
                USER_NAME_ATTR);
        List<GeoServerRole> lResolversRoles = List.of(new GeoServerRole("R1"), new GeoServerRole("R2"));

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(lResolversRoles);
        mockOAuth2UserService.user = lDelgatesUser;
        when(mockUserGroupService.getUserByUsername("james")).thenReturn(null);
        OAuth2User lUser = sut.loadUser(lUserRequest);

        // then
        assertNotNull(lUser);
        assertTrue(lUser instanceof GeoServerUser);
        assertTrue(lUser instanceof OAuth2User);
        assertEquals("james", lUser.getName());
        assertEquals("James", ((GeoServerUser) lUser).getProperties().getProperty(UserProfilePropertyNames.FIRST_NAME));
        assertEquals("Bond", ((GeoServerUser) lUser).getProperties().getProperty(UserProfilePropertyNames.LAST_NAME));
        assertEquals(
                "jbond",
                ((GeoServerUser) lUser).getProperties().getProperty(UserProfilePropertyNames.PREFERRED_USERNAME));
        assertEquals(
                "james@example.com",
                ((GeoServerUser) lUser).getProperties().getProperty(UserProfilePropertyNames.EMAIL));
    }

    /**
     * Verifies that the OIDC user service keeps the resolved Spring authorities and returns a GeoServer-backed
     * principal.
     */
    @Test
    public void testGeoServerOidcUserService() throws IOException {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OidcIdToken lIdToken = new OidcIdToken(
                "idTokenValue", now(), now().plusMillis(1), Map.of("sub", "james@b.co", USER_NAME_ATTR, "james"));
        OidcUserRequest lUserRequest = new OidcUserRequest(clientRegistration, lAccessToken, lIdToken);

        GeoServerOidcUserService sut = new GeoServerOidcUserService(resolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOidcService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        DefaultOidcUser lDelgatesUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE1")), lIdToken);
        List<GeoServerRole> lResolversRoles = List.of(new GeoServerRole("R1"), new GeoServerRole("R2"));

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(lResolversRoles);
        mockOidcService.user = lDelgatesUser;
        OidcUser lUser = sut.loadUser(lUserRequest);

        // then
        assertNotNull(lUser);
        assertTrue(lUser instanceof OidcUser);
        assertThat(lUser.getAuthorities(), containsInAnyOrder(equalTo("R1"), equalTo("R2")));
    }

    /**
     * Verifies that OIDC claims from the ID token and user-info endpoint are merged and exposed as GeoServer user
     * properties.
     */
    @Test
    public void testGeoServerOidcUserServicePropertiesFromMergedClaims() throws IOException {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OidcIdToken lIdToken = new OidcIdToken(
                "idTokenValue",
                now(),
                now().plusMillis(1),
                Map.of(
                        "sub",
                        "james@b.co",
                        USER_NAME_ATTR,
                        "james",
                        "given_name",
                        "Jim",
                        "preferred_username",
                        "jim-id-token"));
        OidcUserRequest lUserRequest = new OidcUserRequest(clientRegistration, lAccessToken, lIdToken);

        GeoServerOidcUserService sut = new GeoServerOidcUserService(resolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOidcService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        OidcUserInfo userInfo = new OidcUserInfo(Map.of(
                "family_name", "Bond",
                "preferred_username", "jbond",
                "email", "james@example.com"));
        DefaultOidcUser lDelgatesUser =
                new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE1")), lIdToken, userInfo, USER_NAME_ATTR);
        List<GeoServerRole> lResolversRoles = List.of(new GeoServerRole("R1"), new GeoServerRole("R2"));

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(lResolversRoles);
        mockOidcService.user = lDelgatesUser;
        when(mockUserGroupService.getUserByUsername("james")).thenReturn(null);
        OidcUser lUser = sut.loadUser(lUserRequest);

        // then
        assertNotNull(lUser);
        assertTrue(lUser instanceof GeoServerUser);
        assertTrue(lUser instanceof OidcUser);
        assertEquals("james", lUser.getName());
        assertEquals("Jim", ((GeoServerUser) lUser).getProperties().getProperty(UserProfilePropertyNames.FIRST_NAME));
        assertEquals("Bond", ((GeoServerUser) lUser).getProperties().getProperty(UserProfilePropertyNames.LAST_NAME));
        assertEquals(
                "jbond",
                ((GeoServerUser) lUser).getProperties().getProperty(UserProfilePropertyNames.PREFERRED_USERNAME));
        assertEquals(
                "james@example.com",
                ((GeoServerUser) lUser).getProperties().getProperty(UserProfilePropertyNames.EMAIL));
        assertEquals("jbond", lUser.getClaims().get("preferred_username"));
        assertSame(lIdToken, lUser.getIdToken());
        assertSame(userInfo, lUser.getUserInfo());
    }

    /**
     * Verifies that user/group service properties win over claims while missing fields are still filled from claim
     * values.
     */
    @Test
    public void testUserGroupServicePropertiesWinOverClaims() throws IOException {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OAuth2UserRequest lUserRequest = new OAuth2UserRequest(clientRegistration, lAccessToken);

        GeoServerOAuth2UserService sut = new GeoServerOAuth2UserService(resolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOAuth2UserService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        DefaultOAuth2User lDelgatesUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE1")),
                Map.of(
                        USER_NAME_ATTR,
                        "james",
                        "given_name",
                        "Claim",
                        "family_name",
                        "User",
                        "preferred_username",
                        "claim-user",
                        "email",
                        "claim@example.com"),
                USER_NAME_ATTR);
        GeoServerUser ugsUser = new GeoServerUser("james");
        Properties ugsProperties = ugsUser.getProperties();
        ugsProperties.setProperty(UserProfilePropertyNames.FIRST_NAME, "Stored");
        ugsProperties.setProperty(UserProfilePropertyNames.EMAIL, "stored@example.com");
        List<GeoServerRole> lResolversRoles = List.of(new GeoServerRole("R1"));

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(lResolversRoles);
        mockOAuth2UserService.user = lDelgatesUser;
        when(mockUserGroupService.getUserByUsername("james")).thenReturn(ugsUser);

        GeoServerUser lUser = (GeoServerUser) sut.loadUser(lUserRequest);

        // then
        assertEquals("Stored", lUser.getProperties().getProperty(UserProfilePropertyNames.FIRST_NAME));
        assertEquals("User", lUser.getProperties().getProperty(UserProfilePropertyNames.LAST_NAME));
        assertEquals("claim-user", lUser.getProperties().getProperty(UserProfilePropertyNames.PREFERRED_USERNAME));
        assertEquals("stored@example.com", lUser.getProperties().getProperty(UserProfilePropertyNames.EMAIL));
    }

    /** Verifies that a blank user/group service name disables the lookup and leaves claim-derived properties intact. */
    @Test
    public void testBlankUserGroupServiceFallsBackToClaimsOnly() {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OAuth2UserRequest lUserRequest = new OAuth2UserRequest(clientRegistration, lAccessToken);

        DefaultResolverContext blankResolverContext =
                new DefaultResolverContext(securityManager, "default", " ", null, roleConverter, roleSource);
        GeoServerOAuth2UserService sut = new GeoServerOAuth2UserService(blankResolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOAuth2UserService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        DefaultOAuth2User lDelgatesUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE1")),
                Map.of(USER_NAME_ATTR, "james", "email", "claims-only@example.com"),
                USER_NAME_ATTR);

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(List.of(new GeoServerRole("R1")));
        mockOAuth2UserService.user = lDelgatesUser;

        GeoServerUser lUser = (GeoServerUser) sut.loadUser(lUserRequest);

        // then
        assertEquals("claims-only@example.com", lUser.getProperties().getProperty(UserProfilePropertyNames.EMAIL));
    }

    /** Verifies that an unloaded user/group service is treated as unavailable and does not block login. */
    @Test
    public void testNullUserGroupServiceFallsBackToClaimsOnly() throws IOException {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OAuth2UserRequest lUserRequest = new OAuth2UserRequest(clientRegistration, lAccessToken);

        GeoServerOAuth2UserService sut = new GeoServerOAuth2UserService(resolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOAuth2UserService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        DefaultOAuth2User lDelgatesUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE1")),
                Map.of(USER_NAME_ATTR, "james", "email", "claims-only@example.com"),
                USER_NAME_ATTR);

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(List.of(new GeoServerRole("R1")));
        mockOAuth2UserService.user = lDelgatesUser;
        when(mockUserGroupService.getUserByUsername("james")).thenReturn(null);
        when(securityManager.loadUserGroupService("default")).thenReturn(null);

        GeoServerUser lUser = (GeoServerUser) sut.loadUser(lUserRequest);

        // then
        assertEquals("claims-only@example.com", lUser.getProperties().getProperty(UserProfilePropertyNames.EMAIL));
    }

    /**
     * Verifies that a user/group service lookup failure does not block login and claim-derived properties are still
     * applied.
     */
    @Test
    public void testIOExceptionInUserGroupServiceDoesNotFailLogin() throws IOException {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OAuth2UserRequest lUserRequest = new OAuth2UserRequest(clientRegistration, lAccessToken);

        GeoServerOAuth2UserService sut = new GeoServerOAuth2UserService(resolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOAuth2UserService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        DefaultOAuth2User lDelgatesUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE1")),
                Map.of(USER_NAME_ATTR, "james", "email", "fallback@example.com"),
                USER_NAME_ATTR);

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(List.of(new GeoServerRole("R1")));
        mockOAuth2UserService.user = lDelgatesUser;
        when(mockUserGroupService.getUserByUsername("james")).thenThrow(new IOException("boom"));

        GeoServerUser lUser = (GeoServerUser) sut.loadUser(lUserRequest);

        // then
        assertEquals("fallback@example.com", lUser.getProperties().getProperty(UserProfilePropertyNames.EMAIL));
    }

    /** Verifies that unexpected runtime failures in user/group service lookup do not block login. */
    @Test
    public void testRuntimeExceptionInUserGroupServiceDoesNotFailLogin() throws IOException {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OAuth2UserRequest lUserRequest = new OAuth2UserRequest(clientRegistration, lAccessToken);

        GeoServerOAuth2UserService sut = new GeoServerOAuth2UserService(resolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOAuth2UserService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        DefaultOAuth2User lDelgatesUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE1")),
                Map.of(USER_NAME_ATTR, "james", "email", "fallback@example.com"),
                USER_NAME_ATTR);

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(List.of(new GeoServerRole("R1")));
        mockOAuth2UserService.user = lDelgatesUser;
        when(mockUserGroupService.getUserByUsername("james")).thenThrow(new RuntimeException("boom"));

        GeoServerUser lUser = (GeoServerUser) sut.loadUser(lUserRequest);

        // then
        assertEquals("fallback@example.com", lUser.getProperties().getProperty(UserProfilePropertyNames.EMAIL));
    }

    /** Verifies that blank claim values do not create empty GeoServer user properties. */
    @Test
    public void testBlankClaimValuesDoNotPopulateProperties() throws IOException {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OAuth2UserRequest lUserRequest = new OAuth2UserRequest(clientRegistration, lAccessToken);

        GeoServerOAuth2UserService sut = new GeoServerOAuth2UserService(resolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOAuth2UserService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        DefaultOAuth2User lDelgatesUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE1")),
                Map.of(USER_NAME_ATTR, "james", "email", " "),
                USER_NAME_ATTR);

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(List.of(new GeoServerRole("R1")));
        mockOAuth2UserService.user = lDelgatesUser;
        when(mockUserGroupService.getUserByUsername("james")).thenReturn(null);

        GeoServerUser lUser = (GeoServerUser) sut.loadUser(lUserRequest);

        // then
        assertFalse(lUser.getProperties().containsKey(UserProfilePropertyNames.EMAIL));
    }

    private static class TestOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

        private OAuth2User user;

        /** Returns the preconfigured OAuth2 principal used by the test case. */
        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) {
            return user;
        }
    }

    private static class TestOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

        private OidcUser user;

        /** Returns the preconfigured OIDC principal used by the test case. */
        @Override
        public OidcUser loadUser(OidcUserRequest userRequest) {
            return user;
        }
    }
}
