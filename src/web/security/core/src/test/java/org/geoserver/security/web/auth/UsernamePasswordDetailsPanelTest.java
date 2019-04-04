/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.junit.Before;
import org.junit.Test;

public class UsernamePasswordDetailsPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Override
    protected String getDetailsFormComponentId() {
        return "authenticationProviderPanel:namedConfig";
    }

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
        return AuthenticationProviderPanel.class;
    }

    protected void setUGName(String serviceName) {
        formTester.setValue("panel:content:userGroupServiceName", serviceName);
    }

    protected String getUGServiceName() {
        return formTester
                .getForm()
                .get("details:config.userGroupServiceName")
                .getDefaultModelObjectAsString();
    }

    @Before
    public void clearAuthProvider() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        if (secMgr.listAuthenticationProviders().contains("default2")) {
            SecurityAuthProviderConfig config = secMgr.loadAuthenticationProviderConfig("default2");
            secMgr.removeAuthenticationProvider(config);
        }
    }

    @Test
    public void testAddModifyRemove() throws Exception {
        initializeForXML();

        activatePanel();

        assertEquals(1, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);

        newFormTester();
        setSecurityConfigName("default2");
        setUGName("default");
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(1, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        clickAddNew();
        newFormTester();
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        setUGName("default");
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        clickSave();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        UsernamePasswordAuthenticationProviderConfig authConfig =
                (UsernamePasswordAuthenticationProviderConfig)
                        getSecurityNamedServiceConfig("default2");
        assertNotNull(authConfig);
        assertEquals("default2", authConfig.getName());
        assertEquals(
                UsernamePasswordAuthenticationProvider.class.getName(), authConfig.getClassName());
        assertEquals("default", authConfig.getUserGroupServiceName());

        // reload from manager
        authConfig =
                (UsernamePasswordAuthenticationProviderConfig)
                        getSecurityManager().loadAuthenticationProviderConfig("default2");
        assertNotNull(authConfig);
        assertEquals("default2", authConfig.getName());
        assertEquals(
                UsernamePasswordAuthenticationProvider.class.getName(), authConfig.getClassName());
        assertEquals("default", authConfig.getUserGroupServiceName());

        // test add with name clash
        clickAddNew();
        newFormTester();
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        setUGName("default");
        clickSave(); // should not work

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        testErrorMessagesWithRegExp(".*default2.*");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());
        // end test add with name clash

        // start test modify
        clickNamedServiceConfig("default");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.debugComponentTrees();
        newFormTester("panel:panel:form");
        formTester.setValue("panel:userGroupServiceName", "test");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        authConfig =
                (UsernamePasswordAuthenticationProviderConfig)
                        getSecurityNamedServiceConfig("default");
        assertEquals("default", authConfig.getUserGroupServiceName());

        clickNamedServiceConfig("default2");
        newFormTester("panel:panel:form");
        formTester.setValue("panel:userGroupServiceName", "test");
        clickSave();
        tester.assertRenderedPage(basePage.getClass());

        authConfig =
                (UsernamePasswordAuthenticationProviderConfig)
                        getSecurityNamedServiceConfig("default2");
        assertEquals("test", authConfig.getUserGroupServiceName());

        // reload from manager
        authConfig =
                (UsernamePasswordAuthenticationProviderConfig)
                        getSecurityManager().loadAuthenticationProviderConfig("default2");
        assertEquals("test", authConfig.getUserGroupServiceName());
    }

    @Test
    public void testMultipleAuthProviders() throws Exception {
        initializeForXML();

        activatePanel();

        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));

        // Test add 1
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);

        newFormTester();
        setSecurityConfigName("default_001");
        setUGName("default");
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(1, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        clickAddNew();
        newFormTester();
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default_001");
        setUGName("default");
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        clickSave();

        // Test add 2
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);

        newFormTester();
        setSecurityConfigName("default_002");
        setUGName("default");
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        clickAddNew();
        newFormTester();
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default_002");
        setUGName("default");
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        clickSave();

        // start test modify
        clickNamedServiceConfig("default_001");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.debugComponentTrees();
        newFormTester("panel:panel:form");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        clickNamedServiceConfig("default_002");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.debugComponentTrees();
        newFormTester("panel:panel:form");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        doRemove(null, "default_001");
        doRemove(null, "default_002");
    }

    @Test
    public void testRemove() throws Exception {
        initializeForXML();
        UsernamePasswordAuthenticationProviderConfig config =
                new UsernamePasswordAuthenticationProviderConfig();
        config.setName("default2");
        config.setClassName(UsernamePasswordAuthenticationProvider.class.getCanonicalName());
        config.setUserGroupServiceName("default");
        getSecurityManager().saveAuthenticationProvider(config);

        activatePanel();
        doRemove(null, "default2");

        assertNull(getSecurityManager().loadAuthenticationProvider("default2"));
    }
}
