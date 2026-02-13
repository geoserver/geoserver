/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.ldap.LDAPUserGroupServiceConfig;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Test;

/**
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * @author Niels Charlier
 */
public class LDAPUserGroupServicePanelTest extends LDAPWicketTestSupport {

    LDAPUserGroupServicePanel current;

    LDAPUserGroupServiceConfig config;

    FeedbackPanel feedbackPanel = null;

    protected void setupPanel(boolean needsAuthentication, boolean setRequiredFields) {
        config = new LDAPUserGroupServiceConfig();
        config.setName("test");
        if (setRequiredFields) {
            config.setServerURL(getServerURL());
            config.setGroupSearchBase(GROUPS_BASE);
            config.setUserSearchBase(USERS_BASE);
        }
        config.setBindBeforeGroupSearch(needsAuthentication);
        config.setGroupSearchFilter(GROUP_SEARCH_FILTER);
        config.setUser(AUTH_USER);
        config.setPassword(AUTH_PASSWORD);
        setupPanel();
    }

    protected void setupPanel() {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public Component buildComponent(String id) {

                                return current = new LDAPUserGroupServicePanel(id, new Model<>(config));
                            }
                        },
                        new CompoundPropertyModel<>(config)) {

                    private static final long serialVersionUID = -4090244876841730821L;

                    @Override
                    protected void onInitialize() {
                        feedbackPanel = new FeedbackPanel("feedback");
                        feedbackPanel.setOutputMarkupId(true);
                        add(feedbackPanel);
                        super.onInitialize();
                    }
                });
    }

    @Test
    public void testDataLoadedFromConfigurationWithoutAuthentication() throws Exception {
        directoryService.setAllowAnonymousAccess(true);
        setupPanel(false, true);
        checkBaseConfig();

        assertNull(tester.getComponentFromLastRenderedPage("form:panel:authenticationPanel:user"));
        assertNull(tester.getComponentFromLastRenderedPage("form:panel:authenticationPanel:password"));
    }

    @Test
    public void testRequiredFields() throws Exception {
        directoryService.setAllowAnonymousAccess(true);
        setupPanel(false, false);

        tester.newFormTester("form").submit();

        tester.assertErrorMessages(
                "Field 'Server URL' is required.",
                "Field 'Group search base' is required.",
                "Field 'User search base' is required.");
    }

    @Test
    public void testDataLoadedFromConfigurationWithAuthentication() throws Exception {
        directoryService.setAllowAnonymousAccess(true);
        setupPanel(true, true);
        checkBaseConfig();

        tester.assertModelValue("form:panel:authenticationPanel:user", AUTH_USER);
        tester.assertModelValue("form:panel:authenticationPanel:password", AUTH_PASSWORD);
    }

    @Test
    public void testAuthenticationDisabled() throws Exception {
        directoryService.setAllowAnonymousAccess(true);
        setupPanel(false, true);
        tester.assertInvisible("form:panel:authenticationPanel");
        tester.newFormTester("form").setValue("panel:bindBeforeGroupSearch", "on");
        tester.executeAjaxEvent("form:panel:bindBeforeGroupSearch", "click");
        tester.assertVisible("form:panel:authenticationPanel");
    }

    @Test
    public void testAuthenticationEnabled() throws Exception {
        directoryService.setAllowAnonymousAccess(true);
        setupPanel(true, true);
        tester.assertVisible("form:panel:authenticationPanel");
        tester.newFormTester("form").setValue("panel:bindBeforeGroupSearch", "");
        tester.executeAjaxEvent("form:panel:bindBeforeGroupSearch", "click");
        tester.assertInvisible("form:panel:authenticationPanel");
    }

    private void checkBaseConfig() {
        tester.assertModelValue("form:panel:serverURL", getServerURL());
        tester.assertModelValue("form:panel:groupSearchBase", GROUPS_BASE);
        tester.assertModelValue("form:panel:groupSearchFilter", GROUP_SEARCH_FILTER);
        tester.assertModelValue("form:panel:allGroupsSearchFilter", config.getAllGroupsSearchFilter());
    }
}
