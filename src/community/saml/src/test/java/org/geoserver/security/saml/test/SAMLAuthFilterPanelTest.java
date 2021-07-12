package org.geoserver.security.saml.test;

import org.apache.wicket.Component;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.auth.AuthenticationPage;
import org.geoserver.security.web.saml.SAMLAuthFilterPanel;
import org.geoserver.security.web.saml.SAMLAuthFilterPanelInfo;
import org.junit.Test;

public class SAMLAuthFilterPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Test
    public void testPasswordFieldsNotEmptyWhenEdit() throws Exception {
        navigateToSAMLPanel("defaultSAML");
        formTester.setValue(
                "panel:content:metadata", "<md:EntitiesDescriptor></md:EntitiesDescriptor>");
        formTester.setValue("panel:content:signing", true);
        formTester.setValue("panel:content:keyStorePath", "/path/to/keystore.jks");
        formTester.setValue("panel:content:keyStoreId", "keystoreId");
        formTester.setValue("panel:content:keyStorePassword", "keyStorePass");
        formTester.setValue("panel:content:keyStoreIdPassword", "keyStoreIdPassword");
        clickSave();
        tester.assertNoErrorMessage();
        clickNamedServiceConfig("defaultSAML");
        tester.debugComponentTrees();
        tester.assertModelValue("panel:panel:form:panel:keyStorePassword", "keyStorePass");
        tester.assertModelValue("panel:panel:form:panel:keyStoreIdPassword", "keyStoreIdPassword");
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
        return SAMLAuthFilterPanel.class;
    }

    @Override
    protected String getDetailsFormComponentId() {
        return "authenticationFilterPanel:namedConfig";
    }

    protected void navigateToSAMLPanel(String name) throws Exception {
        initializeForXML();

        activatePanel();

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(SAMLAuthFilterPanelInfo.class);

        newFormTester();
        setSecurityConfigName(name);
    }
}
