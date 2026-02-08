/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.login;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_GIT_HUB;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_GOOGLE;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilterBuilder.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
import static org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent.disableButtonEvent;
import static org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent.enableButtonEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId;
import org.geoserver.web.LoginFormInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests {@link OAuth2LoginButtonManager}, in particular multi-instance tracking: a button stays enabled as long as at
 * least one filter instance enables that provider type.
 */
@RunWith(MockitoJUnitRunner.class)
public class OAuth2LoginButtonManagerTest {

    @Mock
    private LoginFormInfo oidcButton;

    @Mock
    private LoginFormInfo googleButton;

    @Mock
    private LoginFormInfo gitHubButton;

    private OAuth2LoginButtonManager sut;

    private boolean oidcEnabled;
    private String oidcLoginPath;
    private boolean googleEnabled;
    private String googleLoginPath;
    private boolean gitHubEnabled;
    private String gitHubLoginPath;

    @Before
    public void setUp() {
        // LoginFormInfo IDs mirror the XML config in applicationContext.xml
        when(oidcButton.getId()).thenReturn("openIdConnectOidcLoginButton");
        when(googleButton.getId()).thenReturn("openIdConnectGoogleLoginButton");
        when(gitHubButton.getId()).thenReturn("openIdConnectGitHubLoginButton");

        // Capture setEnabled/setLoginPath calls via Mockito doAnswer
        org.mockito.Mockito.doAnswer(inv -> {
                    oidcEnabled = inv.getArgument(0);
                    return null;
                })
                .when(oidcButton)
                .setEnabled(org.mockito.ArgumentMatchers.anyBoolean());
        org.mockito.Mockito.doAnswer(inv -> {
                    oidcLoginPath = inv.getArgument(0);
                    return null;
                })
                .when(oidcButton)
                .setLoginPath(org.mockito.ArgumentMatchers.anyString());

        org.mockito.Mockito.doAnswer(inv -> {
                    googleEnabled = inv.getArgument(0);
                    return null;
                })
                .when(googleButton)
                .setEnabled(org.mockito.ArgumentMatchers.anyBoolean());
        org.mockito.Mockito.doAnswer(inv -> {
                    googleLoginPath = inv.getArgument(0);
                    return null;
                })
                .when(googleButton)
                .setLoginPath(org.mockito.ArgumentMatchers.anyString());

        org.mockito.Mockito.doAnswer(inv -> {
                    gitHubEnabled = inv.getArgument(0);
                    return null;
                })
                .when(gitHubButton)
                .setEnabled(org.mockito.ArgumentMatchers.anyBoolean());
        org.mockito.Mockito.doAnswer(inv -> {
                    gitHubLoginPath = inv.getArgument(0);
                    return null;
                })
                .when(gitHubButton)
                .setLoginPath(org.mockito.ArgumentMatchers.anyString());

        sut = new OAuth2LoginButtonManager();
        sut.setLoginFormInfos(Arrays.asList(oidcButton, googleButton, gitHubButton));
    }

    private static String expectedLoginPath(String scopedRegId) {
        return "/" + DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + scopedRegId;
    }

    @Test
    public void testSingleFilterEnableDisable() {
        String lFilterName = "my-filter";
        String lScopedOidc = GeoServerOAuth2ClientRegistrationId.scopedRegId(lFilterName, REG_ID_OIDC);

        // when: enable OIDC for a single filter
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScopedOidc));

        // then: OIDC button enabled, path points to scoped ID
        assertTrue(oidcEnabled);
        assertEquals(expectedLoginPath(lScopedOidc), oidcLoginPath);

        // when: disable OIDC for the same filter
        sut.enablementChanged(disableButtonEvent(this, REG_ID_OIDC, lScopedOidc));

        // then: OIDC button disabled
        assertFalse(oidcEnabled);
    }

    @Test
    public void testTwoFiltersEnableSameProvider() {
        String lFilter1 = "keycloak-auth";
        String lFilter2 = "azure-auth";
        String lScoped1 = GeoServerOAuth2ClientRegistrationId.scopedRegId(lFilter1, REG_ID_OIDC);
        String lScoped2 = GeoServerOAuth2ClientRegistrationId.scopedRegId(lFilter2, REG_ID_OIDC);

        // when: both filters enable OIDC
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScoped1));
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScoped2));

        // then: OIDC button enabled, path points to the most recently enabled filter
        assertTrue(oidcEnabled);
        assertEquals(expectedLoginPath(lScoped2), oidcLoginPath);

        // when: first filter disables OIDC
        sut.enablementChanged(disableButtonEvent(this, REG_ID_OIDC, lScoped1));

        // then: button still enabled (filter2 still active), path updated to surviving filter
        assertTrue(oidcEnabled);
        assertEquals(expectedLoginPath(lScoped2), oidcLoginPath);

        // when: second filter also disables
        sut.enablementChanged(disableButtonEvent(this, REG_ID_OIDC, lScoped2));

        // then: button disabled
        assertFalse(oidcEnabled);
    }

    @Test
    public void testDifferentProvidersFromDifferentFilters() {
        String lOidcFilter = "keycloak-auth";
        String lGitHubFilter = "github-auth";
        String lScopedOidc = GeoServerOAuth2ClientRegistrationId.scopedRegId(lOidcFilter, REG_ID_OIDC);
        String lScopedGitHub = GeoServerOAuth2ClientRegistrationId.scopedRegId(lGitHubFilter, REG_ID_GIT_HUB);

        // when: OIDC filter enables OIDC, GitHub filter enables GitHub
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScopedOidc));
        sut.enablementChanged(enableButtonEvent(this, REG_ID_GIT_HUB, lScopedGitHub));

        // then: both buttons enabled with correct paths
        assertTrue(oidcEnabled);
        assertEquals(expectedLoginPath(lScopedOidc), oidcLoginPath);
        assertTrue(gitHubEnabled);
        assertEquals(expectedLoginPath(lScopedGitHub), gitHubLoginPath);

        // when: disable OIDC filter
        sut.enablementChanged(disableButtonEvent(this, REG_ID_OIDC, lScopedOidc));

        // then: OIDC disabled, GitHub still enabled
        assertFalse(oidcEnabled);
        assertTrue(gitHubEnabled);
    }

    @Test
    public void testDisableNonExistentScopedIdIsNoOp() {
        String lFilterName = "my-filter";
        String lScopedOidc = GeoServerOAuth2ClientRegistrationId.scopedRegId(lFilterName, REG_ID_OIDC);

        // when: disable a scoped ID that was never enabled
        sut.enablementChanged(disableButtonEvent(this, REG_ID_OIDC, lScopedOidc));

        // then: button disabled (no error)
        assertFalse(oidcEnabled);
    }

    @Test
    public void testOnlyMatchingButtonsAffected() {
        String lFilterName = "my-filter";
        String lScopedGoogle = GeoServerOAuth2ClientRegistrationId.scopedRegId(lFilterName, REG_ID_GOOGLE);

        // when: enable Google
        sut.enablementChanged(enableButtonEvent(this, REG_ID_GOOGLE, lScopedGoogle));

        // then: only Google button enabled
        assertTrue(googleEnabled);
        assertEquals(expectedLoginPath(lScopedGoogle), googleLoginPath);
        // OIDC and GitHub should not have been touched by this event
    }
}
