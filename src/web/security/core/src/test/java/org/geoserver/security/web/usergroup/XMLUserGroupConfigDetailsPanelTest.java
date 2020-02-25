/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.geoserver.security.validation.PasswordValidatorImpl;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.UserGroupRoleServicesPage;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.security.xml.XMLUserGroupServiceConfig;
import org.junit.Test;

public class XMLUserGroupConfigDetailsPanelTest extends AbstractSecurityNamedServicePanelTest {

    // UserGroupTabbedPage detailsPage;

    @Override
    protected String getDetailsFormComponentId() {
        return "UserGroupTabbedPage:panel:namedConfig";
    }

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new UserGroupRoleServicesPage();
    }

    @Override
    protected String getBasePanelId() {
        return "panel:panel:userGroupServices";
    }

    @Override
    protected Integer getTabIndex() {
        return 0;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return UserGroupServicesPanel.class;
    }

    protected void setPasswordEncoderName(String encName) {
        formTester.setValue("panel:content:passwordEncoderName", encName);
    }

    protected String getPasswordEncoderName() {
        return formTester
                .getForm()
                .get("details:config.passwordEncoderName")
                .getDefaultModelObjectAsString();
    }

    protected void setPasswordPolicy(String policyName) {
        formTester.setValue("panel:content:passwordPolicyName", policyName);
    }

    protected String getPasswordPolicyName() {
        return formTester
                .getForm()
                .get("details:config.passwordPolicyName")
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

    @Test
    public void testAddModify() throws Exception {
        initializeForXML();

        activatePanel();

        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNotNull(getSecurityNamedServiceConfig("test"));
        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(XMLUserGroupServicePanelInfo.class);
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
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);

        setSecurityConfigClassName(XMLUserGroupServicePanelInfo.class);

        newFormTester();
        setPasswordEncoderName(getDigestPasswordEncoder().getName());
        setPasswordPolicy("default");
        setSecurityConfigName("default2");
        setFileName("abc.xml");
        setCheckInterval(5000);
        setValidating(true);
        clickSave();

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(basePage.getClass());
        assertEquals(3, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        XMLUserGroupServiceConfig xmlConfig =
                (XMLUserGroupServiceConfig) getSecurityNamedServiceConfig("default2");
        assertNotNull(xmlConfig);
        assertEquals("default2", xmlConfig.getName());
        assertEquals(XMLUserGroupService.class.getName(), xmlConfig.getClassName());
        assertEquals(getDigestPasswordEncoder().getName(), xmlConfig.getPasswordEncoderName());
        assertEquals(PasswordValidatorImpl.DEFAULT_NAME, xmlConfig.getPasswordPolicyName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5000, xmlConfig.getCheckInterval());
        assertEquals(true, xmlConfig.isValidating());

        // reload from manager
        xmlConfig =
                (XMLUserGroupServiceConfig)
                        getSecurityManager().loadUserGroupServiceConfig("default2");
        assertNotNull(xmlConfig);
        assertEquals("default2", xmlConfig.getName());
        assertEquals(getDigestPasswordEncoder().getName(), xmlConfig.getPasswordEncoderName());
        assertEquals(PasswordValidatorImpl.DEFAULT_NAME, xmlConfig.getPasswordPolicyName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5000, xmlConfig.getCheckInterval());
        assertEquals(true, xmlConfig.isValidating());

        // test add with name clash
        clickAddNew();
        // detailsPage = (UserGroupTabbedPage) tester.getLastRenderedPage();
        newFormTester();
        setSecurityConfigClassName(XMLUserGroupServicePanelInfo.class);

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

        // detailsPage = (UserGroupTabbedPage) tester.getLastRenderedPage();
        newFormTester("panel:panel:panel:form");
        formTester.setValue("panel:passwordPolicyName", PasswordValidatorImpl.MASTERPASSWORD_NAME);
        formTester.setValue("panel:passwordEncoderName", getPlainTextPasswordEncoder().getName());

        assertEquals(getDigestPasswordEncoder().getName(), xmlConfig.getPasswordEncoderName());
        assertEquals(PasswordValidatorImpl.DEFAULT_NAME, xmlConfig.getPasswordPolicyName());

        formTester.setValue("panel:checkInterval", "5001");
        formTester.setValue("panel:validating", true);
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        xmlConfig = (XMLUserGroupServiceConfig) getSecurityNamedServiceConfig("default");
        assertEquals(getDigestPasswordEncoder().getName(), xmlConfig.getPasswordEncoderName());
        assertEquals(PasswordValidatorImpl.DEFAULT_NAME, xmlConfig.getPasswordPolicyName());
        assertEquals("users.xml", xmlConfig.getFileName());
        assertEquals(10000, xmlConfig.getCheckInterval());
        assertEquals(true, xmlConfig.isValidating());

        clickNamedServiceConfig("default2");

        // detailsPage = (UserGroupTabbedPage) tester.getLastRenderedPage();
        newFormTester("panel:panel:panel:form");
        // setPasswordPolicy(PasswordValidatorImpl.MASTERPASSWORD_NAME);
        formTester.setValue("panel:passwordPolicyName", PasswordValidatorImpl.MASTERPASSWORD_NAME);

        //        setPasswordEncoderName(GeoserverPlainTextPasswordEncoder.BeanName);
        formTester.setValue("panel:checkInterval", "5001");
        // setCheckInterval(5001);
        formTester.setValue("panel:validating", false);
        // setValidating(false);
        clickSave();
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(basePage.getClass());

        xmlConfig = (XMLUserGroupServiceConfig) getSecurityNamedServiceConfig("default2");
        assertEquals(getDigestPasswordEncoder().getName(), xmlConfig.getPasswordEncoderName());
        assertEquals(PasswordValidatorImpl.MASTERPASSWORD_NAME, xmlConfig.getPasswordPolicyName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5001, xmlConfig.getCheckInterval());
        assertEquals(false, xmlConfig.isValidating());

        // reload from manager
        xmlConfig =
                (XMLUserGroupServiceConfig)
                        getSecurityManager().loadUserGroupServiceConfig("default2");
        assertEquals(getDigestPasswordEncoder().getName(), xmlConfig.getPasswordEncoderName());
        assertEquals(PasswordValidatorImpl.MASTERPASSWORD_NAME, xmlConfig.getPasswordPolicyName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5001, xmlConfig.getCheckInterval());
        assertEquals(false, xmlConfig.isValidating());
    }

    @Test
    public void testRemove() throws Exception {
        initializeForXML();
        XMLUserGroupServiceConfig config = new XMLUserGroupServiceConfig();
        config.setName("default3");
        config.setClassName(XMLUserGroupService.class.getCanonicalName());
        config.setPasswordEncoderName(getPlainTextPasswordEncoder().getName());
        config.setPasswordPolicyName("default");
        config.setFileName("foo.xml");
        getSecurityManager().saveUserGroupService(config);

        activatePanel();
        doRemove("tabbedPanel:panel:removeSelected", "default3");
        assertNull(getSecurityManager().loadUserGroupService("default3"));
    }
}
