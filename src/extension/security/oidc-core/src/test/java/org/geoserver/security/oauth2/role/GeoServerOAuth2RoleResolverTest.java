/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/** */
package org.geoserver.security.oauth2.role;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.filter.GeoServerRoleResolvers.DefaultResolverContext;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.config.OpenIdRoleSource;
import org.geoserver.security.oauth2.role.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geoserver.security.oauth2.role.provider.msgraph.MSGraphRolesResolver;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** Tests {@link GeoServerOAuth2RoleResolver}. */
@SuppressWarnings("deprecation") // exercises the deprecated supplier-setter façade kept for back-compat
public class GeoServerOAuth2RoleResolverTest {

    private static final String ROLES_CLAIM_NAME = "roles";

    private static final String ROLE_NAME_AUTHENTICATED = "ROLE_AUTHENTICATED";

    private static final String PRINCIPAL_NAME = "james";

    private GeoServerSecurityManager mockSecurityManager = mock(GeoServerSecurityManager.class);
    private GeoServerRoleConverter mockRoleConverter = mock(GeoServerRoleConverter.class);
    private HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    private ClientRegistration mockClientReg = mock(ClientRegistration.class);
    private GeoServerRoleService mockRoleService = mock(GeoServerRoleService.class);

    private GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
    private DefaultResolverContext context = newResolverContext(OpenIdRoleSource.AccessToken);

    private OAuth2AccessToken accessToken =
            new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
    private OAuth2UserRequest userRequest = new OAuth2UserRequest(mockClientReg, accessToken);

    private GeoServerOAuth2RoleResolver sut = new GeoServerOAuth2RoleResolver(config);

    @Before
    public void setUp() {
        when(mockSecurityManager.getActiveRoleService()).thenReturn(mockRoleService);

        config.setTokenRolesClaim(ROLES_CLAIM_NAME);
    }

    /** Verifies that parameter is checked for expected type */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidParameter() throws Exception {
        ResolverParam lParam = new ResolverParam(PRINCIPAL_NAME, mockRequest, context);
        sut.convert(lParam);
    }

    /**
     * Verifies that a user named "root" at the identity provider never receives roles, whatever the administrator
     * setting is: "root" is backed by the master password and can have no external identity.
     */
    @Test
    public void testGetRolesIsEmptyForRootWhateverTheAdminSetting() {
        for (Boolean lAllowAdmin : new Boolean[] {null, Boolean.TRUE, Boolean.FALSE}) {
            // given
            config.setAllowAdminLogin(lAllowAdmin);
            OAuth2ResolverParam lParam = new OAuth2ResolverParam("root", mockRequest, context, userRequest);

            // when
            Collection<GeoServerRole> lRoles = sut.convert(lParam);

            // then
            assertTrue("Expecting no roles for root, allowAdminLogin=" + lAllowAdmin, lRoles.isEmpty());
        }
    }

    /**
     * Verifies that a user named "admin" at the identity provider is refused roles once the administrator account is
     * declared local only.
     */
    @Test
    public void testGetRolesIsEmptyForAdminWhenAdminLoginDisallowed() {
        // given
        config.setAllowAdminLogin(Boolean.FALSE);
        OAuth2ResolverParam lParam = new OAuth2ResolverParam("admin", mockRequest, context, userRequest);

        // when
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertTrue("Expecting no roles for admin", lRoles.isEmpty());
    }

