/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.jwtheaders.web;

import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.auth.AuthenticationPage;
import org.geoserver.web.FormTestPage;
import org.junit.Test;

public class JwtHeadersAuthFilterPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Test
    public void smokeTest() {
        Model<GeoServerJwtHeadersFilterConfig> model =
                new Model<>(new GeoServerJwtHeadersFilterConfig());
        FormTestPage testPage = new FormTestPage(id -> new JwtHeadersAuthFilterPanel(id, model));
        tester.startPage(testPage);
    }

    // trivial test - jus set the name and userNameHeaderAttributeName
    // make sure it comes back
    @Test
    public void webtest0() throws Exception {
        navigateToJwtHeadersPanel("JwtHeaderFilter1");

        formTester.setValue("panel:content:name", "JwtHeaderFilter1");
        formTester.setValue(
                "panel:content:userNameHeaderAttributeName", "userNameHeaderAttributeName111");

        clickSave();
        tester.assertNoErrorMessage();
        clickNamedServiceConfig("JwtHeaderFilter1");
        tester.assertModelValue("panel:panel:form:panel:name", "JwtHeaderFilter1");
        tester.assertModelValue(
                "panel:panel:form:panel:userNameHeaderAttributeName",
                "userNameHeaderAttributeName111");
    }

    @Test
    public void webtest1_roles() throws Exception {
        navigateToJwtHeadersPanel("JwtHeaderFilter1");

        formTester.setValue("panel:content:name", "JwtHeaderFilter1");
        formTester.setValue(
                "panel:content:userNameHeaderAttributeName", "userNameHeaderAttributeName111");

        formTester.setValue("panel:content:roleSource", "JWT");

        clickSave();
        tester.assertNoErrorMessage();
        clickNamedServiceConfig("JwtHeaderFilter1");
        tester.assertModelValue("panel:panel:form:panel:name", "JwtHeaderFilter1");
        tester.assertModelValue(
                "panel:panel:form:panel:userNameHeaderAttributeName",
                "userNameHeaderAttributeName111");

        tester.assertModelValue(
                "panel:panel:form:panel:roleSource",
                GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JWT);
    }

    // ----------------------------------------------
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
        return JwtHeadersAuthFilterPanel.class;
    }

    @Override
    protected String getDetailsFormComponentId() {
        return "authenticationFilterPanel:namedConfig";
    }

    protected void navigateToJwtHeadersPanel(String name) throws Exception {
        initializeForXML();

        activatePanel();

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(JwtHeadersAuthFilterPanelInfo.class);

        newFormTester();
        setSecurityConfigName(name);
    }
}
