/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2;

import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.auth.AuthenticationPage;
import org.geoserver.web.FormTestPage;
import org.junit.Test;

public class OpenIdConnectAuthProviderPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Test
    public void smokeTest() {
        Model<OpenIdConnectFilterConfig> model = new Model<>(new OpenIdConnectFilterConfig());
        FormTestPage testPage =
                new FormTestPage(id -> new OpenIdConnectAuthProviderPanel(id, model));
        tester.startPage(testPage);
    }

    @Test
    public void testResponseModeParam() throws Exception {
        String baseUrl = "https://localhost:8080";
        navigateToOpenIdPanel("OpenIdFilter1");
        formTester.setValue("panel:content:name", "OpenIdFilter1");
        formTester.setValue("panel:content:userAuthorizationUri", baseUrl + "/authorize");
        formTester.setValue("panel:content:accessTokenUri", baseUrl + "/token");
        formTester.setValue("panel:content:checkTokenEndpointUrl", baseUrl + "/checkToken");
        formTester.setValue("panel:content:logoutUri", baseUrl + "/logout");
        formTester.setValue("panel:content:scopes", "open_id");
        formTester.setValue("panel:content:cliendId", "fnruurnu4unu4");
        formTester.setValue("panel:content:clientSecret", "fnruurnu4unu4");
        formTester.setValue("panel:content:jwkURI", baseUrl + "/jwk");

        // set the response mode
        formTester.setValue("panel:content:responseMode", "query");

        clickSave();
        tester.assertNoErrorMessage();
        clickNamedServiceConfig("OpenIdFilter1");
        tester.assertModelValue("panel:panel:form:panel:responseMode", "query");
    }

    @Test
    public void testSendClientSecret() throws Exception {
        String baseUrl = "https://localhost:8080";
        navigateToOpenIdPanel("OpenIdFilter2");
        formTester.setValue("panel:content:name", "OpenIdFilter2");
        formTester.setValue("panel:content:userAuthorizationUri", baseUrl + "/authorize");
        formTester.setValue("panel:content:accessTokenUri", baseUrl + "/token");
        formTester.setValue("panel:content:checkTokenEndpointUrl", baseUrl + "/checkToken");
        formTester.setValue("panel:content:logoutUri", baseUrl + "/logout");
        formTester.setValue("panel:content:scopes", "open_id");
        formTester.setValue("panel:content:cliendId", "fnruurnu4unu4");
        formTester.setValue("panel:content:clientSecret", "fnruurnu4unu4");
        formTester.setValue("panel:content:jwkURI", baseUrl + "/jwk");

        formTester.setValue("panel:content:sendClientSecret", true);

        clickSave();
        tester.assertNoErrorMessage();
        clickNamedServiceConfig("OpenIdFilter2");
        tester.assertModelValue("panel:panel:form:panel:sendClientSecret", true);
    }

    @Test
    public void testPostRedirectUri() throws Exception {
        String baseUrl = "https://localhost:8080";
        navigateToOpenIdPanel("OpenIdFilter3");
        formTester.setValue("panel:content:name", "OpenIdFilter3");
        formTester.setValue("panel:content:userAuthorizationUri", baseUrl + "/authorize");
        formTester.setValue("panel:content:accessTokenUri", baseUrl + "/token");
        formTester.setValue("panel:content:checkTokenEndpointUrl", baseUrl + "/checkToken");
        formTester.setValue("panel:content:logoutUri", baseUrl + "/logout");
        formTester.setValue("panel:content:scopes", "open_id");
        formTester.setValue("panel:content:cliendId", "fnruurnu4unu4");
        formTester.setValue("panel:content:clientSecret", "fnruurnu4unu4");
        formTester.setValue("panel:content:jwkURI", baseUrl + "/jwk");

        formTester.setValue(
                "panel:content:postLogoutRedirectUri", "http://localhost:8080/post/redirect");

        clickSave();
        tester.assertNoErrorMessage();
        clickNamedServiceConfig("OpenIdFilter3");
        tester.assertModelValue(
                "panel:panel:form:panel:postLogoutRedirectUri",
                "http://localhost:8080/post/redirect");
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
        return OpenIdConnectAuthProviderPanel.class;
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
        setSecurityConfigClassName(OpenIdConnectAuthProviderPanelInfo.class);

        newFormTester();
        setSecurityConfigName(name);
    }
}