    /**
     * Verifies that by default a user named "admin" at the identity provider is treated like any other user. This is
     * the case of a platform provisioning its own administrator account and expecting GeoServer to honour it.
     */
    @Test
    public void testGetRolesForAdminByDefault() {
        // given
        OAuth2ResolverParam lParam = new OAuth2ResolverParam("admin", mockRequest, context, userRequest);

        // when
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED)));
    }

    /**
     * Pins the consequence of allowing the administrator account through when roles are resolved by looking the
     * principal name up locally, rather than from the token: the identity provider's "admin" inherits the roles of the
     * local "admin", without having asserted any role itself. That is the reason the option exists, and the reason the
     * documentation tells operators using a name-based role source to turn it off.
     */
    @Test
    public void testAdminInheritsLocalRolesFromRoleServiceWhenAllowed() throws Exception {
        // given: roles come from the local role service, which grants ROLE_ADMINISTRATOR to "admin"
        SortedSet<GeoServerRole> localRoles = new TreeSet<>();
        localRoles.add(new GeoServerRole("ROLE_ADMINISTRATOR"));
        when(mockSecurityManager.loadRoleService("default")).thenReturn(mockRoleService);
        when(mockRoleService.getRolesForUser("admin")).thenReturn(localRoles);
        context = newResolverContext(PreAuthenticatedUserNameRoleSource.RoleService);

        OAuth2ResolverParam lParam = new OAuth2ResolverParam("admin", mockRequest, context, userRequest);

        // when
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then: the local administrator's role arrives, on top of the usual authenticated marker
        assertThat(lRoles, containsInAnyOrder(equalTo("ROLE_ADMINISTRATOR"), equalTo(ROLE_NAME_AUTHENTICATED)));
    }

    /** ... and that turning the option off closes exactly that path. */
    @Test
    public void testAdminInheritsNothingFromRoleServiceWhenDisallowed() throws Exception {
        SortedSet<GeoServerRole> localRoles = new TreeSet<>();
        localRoles.add(new GeoServerRole("ROLE_ADMINISTRATOR"));
        when(mockSecurityManager.loadRoleService("default")).thenReturn(mockRoleService);
        when(mockRoleService.getRolesForUser("admin")).thenReturn(localRoles);
        context = newResolverContext(PreAuthenticatedUserNameRoleSource.RoleService);
        config.setAllowAdminLogin(Boolean.FALSE);

        OAuth2ResolverParam lParam = new OAuth2ResolverParam("admin", mockRequest, context, userRequest);

        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        assertTrue("Expecting no roles for admin", lRoles.isEmpty());
    }

    /** Verifies that disallowing the administrator account leaves every other user alone. */
    @Test
    public void testGetRolesForRegularUserWhenAdminLoginDisallowed() {
        // given
        config.setAllowAdminLogin(Boolean.FALSE);
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        // when
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED)));
    }

    /** Verifies that extracting roles from access token works as expected when claim is missing */
    @Test
    public void testGetRolesFromAccessTokenWithNoneExistingClaim() {
        // given
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        // when
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED)));
    }

    /** Verifies that extracting roles from access token works as expected when claim is list of strings */
    @Test
    public void testGetRolesFromAccessTokenWithExistingClaim() {
        // given
        userRequest = new OAuth2UserRequest(
                mockClientReg, accessToken, singletonMap(ROLES_CLAIM_NAME, Arrays.asList("ROLE1", "ROLE2")));
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        // when
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED), equalTo("ROLE1"), equalTo("ROLE2")));
    }

    /** Verifies that extracting roles from access token works as expected when claim is simple string */
    @Test
    public void testGetRolesFromAccessTokenWithExistingClaimSimpleString() {
        // given
        userRequest = new OAuth2UserRequest(mockClientReg, accessToken, singletonMap(ROLES_CLAIM_NAME, "ROLE1"));
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        // when
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED), equalTo("ROLE1")));
    }

    /** Verifies that extracting roles from access token works as expected when claim is string array */
    @Test
    public void testGetRolesFromAccessTokenWithExistingClaimArray() {
        // given
        userRequest = new OAuth2UserRequest(
                mockClientReg, accessToken, singletonMap(ROLES_CLAIM_NAME, new String[] {"ROLE1", "ROLE2"}));
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        // when
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED), equalTo("ROLE1"), equalTo("ROLE2")));
    }

    /** Verifies that extracting roles from access token works as expected when using scope as source */
    @Test
    public void testGetRolesFromAccessTokenScope() {
        // given
        config.setTokenRolesClaim("scope");
        accessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1), singleton("ROLE1"));
        userRequest = new OAuth2UserRequest(mockClientReg, accessToken);
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        // when
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED), equalTo("ROLE1")));
    }

    /** Verifies that extracting roles from ID token works as expected */
    @Test
    public void testGetRolesFromIdToken() {
        // given
        context = newResolverContext(OpenIdRoleSource.IdToken);
        OidcIdToken lToken = new OidcIdToken(
                "tokenValue", now(), now().plusMillis(1), Collections.singletonMap(ROLES_CLAIM_NAME, "ROLE1"));
        OidcUserRequest lRequest = new OidcUserRequest(mockClientReg, accessToken, lToken);
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, lRequest);

        // when
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED), equalTo("ROLE1")));
    }

    /** Verifies that extracting roles from userInfo service works as expected when using authorities as source */
    @Test
    @SuppressWarnings("unchecked")
    public void testGetRolesFromUserInfoServiceAuthorities() {
        // given
        config.setTokenRolesClaim("authorities");
        context = newResolverContext(OpenIdRoleSource.UserInfo);
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        OAuth2UserService<OAuth2UserRequest, OAuth2User> mock = mock(OAuth2UserService.class);
        sut.setUserServiceSupplier(() -> mock);

        DefaultOAuth2User lUser = new DefaultOAuth2User(
                singleton(new SimpleGrantedAuthority("ROLE1")),
                Map.of("principalName", PRINCIPAL_NAME),
                "principalName");

        // when
        when(mock.loadUser(any())).thenReturn(lUser);
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED), equalTo("ROLE1")));
    }

    /** Verifies that extracting roles from userInfo service works as expected when using attributes as source */
    @Test
    @SuppressWarnings("unchecked")
    public void testGetRolesFromUserInfoServiceAttributes() {
        // given
        context = newResolverContext(OpenIdRoleSource.UserInfo);
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        OAuth2UserService<OAuth2UserRequest, OAuth2User> mock = mock(OAuth2UserService.class);
        sut.setUserServiceSupplier(() -> mock);

        DefaultOAuth2User lUser = new DefaultOAuth2User(
                singleton(new SimpleGrantedAuthority("ROLE1")),
                Map.of("principalName", PRINCIPAL_NAME, ROLES_CLAIM_NAME, "ROLE2"),
                "principalName");

        // when
        when(mock.loadUser(any())).thenReturn(lUser);
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED), equalTo("ROLE2")));
    }

    /**
     * Verifies that extracting roles from MS Graph API is skipped if MS is not the current IDP.
     *
     * @throws IOException
     */
    @Test
    public void testGetRolesFromMsGraphAPIWithIdpNotMs() throws Exception {
        // given
        context = newResolverContext(OpenIdRoleSource.MSGraphAPI);
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        MSGraphRolesResolver mock = mock(MSGraphRolesResolver.class);
        sut.setMsGraphRolesResolverSupplier(() -> mock);

        List<String> lRoleNames = List.of("ROLE1", "ROLE2");

        // when
        when(mock.resolveRoles(any(), any(), any(), any())).thenReturn(lRoleNames);
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED)));
        verify(mock, times(0)).resolveRoles(any(), any(), any(), any());
    }

    /**
     * Verifies that extracting roles from MS Graph API is working as expected with clientId "MS".
     *
     * @throws IOException
     */
    @Test
    public void testGetRolesFromMsGraphAPIWithIdpMs() throws Exception {
        // given
        context = newResolverContext(OpenIdRoleSource.MSGraphAPI);
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        MSGraphRolesResolver mock = mock(MSGraphRolesResolver.class);
        sut.setMsGraphRolesResolverSupplier(() -> mock);

        List<String> lRoleNames = List.of("ROLE1", "ROLE2");

        // when
        when(mockClientReg.getRegistrationId()).thenReturn(REG_ID_MICROSOFT);
        when(mock.resolveRoles(any(), any(), any(), any())).thenReturn(lRoleNames);
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED), equalTo("ROLE1"), equalTo("ROLE2")));
    }

    /**
     * Verifies that extracting roles from MS Graph API works with a scoped registration ID (e.g.
     * "my-filter__microsoft") as used when a filter name is configured.
     */
    @Test
    public void testGetRolesFromMsGraphAPIWithScopedRegistrationId() throws Exception {
        // given
        context = newResolverContext(OpenIdRoleSource.MSGraphAPI);
        OAuth2ResolverParam lParam = new OAuth2ResolverParam(PRINCIPAL_NAME, mockRequest, context, userRequest);

        MSGraphRolesResolver mock = mock(MSGraphRolesResolver.class);
        sut.setMsGraphRolesResolverSupplier(() -> mock);

        List<String> lRoleNames = List.of("ROLE1", "ROLE2");

        // when - use a scoped registration ID like production does
        when(mockClientReg.getRegistrationId()).thenReturn("oidc-entra__microsoft");
        when(mock.resolveRoles(any(), any(), any(), any())).thenReturn(lRoleNames);
        Collection<GeoServerRole> lRoles = sut.convert(lParam);

        // then
        assertThat(lRoles, containsInAnyOrder(equalTo(ROLE_NAME_AUTHENTICATED), equalTo("ROLE1"), equalTo("ROLE2")));
    }

    private DefaultResolverContext newResolverContext(RoleSource pRoleSource) {
        return new DefaultResolverContext(
                mockSecurityManager, "default", "default", null, mockRoleConverter, pRoleSource);
    }
}
