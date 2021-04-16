/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.auth.web.WebAuthenticationConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.auth.AuthenticationPage;
import org.geoserver.security.web.auth.WebAuthProviderPanel;
import org.geoserver.security.web.auth.WebAuthProviderPanelInfo;
import org.geoserver.security.web.role.RoleServiceChoice;
import org.junit.Before;
import org.junit.Test;

public class WebAuthPanelTest extends AbstractSecurityNamedServicePanelTest {

    static String webAuthProviderName = "web_auth";

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new AuthenticationPage();
    }

    @Override
    protected String getBasePanelId() {
        return "form:authProviders";
    }

    @Override
    protected Integer getTabIndex() {
        return 2;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return WebAuthProviderPanel.class;
    }

    @Override
    protected String getDetailsFormComponentId() {
        return "authenticationProviderPanel:namedConfig";
    }

    @Before
    public void clearAuthProvider() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        if (secMgr.listAuthenticationProviders().contains(webAuthProviderName)) {
            SecurityAuthProviderConfig config =
                    secMgr.loadAuthenticationProviderConfig(webAuthProviderName);
            secMgr.removeAuthenticationProvider(config);
        }
    }

    protected void navigateToNewWebAuthPanel(String name) throws Exception {
        initializeForXML();

        activatePanel();

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(WebAuthProviderPanelInfo.class);

        newFormTester();
        setSecurityConfigName(name);
    }

    @Test
    public void testAddWithDefault() throws Exception {

        navigateToNewWebAuthPanel(webAuthProviderName);
        // set url value
        formTester.setValue(
                "panel:content:connectionURL",
                "http://localhost:5000/auth?user={user}&pass={password}");

        formTester.setValue(
                "panel:content:webAuthorizationContainer:roleRegex",
                "^.*?roles\"\\s*.\\s*\"([^\"]+)\".*$");
        formTester.setValue("panel:content:allowHTTPConnection", true);

        clickSave();
        tester.assertNoErrorMessage();
        // assert configuration

        WebAuthenticationConfig savedConfig =
                (WebAuthenticationConfig)
                        getSecurityManager().loadAuthenticationProviderConfig(webAuthProviderName);
        assertNotNull(savedConfig);
        assertNotNull(savedConfig.getRoleRegex());
    }

    @Test
    public void testAddWithRoleService() throws Exception {

        navigateToNewWebAuthPanel(webAuthProviderName);

        // select role serice radio button
        formTester.select("panel:content:authorizationOption", 0);

        String roleServiceName = getSecurityManager().getActiveRoleService().getName();
        // set url value
        formTester.setValue(
                "panel:content:connectionURL",
                "http://localhost:5000/auth?user={user}&pass={password}");
        formTester.setValue("panel:content:allowHTTPConnection", true);

        // selecting a role service
        RoleServiceChoice roleChoice =
                (RoleServiceChoice)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:content:roleAuthorizationContainer:roleServiceName",
                                false);
        roleChoice.setModelObject(roleServiceName);
        clickSave();
        tester.assertNoErrorMessage();

        // assert configuration
        WebAuthenticationConfig savedConfig =
                (WebAuthenticationConfig)
                        getSecurityManager().loadAuthenticationProviderConfig(webAuthProviderName);
        assertNotNull(savedConfig);
        assertNotNull(savedConfig.getRoleServiceName().equalsIgnoreCase(roleServiceName));
    }

    @Test
    public void testURLValidation() throws Exception {

        navigateToNewWebAuthPanel(webAuthProviderName);
        // set url value without place holders
        formTester.setValue("panel:content:connectionURL", "http://localhost:5000/auth");
        formTester.setValue("panel:content:allowHTTPConnection", true);
        clickSave();
        assertTrue(
                findErrorMessage(
                        "Web Authentication Service URL http://localhost:5000/auth does not have place holders {user} and {password}",
                        FeedbackMessage.ERROR));

        // test the place holders are not required when using headers
        navigateToNewWebAuthPanel(webAuthProviderName);
        formTester.setValue("panel:content:connectionURL", "http://localhost:5000/auth");
        formTester.setValue("panel:content:useHeader", true);
        formTester.setValue("panel:content:allowHTTPConnection", true);
        clickSave();
        tester.assertNoErrorMessage();
    }

    @Test
    public void testInvalidRolesRegexValidation() throws Exception {

        navigateToNewWebAuthPanel(webAuthProviderName);
        formTester.setValue(
                "panel:content:connectionURL",
                "http://localhost:5000/auth?user={user}&pass={password}");

        formTester.setValue("panel:content:webAuthorizationContainer:roleRegex", "[");
        formTester.setValue("panel:content:allowHTTPConnection", true);
        clickSave();
        assertTrue(findErrorMessage("Invalid Regex Expression", FeedbackMessage.ERROR));
    }

    @Test
    public void testRoleServiceNameisValidated() throws Exception {

        navigateToNewWebAuthPanel(webAuthProviderName);

        // select role serice radio button
        formTester.select("panel:content:authorizationOption", 0);
        // set url value
        formTester.setValue(
                "panel:content:connectionURL",
                "http://localhost:5000/auth?user={user}&pass={password}");
        formTester.setValue("panel:content:allowHTTPConnection", true);

        // no role service is selected and click save
        clickSave();
        assertTrue(findErrorMessage("No Role Service has been selected", FeedbackMessage.ERROR));
    }

    @Test
    public void testAllowsHTTPConnectionValidation() throws Exception {

        // test the place holders are not required when using headers
        navigateToNewWebAuthPanel(webAuthProviderName);
        formTester.setValue("panel:content:connectionURL", "http://localhost:5000/auth");
        formTester.setValue("panel:content:useHeader", true);
        clickSave();
        assertTrue(
                findErrorMessage(
                        "HTTP connections is not allowed. If you really want to allow it flag the Allow HTTP connection checkbox",
                        FeedbackMessage.ERROR));
    }

    private boolean findErrorMessage(String msg, int level) {
        // Web Authentication Service URL http://localhost:5000/auth does not have place holders
        // {user} and {password}
        List<Serializable> messages = tester.getMessages(level);

        for (Serializable m : messages) {
            if (m.toString().contains(msg)) return true;
        }

        return false;
    }
}
