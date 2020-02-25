/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.validation.PasswordValidatorImpl;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.junit.Test;

public class PasswordPolicyDetailsPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Override
    protected String getDetailsFormComponentId() {
        return "passwordPolicyPanel:namedConfig";
    }

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new PasswordPage();
    }

    @Override
    protected String getBasePanelId() {
        return "form:passwordPolicies";
    }

    @Override
    protected Integer getTabIndex() {
        return 2;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return PasswordPoliciesPanel.class;
    }

    protected void setDigitRequired(boolean value) {
        formTester.setValue("panel:content:digitRequired", value);
    }

    protected boolean getDigitRequired(boolean value) {
        return (Boolean)
                formTester.getForm().get("details:config.digitRequired").getDefaultModelObject();
    }

    protected void setUpperCaseRequired(boolean value) {
        formTester.setValue("panel:content:uppercaseRequired", value);
    }

    protected boolean getUpperCaseRequired(boolean value) {
        return (Boolean)
                formTester
                        .getForm()
                        .get("details:config.uppercaseRequired")
                        .getDefaultModelObject();
    }

    protected void setLowerCaseRequired(boolean value) {
        formTester.setValue("panel:content:lowercaseRequired", value);
    }

    protected boolean getLowerCaseRequired(boolean value) {
        return (Boolean)
                formTester
                        .getForm()
                        .get("details:config.lowercaseRequired")
                        .getDefaultModelObject();
    }

    protected void setUnlimted(boolean value) {
        formTester.setValue("panel:content:unlimitedMaxLength", value);
        tester.executeAjaxEvent("form:panel:content:unlimitedMaxLength", "click");
    }

    protected boolean getUnlimted(boolean value) {
        return (Boolean) formTester.getForm().get("details:unlimited").getDefaultModelObject();
    }

    protected void setMinLength(int value) {
        formTester.setValue("panel:content:minLength", Integer.valueOf(value).toString());
    }

    protected int getMinLength(int value) {
        return (Integer)
                formTester.getForm().get("details:config.minLength").getDefaultModelObject();
    }

    protected void setMaxLength(int value) {
        formTester.setValue("panel:content:maxLength:maxLength", Integer.valueOf(value).toString());
    }

    protected int getMaxLength(int value) {
        return (Integer)
                formTester.getForm().get("details:config.maxLength").getDefaultModelObject();
    }

    @Test
    public void testAddModify() throws Exception {
        initializeForXML();

        activatePanel();

        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        // detailsPage = (PasswordPolicyPage) tester.getLastRenderedPage();
        newFormTester();
        setSecurityConfigClassName(PasswordPolicyPanelInfo.class);
        newFormTester();

        setSecurityConfigName("default2");
        setMinLength(5);
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNotNull(getSecurityNamedServiceConfig("master"));

        clickAddNew();
        // detailsPage = (PasswordPolicyPage) tester.getLastRenderedPage();
        newFormTester();
        setSecurityConfigClassName(PasswordPolicyPanelInfo.class);
        setUnlimted(false);
        tester.assertVisible("form:panel:content:maxLength:maxLength");

        newFormTester();
        setSecurityConfigName("default2");
        setDigitRequired(true);
        setUpperCaseRequired(true);
        setLowerCaseRequired(true);
        setMinLength(2);
        setMaxLength(4);
        clickSave();

        tester.assertRenderedPage(basePage.getClass());

        assertEquals(3, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNotNull(getSecurityNamedServiceConfig("master"));
        PasswordPolicyConfig pwConfig =
                (PasswordPolicyConfig) getSecurityNamedServiceConfig("default2");
        assertNotNull(pwConfig);
        assertEquals("default2", pwConfig.getName());
        assertEquals(PasswordValidatorImpl.class.getName(), pwConfig.getClassName());
        assertTrue(pwConfig.isDigitRequired());
        assertTrue(pwConfig.isLowercaseRequired());
        assertTrue(pwConfig.isUppercaseRequired());
        assertEquals(2, pwConfig.getMinLength());
        assertEquals(4, pwConfig.getMaxLength());

        // reload from manager
        pwConfig = (PasswordPolicyConfig) getSecurityManager().loadPasswordPolicyConfig("default2");
        assertNotNull(pwConfig);
        assertEquals("default2", pwConfig.getName());
        assertEquals(PasswordValidatorImpl.class.getName(), pwConfig.getClassName());
        assertTrue(pwConfig.isDigitRequired());
        assertTrue(pwConfig.isLowercaseRequired());
        assertTrue(pwConfig.isUppercaseRequired());
        assertEquals(2, pwConfig.getMinLength());
        assertEquals(4, pwConfig.getMaxLength());

        // test add with name clash
        clickAddNew();

        newFormTester();
        setSecurityConfigClassName(PasswordPolicyPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        clickSave(); // should not work
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        testErrorMessagesWithRegExp(".*default2.*");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());
        // end test add with name clash

        // start test modify
        clickNamedServiceConfig("default2");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        newFormTester("panel:panel:form");
        formTester.setValue("panel:maxLength:maxLength", "27");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        pwConfig = (PasswordPolicyConfig) getSecurityNamedServiceConfig("default2");
        assertEquals(4, pwConfig.getMaxLength());

        clickNamedServiceConfig("default2");

        newFormTester("panel:panel:form");
        // setUnlimted(true);
        formTester.setValue("panel:unlimitedMaxLength", true);
        tester.executeAjaxEvent("panel:panel:form:panel:unlimitedMaxLength", "click");
        tester.assertInvisible("panel:panel:form:panel:maxLength:maxLength");
        newFormTester("panel:panel:form");
        // setDigitRequired(false);
        formTester.setValue("panel:digitRequired", false);
        // setUpperCaseRequired(false);
        formTester.setValue("panel:uppercaseRequired", false);
        // setLowerCaseRequired(false);
        formTester.setValue("panel:lowercaseRequired", false);

        formTester.setValue("panel:minLength", "3");
        // setMinLength(3);

        clickSave();
        tester.assertRenderedPage(basePage.getClass());

        pwConfig = (PasswordPolicyConfig) getSecurityNamedServiceConfig("default2");

        assertFalse(pwConfig.isDigitRequired());
        assertFalse(pwConfig.isLowercaseRequired());
        assertFalse(pwConfig.isUppercaseRequired());
        assertEquals(3, pwConfig.getMinLength());
        assertEquals(-1, pwConfig.getMaxLength());

        pwConfig = getSecurityManager().loadPasswordPolicyConfig("default2");

        assertFalse(pwConfig.isDigitRequired());
        assertFalse(pwConfig.isLowercaseRequired());
        assertFalse(pwConfig.isUppercaseRequired());
        assertEquals(3, pwConfig.getMinLength());
        assertEquals(-1, pwConfig.getMaxLength());

        // doRemove("tabbedPanel:panel:removeSelected");
    }

    @Test
    public void testRemove() throws Exception {
        initializeForXML();
        PasswordPolicyConfig config = new PasswordPolicyConfig();
        config.setName("default3");
        config.setClassName(PasswordValidatorImpl.class.getCanonicalName());
        getSecurityManager().savePasswordPolicy(config);

        activatePanel();
        doRemove(null, "default3");
        assertNull(getSecurityManager().loadPasswordPolicyConfig("default3"));
    }
}
