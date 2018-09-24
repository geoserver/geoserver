/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.Set;
import org.apache.wicket.Component;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.password.URLMasterPasswordProvider;
import org.geoserver.security.password.URLMasterPasswordProviderConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.junit.Before;
import org.junit.Test;

public class MasterPasswordProviderPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Before
    public void clearSecurityStuff() throws Exception {
        Set<String> mpProviders = getSecurityManager().listMasterPasswordProviders();
        if (mpProviders.contains("default2")) {
            MasterPasswordProviderConfig default2 =
                    getSecurityManager().loadMasterPassswordProviderConfig("default2");
            getSecurityManager().removeMasterPasswordProvder(default2);
        }
    }

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new PasswordPage();
    }

    @Override
    protected String getBasePanelId() {
        return "form:masterPasswordProviders";
    }

    @Override
    protected Integer getTabIndex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return MasterPasswordProvidersPanel.class;
    }

    @Override
    protected String getDetailsFormComponentId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Test
    public void testAddModify() throws Exception {
        initializeForXML();

        activatePanel();

        assertEquals(1, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));

        // Test simple add
        clickAddNew();
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        // detailsPage = (PasswordPolicyPage) tester.getLastRenderedPage();

        setSecurityConfigClassName(URLMasterPasswordProviderPanelInfo.class);
        newFormTester();

        setSecurityConfigName("default2");
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(1, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNull(getSecurityNamedServiceConfig("default2"));

        clickAddNew();

        setSecurityConfigClassName(URLMasterPasswordProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        formTester.setValue("panel:content:uRL", "file:passwd");
        clickSave();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNotNull(getSecurityNamedServiceConfig("default2"));

        // test add with name clash
        clickAddNew();
        setSecurityConfigClassName(URLMasterPasswordProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        formTester.setValue("panel:content:uRL", "file:passwd");
        clickSave(); // should not work

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        testErrorMessagesWithRegExp(".*default2.*");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());
        // end test add with name clash

        // start test modify
        clickNamedServiceConfig("default2");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.debugComponentTrees();
        newFormTester("panel:panel:form");
        formTester.setValue("panel:uRL", "file:passwd2");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        URLMasterPasswordProviderConfig config =
                (URLMasterPasswordProviderConfig) getSecurityNamedServiceConfig("default2");
        assertEquals(new URL("file:passwd"), config.getURL());

        clickNamedServiceConfig("default2");

        newFormTester("panel:panel:form");
        formTester.setValue("panel:uRL", "file:passwd2");
        clickSave();

        tester.assertRenderedPage(basePage.getClass());

        config = (URLMasterPasswordProviderConfig) getSecurityNamedServiceConfig("default2");
        assertEquals(new URL("file:passwd2"), config.getURL());
    }

    @Test
    public void testRemove() throws Exception {
        initializeForXML();
        URLMasterPasswordProviderConfig config = new URLMasterPasswordProviderConfig();
        config.setName("default2");
        config.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
        config.setURL(new URL("file:passwd"));
        config.setLoginEnabled(true);

        getSecurityManager().saveMasterPasswordProviderConfig(config);
        activatePanel();

        assertEquals(2, countItems());

        doRemove(null, "default2");
        assertNull(getSecurityManager().loadMasterPassswordProviderConfig("default2"));
        assertEquals(1, countItems());
    }
}
