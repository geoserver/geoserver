/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.UserGroupRoleServicesPage;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLRoleServiceConfig;
import org.junit.Before;
import org.junit.Test;

public class XMLRoleConfigDetailsPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Override
    protected String getDetailsFormComponentId() {
        return "RoleTabbedPage:panel:namedConfig";
    }

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new UserGroupRoleServicesPage();
    }

    @Override
    protected String getBasePanelId() {
        return "panel:panel:roleServices";
    }

    @Override
    protected Integer getTabIndex() {
        return 1;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return RoleServicesPanel.class;
    }

    protected void setAdminRoleName(String roleName) {
        formTester.setValue("panel:content:adminRoleName", roleName);
    }

    protected String getAdminRoleName() {
        return formTester
                .getForm()
                .get("details:config.adminRoleName")
                .getDefaultModelObjectAsString();
    }

    protected void setFileName(String fileName) {
        formTester.setValue("panel:content:fileName", fileName);
    }

    protected String getFileName() {
        return formTester.getForm().get("details:config.fileName").getDefaultModelObjectAsString();
    }

    protected void setCheckInterval(Integer interval) {
        formTester.setValue("panel:content:checkInterval", interval.toString());
    }

    protected Integer getCheckInterval() {
        String temp =
                formTester
                        .getForm()
                        .get("details:config.checkInterval")
                        .getDefaultModelObjectAsString();
        if (temp == null || temp.length() == 0) return 0;
        return Integer.valueOf(temp);
    }

    protected void setValidating(Boolean flag) {
        formTester.setValue("panel:content:validating", flag);
    }

    protected Boolean getValidating() {
        String temp =
                formTester
                        .getForm()
                        .get("details:config.validating")
                        .getDefaultModelObjectAsString();
        return Boolean.valueOf(temp);
    }

    @Before
    public void removeRoleService2() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        if (secMgr.listRoleServices().contains("default2")) {
            SecurityRoleServiceConfig roleService = secMgr.loadRoleServiceConfig("default2");
            secMgr.removeRoleService(roleService);
        }
    }

    @Test
    public void testAddModifyRemove() throws Exception {
        initializeForXML();

        activatePanel();

        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNotNull(getSecurityNamedServiceConfig("test"));
        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        // detailsPage = (RoleTabbedPage) tester.getLastRenderedPage();
        newFormTester();
        setSecurityConfigClassName(XMLRoleServicePanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        setFileName("abc.xml");
        setCheckInterval(5000);
        setValidating(true);
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        clickAddNew();
        newFormTester();
        setSecurityConfigClassName(XMLRoleServicePanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        setFileName("abc.xml");
        setCheckInterval(5000);
        setValidating(true);
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        clickSave();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(3, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        XMLRoleServiceConfig xmlConfig =
                (XMLRoleServiceConfig) getSecurityNamedServiceConfig("default2");
        assertNotNull(xmlConfig);
        assertEquals("default2", xmlConfig.getName());
        assertEquals(XMLRoleService.class.getName(), xmlConfig.getClassName());
        assertNull(xmlConfig.getAdminRoleName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5000, xmlConfig.getCheckInterval());
        assertEquals(true, xmlConfig.isValidating());

        // reload from manager
        xmlConfig = (XMLRoleServiceConfig) getSecurityManager().loadRoleServiceConfig("default2");
        assertNotNull(xmlConfig);
        assertEquals("default2", xmlConfig.getName());
        assertEquals(XMLRoleService.class.getName(), xmlConfig.getClassName());
        assertNull(xmlConfig.getAdminRoleName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5000, xmlConfig.getCheckInterval());
        assertEquals(true, xmlConfig.isValidating());

        // test add with name clash
        clickAddNew();
        // detailsPage = (RoleTabbedPage) tester.getLastRenderedPage();
        newFormTester();
        setSecurityConfigClassName(XMLRoleServicePanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        clickSave(); // should not work
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        testErrorMessagesWithRegExp(".*default2.*");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());
        // end test add with name clash

        // start test modify
        clickNamedServiceConfig("default");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        // detailsPage = (RoleTabbedPage) tester.getLastRenderedPage();
        newFormTester("panel:panel:panel:form");
        tester.debugComponentTrees();
        formTester.setValue("panel:adminRoleName", "ROLE_ADMINISTRATOR");

        // setFileName("abcd.xml");
        formTester.setValue("panel:checkInterval", "5001");
        // setCheckInterval(5001);
        formTester.setValue("panel:validating", true);
        // setValidating(true);
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        xmlConfig = (XMLRoleServiceConfig) getSecurityNamedServiceConfig("default");
        assertEquals(XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE, xmlConfig.getAdminRoleName());
        assertEquals(
                XMLRoleService.DEFAULT_LOCAL_GROUP_ADMIN_ROLE, xmlConfig.getGroupAdminRoleName());
        assertEquals("roles.xml", xmlConfig.getFileName());
        assertEquals(10000, xmlConfig.getCheckInterval());
        assertEquals(true, xmlConfig.isValidating());

        clickNamedServiceConfig("default2");
        // detailsPage = (RoleTabbedPage) tester.getLastRenderedPage();
        newFormTester("panel:panel:panel:form");
        formTester.setValue("panel:adminRoleName", null);

        // setFileName("abcd.xml");
        formTester.setValue("panel:checkInterval", "5001");
        // setCheckInterval(5001);
        formTester.setValue("panel:validating", false);

        clickSave();
        tester.assertRenderedPage(basePage.getClass());

        xmlConfig = (XMLRoleServiceConfig) getSecurityNamedServiceConfig("default2");
        assertNull(xmlConfig.getAdminRoleName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5001, xmlConfig.getCheckInterval());
        assertEquals(false, xmlConfig.isValidating());

        // reload from manager
        xmlConfig = (XMLRoleServiceConfig) getSecurityManager().loadRoleServiceConfig("default2");
        assertNull(xmlConfig.getAdminRoleName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5001, xmlConfig.getCheckInterval());
        assertEquals(false, xmlConfig.isValidating());

        // doRemove("tabbedPanel:panel:removeSelected");
    }

    @Test
    public void testRemove() throws Exception {
        initializeForXML();

        XMLRoleServiceConfig config = new XMLRoleServiceConfig();
        config.setName("default2");
        config.setClassName(XMLRoleService.class.getCanonicalName());
        config.setFileName("foo.xml");
        getSecurityManager().saveRoleService(config);
        activatePanel();

        doRemove(null, "default2");
        assertNull(getSecurityManager().loadRoleService("default2"));
    }
}
