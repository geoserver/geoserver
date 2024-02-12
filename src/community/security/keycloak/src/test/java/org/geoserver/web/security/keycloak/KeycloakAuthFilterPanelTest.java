/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.auth.AuthenticationPage;
import org.junit.Test;
import org.keycloak.representations.adapters.config.AdapterConfig;

public class KeycloakAuthFilterPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Test
    public void testRoleSourceIsPresent() throws Exception {
        navigateToKeycloakPanel("KeycloakFilter1");
        String adapterConfig = getStringAdapterConfig();
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("panel:content:name", "KeycloakFilter1");
        formTester.setValue("panel:content:adapterConfig", adapterConfig);
        formTester.setValue("panel:content:enableRedirectEntryPoint", false);
        formTester.select("panel:content:roleSource", 1);
        // formTester=tester.newFormTester("form");
        formTester.submit("save");
        tester.assertNoErrorMessage();
        clickNamedServiceConfig("KeycloakFilter1");
        tester.assertModelValue("panel:panel:form:panel:adapterConfig", adapterConfig);
        tester.assertModelValue("panel:panel:form:panel:enableRedirectEntryPoint", false);
        tester.debugComponentTrees();
        tester.assertModelValue(
                "panel:panel:form:panel:roleSource",
                PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource
                        .UserGroupService);
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
        return KeycloakAuthFilterPanel.class;
    }

    @Override
    protected String getDetailsFormComponentId() {
        return "authenticationFilterPanel:namedConfig";
    }

    private String getStringAdapterConfig() throws JsonProcessingException {
        AdapterConfig aConfig = new AdapterConfig();
        aConfig.setRealm("realm");
        aConfig.setResource("client_id");
        aConfig.setAuthServerUrl("http://localhost:8080/auth");
        ObjectMapper om = new ObjectMapper();
        return om.writeValueAsString(aConfig);
    }

    protected void navigateToKeycloakPanel(String name) throws Exception {
        initializeForXML();

        activatePanel();

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(KeycloakAuthFilterPanelInfo.class);

        newFormTester();
        setSecurityConfigName(name);
    }
}
