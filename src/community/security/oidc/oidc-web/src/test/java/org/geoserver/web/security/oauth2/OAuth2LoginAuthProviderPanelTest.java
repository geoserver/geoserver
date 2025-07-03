/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.web.security.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.auth.AuthenticationPage;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.security.oauth2.login.OAuth2LoginAuthProviderPanel;
import org.geoserver.web.security.oauth2.login.OAuth2LoginAuthProviderPanelInfo;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link OAuth2LoginAuthProviderPanel} */
public class OAuth2LoginAuthProviderPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Before
    public void setup() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://localhost/geoserver");
    }

    @Test
    public void smokeTest() {
        Model<GeoServerOAuth2LoginFilterConfig> model = new Model<>(new GeoServerOAuth2LoginFilterConfig());
        FormTestPage testPage = new FormTestPage(id -> new OAuth2LoginAuthProviderPanel(id, model));
        tester.startPage(testPage);
    }

    /**
     * Creates a new configuration for a {@link GeoServerOAuth2LoginFilterConfig} providing all user input and verifies
     * the configuration object contains the input after saving and reopening. Further steps change the user input and
     * verify changes are also written to configuration.
     *
     * @throws Exception
     */
    @Test
    public void testUserInputSaveModify() throws Exception {
        String filterName = "OpenIdFilter1";
        navigateToOpenIdPanel(filterName);

        // redirectUri Ajax test
        // Unfortunately wicketTester forgets existing form data on ajax request, even if not submitting.
        // So input has to provided twice. I think is a wicketTester bug...
        String prefix = "panel:content:";
        String baseUrl = "https://localhost:9090";
        String baseUrlComponentPath = prefix + "baseRedirectUri";
        formTester.setValue(baseUrlComponentPath, baseUrl + "/geoserver");
        Component lComponent = formTester.getForm().get(baseUrlComponentPath);
        tester.executeAjaxEvent(lComponent, "change");
        formTester.setValue(baseUrlComponentPath, baseUrl + "/geoserver");

        // filter
        formTester.setValue(prefix + "name", filterName);

        // common
        formTester.setValue(prefix + "postLogoutRedirectUri", baseUrl + "/geoserver/postlogout");
        formTester.setValue(prefix + "enableRedirectAuthenticationEntryPoint", false);

        // Google
        prefix = "panel:content:pfv:1:";
        setBasicProviderValues(prefix, "google");

        // GitHub
        prefix = "panel:content:pfv:2:";
        setBasicProviderValues(prefix, "gitHub");

        // Microsoft
        prefix = "panel:content:pfv:3:";
        setBasicProviderValues(prefix, "ms");
        prefix = prefix + "settings:";
        formTester.setValue(prefix + "displayOnScopeSupport:scopes", "msScopes");

        // OIDC
        prefix = "panel:content:pfv:4:";
        setBasicProviderValues(prefix, "oidc");
        prefix = prefix + "settings:";
        formTester.setValue(prefix + "displayOnScopeSupport:scopes", "oidcScopes");

        String authUrl = "https://localhost:9000";
        formTester.setValue(prefix + "displayOnOidc:oidcTokenUri", authUrl + "/token");
        formTester.setValue(prefix + "displayOnOidc:oidcAuthorizationUri", authUrl + "/authorize");
        formTester.setValue(prefix + "displayOnOidc:oidcUserInfoUri", authUrl + "/userinfo");
        formTester.setValue(prefix + "displayOnOidc:oidcJwkSetUri", authUrl + "/jws.json");
        formTester.setValue(prefix + "displayOnOidc:oidcLogoutUri", authUrl + "/logout");

        formTester.setValue(prefix + "displayOnOidc:oidcForceAuthorizationUriHttps", true);
        formTester.setValue(prefix + "displayOnOidc:oidcForceTokenUriHttps", true);
        formTester.setValue(prefix + "displayOnOidc:oidcEnforceTokenValidation", true);
        formTester.setValue(prefix + "displayOnOidc:oidcUsePKCE", true);
        formTester.setValue(prefix + "displayOnOidc:oidcAllowUnSecureLogging", true);

        formTester.setValue(prefix + "displayOnOidc:oidcResponseMode", "query");
        formTester.setValue(prefix + "displayOnOidc:oidcAuthenticationMethodPostSecret", true);

        // when: save
        clickSave();

        // then: no error
        tester.assertNoErrorMessage();

        // when: open edit
        clickNamedServiceConfig(filterName);

        // then: assert all values present in configuration
        newFormTester("panel:panel:form");
        Component lPanel = formTester.getForm().get("panel");
        assertNotNull(lPanel);
        assertEquals(OAuth2LoginAuthProviderPanel.class, lPanel.getClass());
        OAuth2LoginAuthProviderPanel lOauthPanel = (OAuth2LoginAuthProviderPanel) lPanel;
        GeoServerOAuth2LoginFilterConfig lConfig = lOauthPanel.getConfigModel().getObject();

        // common
        assertEquals("https://localhost:9090/geoserver", lConfig.getBaseRedirectUri());
        assertEquals("https://localhost:9090/geoserver/postlogout", lConfig.getPostLogoutRedirectUri());
        assertEquals(Boolean.FALSE, lConfig.getEnableRedirectAuthenticationEntryPoint());

        // Google
        assertEquals(Boolean.TRUE, lConfig.isGoogleEnabled());
        assertEquals("googleClientId", lConfig.getGoogleClientId());
        assertEquals("googleClientSecret", lConfig.getGoogleClientSecret());
        assertEquals("googleUserNameAttribute", lConfig.getGoogleUserNameAttribute());
        assertEquals("https://localhost:9090/geoserver/login/oauth2/code/google", lConfig.getGoogleRedirectUri());

        // gitHub
        assertEquals(Boolean.TRUE, lConfig.isGitHubEnabled());
        assertEquals("gitHubClientId", lConfig.getGitHubClientId());
        assertEquals("gitHubClientSecret", lConfig.getGitHubClientSecret());
        assertEquals("gitHubUserNameAttribute", lConfig.getGitHubUserNameAttribute());
        assertEquals("https://localhost:9090/geoserver/login/oauth2/code/gitHub", lConfig.getGitHubRedirectUri());

        // MS
        assertEquals(Boolean.TRUE, lConfig.isMsEnabled());
        assertEquals("msClientId", lConfig.getMsClientId());
        assertEquals("msClientSecret", lConfig.getMsClientSecret());
        assertEquals("msUserNameAttribute", lConfig.getMsUserNameAttribute());
        assertEquals("msScopes", lConfig.getMsScopes());
        assertEquals("https://localhost:9090/geoserver/login/oauth2/code/microsoft", lConfig.getMsRedirectUri());

        // OIDC
        assertEquals(Boolean.TRUE, lConfig.isOidcEnabled());
        assertEquals("oidcClientId", lConfig.getOidcClientId());
        assertEquals("oidcClientSecret", lConfig.getOidcClientSecret());
        assertEquals("oidcUserNameAttribute", lConfig.getOidcUserNameAttribute());
        assertEquals("oidcScopes", lConfig.getOidcScopes());
        assertEquals("https://localhost:9090/geoserver/login/oauth2/code/oidc", lConfig.getOidcRedirectUri());

        assertTrue(lConfig.getOidcForceAuthorizationUriHttps());
        assertTrue(lConfig.isOidcEnforceTokenValidation());
        assertTrue(lConfig.isOidcUsePKCE());
        assertTrue(lConfig.isOidcAllowUnSecureLogging());
        assertEquals("query", lConfig.getOidcResponseMode());
        assertTrue(lConfig.isOidcAuthenticationMethodPostSecret());

        tester.assertModelValue("panel:panel:form:panel:pfv:4:settings:displayOnOidc:oidcResponseMode", "query");

        // when: some values changed in edit mode
        prefix = "panel:pfv:4:settings:";

        formTester.setValue(prefix + "displayOnOidc:oidcForceAuthorizationUriHttps", false);
        formTester.setValue(prefix + "displayOnOidc:oidcEnforceTokenValidation", false);
        formTester.setValue(prefix + "displayOnOidc:oidcUsePKCE", false);
        formTester.setValue(prefix + "displayOnOidc:oidcAllowUnSecureLogging", false);
        formTester.setValue(prefix + "displayOnOidc:oidcResponseMode", "");
        formTester.setValue(prefix + "displayOnOidc:oidcAuthenticationMethodPostSecret", false);

        // when: saved
        clickSave();

        // then: no error
        tester.assertNoErrorMessage();
        clickNamedServiceConfig(filterName);

        // then: in edit mode all modified values must be present
        newFormTester("panel:panel:form");
        lPanel = formTester.getForm().get("panel");
        assertNotNull(lPanel);
        assertEquals(OAuth2LoginAuthProviderPanel.class, lPanel.getClass());
        lOauthPanel = (OAuth2LoginAuthProviderPanel) lPanel;
        lConfig = lOauthPanel.getConfigModel().getObject();

        assertFalse(lConfig.getOidcForceAuthorizationUriHttps());
        assertFalse(lConfig.isOidcEnforceTokenValidation());
        assertFalse(lConfig.isOidcUsePKCE());
        assertFalse(lConfig.isOidcAllowUnSecureLogging());
        assertNull(lConfig.getOidcResponseMode());
        assertFalse(lConfig.isOidcAuthenticationMethodPostSecret());
    }

    private void setBasicProviderValues(String pPrefix, String pValuePrefix) {
        String enableComponentPath = pPrefix + "enabled";
        formTester.setValue(enableComponentPath, true);

        pPrefix = pPrefix + "settings:";
        formTester.setValue(pPrefix + "clientId", pValuePrefix + "ClientId");
        formTester.setValue(pPrefix + "clientSecret", pValuePrefix + "ClientSecret");
        formTester.setValue(pPrefix + "userNameAttribute", pValuePrefix + "UserNameAttribute");
    }

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new AuthenticationPage();
    }

    @Override
    protected String getBasePanelId() {
        return "form:authFilters";
    }

    @Override
    protected Integer getTabIndex() {
        return 2;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return OAuth2LoginAuthProviderPanel.class;
    }

    @Override
    protected String getDetailsFormComponentId() {
        return "authenticationFilterPanel:namedConfig";
    }

    protected void navigateToOpenIdPanel(String name) throws Exception {
        initializeForXML();

        activatePanel();

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(OAuth2LoginAuthProviderPanelInfo.class);

        newFormTester();
        setSecurityConfigName(name);
    }
}
