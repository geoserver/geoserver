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
     * the configuration object contains the input after saving and reopening. The OIDC provider is selected (default)
     * and configured. Further steps change the user input and verify changes are also written to configuration.
     *
     * <p>Note: The panel uses a mutually exclusive provider dropdown selector, so only one provider can be active at a
     * time. This test focuses on the OIDC provider which is the default and most feature-rich.
     *
     * @throws Exception
     */
    @Test
    public void testUserInputSaveModify() throws Exception {
        String filterName = "OpenIdFilter1";
        navigateToOpenIdPanel(filterName);

        String prefix = "panel:content:";
        String baseUrl = "https://localhost:9090";
        String baseUrlComponentPath = prefix + "baseRedirectUri";

        // --- Phase 1: trigger AJAX events to update dynamic state ---

        // baseRedirectUri AJAX: updates redirect URIs for all providers
        formTester.setValue(baseUrlComponentPath, baseUrl + "/geoserver");
        Component baseUriComponent = formTester.getForm().get(baseUrlComponentPath);
        tester.executeAjaxEvent(baseUriComponent, "change");

        // providerSelector AJAX: calls setSelectedProvider("oidc") which sets oidcEnabled=true
        // and updates container visibility. This is necessary because
        // AjaxFormComponentUpdatingBehavior explicitly calls config.setSelectedProvider().
        formTester.select(prefix + "providerSelector", 0);
        Component providerSelectorComponent = formTester.getForm().get(prefix + "providerSelector");
        tester.executeAjaxEvent(providerSelectorComponent, "change");

        // --- Phase 2: re-set ALL form values after AJAX events ---
        // (WicketTester clears queued form data on each AJAX request)

        // Common fields
        formTester.setValue(baseUrlComponentPath, baseUrl + "/geoserver");
        formTester.setValue(prefix + "name", filterName);
        formTester.setValue(prefix + "postLogoutRedirectUri", baseUrl + "/geoserver/postlogout");
        formTester.setValue(prefix + "enableRedirectAuthenticationEntryPoint", false);

        // Re-select OIDC provider so the dropdown value is submitted with the form save
        formTester.select(prefix + "providerSelector", 0);

        // OIDC provider settings (pfv:4 — the 4th provider panel added by addProviderComponents)
        prefix = "panel:content:pfv:4:settings:";
        formTester.setValue(prefix + "clientId", "oidcClientId");
        formTester.setValue(prefix + "clientSecret", "oidcClientSecret");
        formTester.setValue(prefix + "userNameAttribute", "oidcUserNameAttribute");
        formTester.setValue(prefix + "displayOnScopeSupport:scopes", "oidcScopes");

        String authUrl = "https://localhost:9000";
        formTester.setValue(prefix + "displayOnOidc:oidcTokenUri", authUrl + "/token");
        formTester.setValue(prefix + "displayOnOidc:oidcAuthorizationUri", authUrl + "/authorize");
        formTester.setValue(prefix + "displayOnOidc:oidcUserInfoUri", authUrl + "/userinfo");
        formTester.setValue(prefix + "displayOnOidc:oidcJwkSetUri", authUrl + "/jws.json");
        formTester.setValue(prefix + "displayOnOidc:oidcLogoutUri", authUrl + "/logout");

        formTester.setValue(prefix + "displayOnOidc:oidcForceAuthorizationUriHttps", true);
        formTester.setValue(prefix + "displayOnOidc:oidcForceTokenUriHttps", true);
        formTester.setValue(prefix + "displayOnOidc:disableSignatureValidation", true);
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
        assertFalse(lConfig.getEnableRedirectAuthenticationEntryPoint());

        // OIDC should be the only enabled provider (dropdown is mutually exclusive)
        assertTrue(lConfig.isOidcEnabled());
        assertFalse(lConfig.isGoogleEnabled());
        assertFalse(lConfig.isGitHubEnabled());
        assertFalse(lConfig.isMsEnabled());

        // OIDC values
        assertEquals("oidcClientId", lConfig.getOidcClientId());
        assertEquals("oidcClientSecret", lConfig.getOidcClientSecret());
        assertEquals("oidcUserNameAttribute", lConfig.getOidcUserNameAttribute());
        assertEquals("oidcScopes", lConfig.getOidcScopes());
        assertEquals("https://localhost:9090/geoserver/web/login/oauth2/code/oidc", lConfig.getOidcRedirectUri());

        assertTrue(lConfig.getOidcForceAuthorizationUriHttps());
        assertTrue(lConfig.isDisableSignatureValidation());
        assertTrue(lConfig.isOidcUsePKCE());
        assertTrue(lConfig.isOidcAllowUnSecureLogging());
        assertEquals("query", lConfig.getOidcResponseMode());
        assertTrue(lConfig.isOidcAuthenticationMethodPostSecret());

        tester.assertModelValue("panel:panel:form:panel:pfv:4:settings:displayOnOidc:oidcResponseMode", "query");

        // when: some values changed in edit mode
        prefix = "panel:pfv:4:settings:";

        formTester.setValue(prefix + "displayOnOidc:oidcForceAuthorizationUriHttps", false);
        formTester.setValue(prefix + "displayOnOidc:disableSignatureValidation", false);
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
        assertFalse(lConfig.isDisableSignatureValidation());
        assertFalse(lConfig.isOidcUsePKCE());
        assertFalse(lConfig.isOidcAllowUnSecureLogging());
        assertNull(lConfig.getOidcResponseMode());
        assertFalse(lConfig.isOidcAuthenticationMethodPostSecret());
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
