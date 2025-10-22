/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/** */
package org.geoserver.security.oauth2.common;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_MICROSOFT;
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
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.filter.GeoServerRoleResolvers.DefaultResolverContext;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.oauth2.common.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
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
     * Verifies that users named "admin" or "root" at identity provider do not receive any roles to prevent from
     * accidental local admin access.
     */
    @Test
    public void testGetRolesIsEmptyForGsLocalAdmins() {
        for (String lName : new String[] {"admin", "root"}) {
            // given
            OAuth2ResolverParam lParam = new OAuth2ResolverParam(lName, mockRequest, context, userRequest);

            // when
            Collection<GeoServerRole> lRoles = sut.convert(lParam);

            // then
            assertTrue("Expecting no roles for " + lName, lRoles.isEmpty());
        }
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

    private DefaultResolverContext newResolverContext(RoleSource pRoleSource) {
        return new DefaultResolverContext(
                mockSecurityManager, "default", "default", null, mockRoleConverter, pRoleSource);
    }
}
