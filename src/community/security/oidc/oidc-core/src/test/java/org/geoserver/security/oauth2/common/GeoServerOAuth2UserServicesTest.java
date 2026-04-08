/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import static java.time.Instant.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.filter.GeoServerRoleResolvers.DefaultResolverContext;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.oauth2.common.GeoServerOAuth2UserServices.GeoServerOAuth2UserService;
import org.geoserver.security.oauth2.common.GeoServerOAuth2UserServices.GeoServerOidcUserService;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails.UserInfoEndpoint;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
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
    private ClientRegistration mockClientReg = mock(ClientRegistration.class);
    private DefaultOAuth2UserService mockOAuth2UserService = mock(DefaultOAuth2UserService.class);
    private OidcUserService mockOidcService = mock(OidcUserService.class);
    private GeoServerOAuth2RoleResolver mockRoleResolver = mock(GeoServerOAuth2RoleResolver.class);
    private ProviderDetails mockProviderDetails = mock(ProviderDetails.class);
    private UserInfoEndpoint mockUserInfoEndpoint = mock(UserInfoEndpoint.class);

    private Supplier<HttpServletRequest> requestSupplier = () -> mockRequest;
    private DefaultResolverContext resolverContext =
            new DefaultResolverContext(securityManager, "default", "default", null, roleConverter, roleSource);

    private GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

    @Before
    public void setUp() {
        when(mockClientReg.getProviderDetails()).thenReturn(mockProviderDetails);
        when(mockProviderDetails.getUserInfoEndpoint()).thenReturn(mockUserInfoEndpoint);
        when(mockUserInfoEndpoint.getUserNameAttributeName()).thenReturn(USER_NAME_ATTR);
    }

    /** Smoke test for {@link GeoServerOAuth2UserService}. */
    @Test
    public void testGeoServerOAuth2UserService() {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OAuth2UserRequest lUserRequest = new OAuth2UserRequest(mockClientReg, lAccessToken);

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
        when(mockOAuth2UserService.loadUser(any(OAuth2UserRequest.class))).thenReturn(lDelgatesUser);
        OAuth2User lUser = sut.loadUser(lUserRequest);

        // then
        assertNotNull(lUser);
        assertThat(lUser.getAuthorities(), containsInAnyOrder(equalTo("R1"), equalTo("R2")));
    }

    /** Smoke test for {@link GeoServerOidcUserService}. */
    @Test
    public void testGeoServerOidcUserService() {
        // given
        OAuth2AccessToken lAccessToken =
                new OAuth2AccessToken(TokenType.BEARER, "tokenValue", now(), now().plusMillis(1));
        OidcIdToken lIdToken = new OidcIdToken(
                "idTokenValue", now(), now().plusMillis(1), Map.of("sub", "james@b.co", USER_NAME_ATTR, "james"));
        OidcUserRequest lUserRequest = new OidcUserRequest(mockClientReg, lAccessToken, lIdToken);

        GeoServerOidcUserService sut = new GeoServerOidcUserService(resolverContext, requestSupplier, config);
        sut.setDelegateSupplier(() -> mockOidcService);
        sut.setResolverSupplier(() -> mockRoleResolver);

        DefaultOidcUser lDelgatesUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE1")), lIdToken);
        List<GeoServerRole> lResolversRoles = List.of(new GeoServerRole("R1"), new GeoServerRole("R2"));

        // when
        when(mockRoleResolver.convert(any(ResolverParam.class))).thenReturn(lResolversRoles);
        when(mockOidcService.loadUser(any(OidcUserRequest.class))).thenReturn(lDelgatesUser);
        OidcUser lUser = sut.loadUser(lUserRequest);

        // then
        assertNotNull(lUser);
        assertThat(lUser.getAuthorities(), containsInAnyOrder(equalTo("R1"), equalTo("R2")));
    }
}
