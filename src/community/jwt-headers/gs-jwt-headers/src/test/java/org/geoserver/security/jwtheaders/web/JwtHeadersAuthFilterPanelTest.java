/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.jwtheaders.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.auth.AuthenticationPage;
import org.geoserver.web.FormTestPage;
import org.junit.Test;

public class JwtHeadersAuthFilterPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Test
    public void smokeTest() {
        Model<GeoServerJwtHeadersFilterConfig> model = new Model<>(new GeoServerJwtHeadersFilterConfig());
        FormTestPage testPage = new FormTestPage(id -> new JwtHeadersAuthFilterPanel(id, model));
        tester.startPage(testPage);
    }

    // minimal filter: a new filter validates the token, so it needs a key set, an accepted issuer and a role source
    // make sure it comes back
    @Test
    public void webtest0() throws Exception {
        navigateToJwtHeadersPanel("JwtHeaderFilter1");

        selectJwtRoleSource();
        fillMinimalFilter("JwtHeaderFilter1");

        clickSave();
        tester.assertNoErrorMessage();
        clickNamedServiceConfig("JwtHeaderFilter1");
        tester.assertModelValue("panel:panel:form:panel:name", "JwtHeaderFilter1");
        tester.assertModelValue("panel:panel:form:panel:userNameHeaderAttributeName", "userNameHeaderAttributeName111");
        tester.assertModelValue("panel:panel:form:panel:validateTokenIssuer", "https://idp.example.org/realms/r");
        tester.assertModelValue("panel:panel:form:panel:allowAdminLogin", Boolean.FALSE);
        tester.assertModelValue("panel:panel:form:panel:logSensitiveInformation", Boolean.FALSE);
    }

    @Test
    public void webtest1_roles() throws Exception {

        navigateToJwtHeadersPanel("JwtHeaderFilter2");

        selectJwtRoleSource();
        fillMinimalFilter("JwtHeaderFilter2");

        clickSave();
        tester.assertNoErrorMessage();
        clickNamedServiceConfig("JwtHeaderFilter2");
        tester.assertModelValue("panel:panel:form:panel:name", "JwtHeaderFilter2");
        tester.assertModelValue("panel:panel:form:panel:userNameHeaderAttributeName", "userNameHeaderAttributeName111");

        tester.assertModelValue(
                "panel:panel:form:panel:roleSource", GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JWT);
    }

    /** A new filter validates the token by default and cannot be saved without saying how. */
    @Test
    public void webtest2_validationNeedsKeySet() throws Exception {
        navigateToJwtHeadersPanel("JwtHeaderFilter3");

        selectJwtRoleSource();
        fillMinimalFilter("JwtHeaderFilter3");
        formTester.setValue("panel:content:validateTokenSignatureURL", "");

        clickSave();
        assertFalse(tester.getMessages(FeedbackMessage.ERROR).isEmpty());
        assertNull(getSecurityManager().loadFilterConfig("JwtHeaderFilter3", false));
    }

    /** Each role source is offered once, the JWT Headers list already covers the core ones. */
    @Test
    public void testRoleSourcesListedOnce() {
        Model<GeoServerJwtHeadersFilterConfig> model = new Model<>(new GeoServerJwtHeadersFilterConfig());
        tester.startPage(new FormTestPage(id -> new JwtHeadersAuthFilterPanel(id, model)));

        DropDownChoice<?> roleSource =
                (DropDownChoice<?>) tester.getComponentFromLastRenderedPage("form:panel:roleSource");
        assertEquals(Arrays.asList(JWTHeaderRoleSource.values()), roleSource.getChoices());
    }

    @Test
    public void webtest3_userGroupServiceRoleSource() throws Exception {
        roundTripCoreRoleSource("JwtHeaderFilter4", JWTHeaderRoleSource.UserGroupService, "userGroupServiceName");
    }

    @Test
    public void webtest4_roleServiceRoleSource() throws Exception {
        roundTripCoreRoleSource("JwtHeaderFilter5", JWTHeaderRoleSource.RoleService, "roleServiceName");
    }

    // ----------------------------------------------

    private void selectJwtRoleSource() {
        formTester.setValue("panel:content:roleSource", "JWT");
        tester.executeAjaxEvent("form:panel:content:roleSource", "change");
        newFormTester();
    }

    private void fillMinimalFilter(String name) {
        fillUserNameAndValidation(name);
        formTester.setValue("panel:content:roleSource", "JWT");
        formTester.setValue("panel:content:container:panel:rolesHeaderName", "userNameHeaderAttributeName111");
        formTester.setValue("panel:content:container:panel:rolesJsonPath", "resource_access.geoserver.roles");
    }

    /** A core role source picked in the list is saved, comes back with its own settings, and saves again. */
    private void roundTripCoreRoleSource(String name, JWTHeaderRoleSource source, String serviceField)
            throws Exception {
        navigateToJwtHeadersPanel(name);
        formTester.setValue("panel:content:roleSource", source.toString());
        tester.executeAjaxEvent("form:panel:content:roleSource", "change");
        newFormTester();
        fillUserNameAndValidation(name);
        formTester.setValue("panel:content:container:panel:" + serviceField, "default");

        clickSave();
        tester.assertNoErrorMessage();
        clickNamedServiceConfig(name);
        tester.assertModelValue("panel:panel:form:panel:roleSource", source);
        tester.assertModelValue("panel:panel:form:panel:container:panel:" + serviceField, "default");

        newFormTester("panel:panel:form");
        clickSave();
        tester.assertNoErrorMessage();
        GeoServerJwtHeadersFilterConfig saved =
                (GeoServerJwtHeadersFilterConfig) getSecurityManager().loadFilterConfig(name, false);
        assertEquals(source, saved.getRoleSource());
    }

    private void fillUserNameAndValidation(String name) {
        formTester.setValue("panel:content:name", name);
        formTester.setValue("panel:content:userNameHeaderAttributeName", "userNameHeaderAttributeName111");
        formTester.setValue("panel:content:userNameJsonPath", "preferred_username");
        formTester.setValue("panel:content:validateTokenSignatureURL", "https://idp.example.org/realms/r/certs");
        formTester.setValue("panel:content:validateTokenIssuer", "https://idp.example.org/realms/r");
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
