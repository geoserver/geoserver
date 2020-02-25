/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import static org.junit.Assert.assertNull;

import java.io.Serializable;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.CreateLdapServerRule;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.ldap.LDAPRoleServiceConfig;
import org.geoserver.security.ldap.LDAPTestUtils;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

/** @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it" */
@CreateLdapServer(
    transports = {
        @CreateTransport(
            protocol = "LDAP",
            address = "localhost",
            port = LDAPTestUtils.LDAP_SERVER_PORT
        )
    },
    allowAnonymousAccess = true
)
@CreateDS(
    name = "myDS",
    partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)}
)
@ApplyLdifFiles({"data.ldif"})
public class LDAPRoleServicePanelTest extends AbstractSecurityWicketTestSupport {

    private static final String GROUPS_BASE = "ou=Groups";

    private static final String GROUP_SEARCH_FILTER = "member=cn={0}";

    private static final String AUTH_USER = "admin";

    private static final String AUTH_PASSWORD = "secret";

    LDAPRoleServicePanel current;

    String relBase = "panel:";
    String base = "form:" + relBase;

    LDAPRoleServiceConfig config;

    FeedbackPanel feedbackPanel = null;

    private static final String ldapServerUrl = LDAPTestUtils.LDAP_SERVER_URL;
    private static final String basePath = LDAPTestUtils.LDAP_BASE_PATH;

    @ClassRule public static CreateLdapServerRule serverRule = new CreateLdapServerRule();

    @After
    public void tearDown() throws Exception {}

    protected void setupPanel(boolean needsAuthentication, boolean setRequiredFields) {
        config = new LDAPRoleServiceConfig();
        config.setName("test");
        if (setRequiredFields) {
            config.setServerURL(ldapServerUrl + "/" + basePath);
            config.setGroupSearchBase(GROUPS_BASE);
        }
        config.setBindBeforeGroupSearch(needsAuthentication);
        config.setGroupSearchFilter(GROUP_SEARCH_FILTER);
        config.setUser(AUTH_USER);
        config.setPassword(AUTH_PASSWORD);
        setupPanel(config);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // disable url parameter encoding for these tests
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        config.setEncryptingUrlParams(false);
        getSecurityManager().saveSecurityConfig(config);
    }

    protected void setupPanel(LDAPRoleServiceConfig theConfig) {
        this.config = theConfig;
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 1L;

                            public Component buildComponent(String id) {

                                return current =
                                        new LDAPRoleServicePanel(
                                                id, new Model<LDAPRoleServiceConfig>(config));
                            };
                        },
                        new CompoundPropertyModel<Object>(config)) {

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
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(false, true);
        checkBaseConfig();

        assertNull(
                tester.getComponentFromLastRenderedPage(
                        "form:panel:authenticationPanelContainer:authenticationPanel:user"));
        assertNull(
                tester.getComponentFromLastRenderedPage(
                        "form:panel:authenticationPanelContainer:authenticationPanel:password"));
    }

    @Test
    public void testRequiredFields() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(false, false);

        tester.newFormTester("form").submit();

        tester.assertErrorMessages(
                (Serializable[])
                        new String[] {
                            "Field 'Server URL' is required.",
                            "Field 'Group search base' is required."
                        });
    }

    @Test
    public void testDataLoadedFromConfigurationWithAuthentication() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(true, true);
        checkBaseConfig();

        tester.assertModelValue(
                "form:panel:authenticationPanelContainer:authenticationPanel:user", AUTH_USER);
        tester.assertModelValue(
                "form:panel:authenticationPanelContainer:authenticationPanel:password",
                AUTH_PASSWORD);
    }

    @Test
    public void testAuthenticationDisabled() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(false, true);
        tester.assertInvisible("form:panel:authenticationPanelContainer:authenticationPanel");
        tester.newFormTester("form").setValue("panel:bindBeforeGroupSearch", "on");
        tester.executeAjaxEvent("form:panel:bindBeforeGroupSearch", "click");
        tester.assertVisible("form:panel:authenticationPanelContainer:authenticationPanel");
    }

    @Test
    public void testAuthenticationEnabled() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(true, true);
        tester.assertVisible("form:panel:authenticationPanelContainer:authenticationPanel");
        tester.newFormTester("form").setValue("panel:bindBeforeGroupSearch", "");
        tester.executeAjaxEvent("form:panel:bindBeforeGroupSearch", "click");
        tester.assertInvisible("form:panel:authenticationPanelContainer:authenticationPanel");
    }

    private void checkBaseConfig() {
        tester.assertModelValue("form:panel:serverURL", ldapServerUrl + "/" + basePath);
        tester.assertModelValue("form:panel:groupSearchBase", GROUPS_BASE);
        tester.assertModelValue("form:panel:groupSearchFilter", GROUP_SEARCH_FILTER);
        tester.assertModelValue(
                "form:panel:allGroupsSearchFilter", config.getAllGroupsSearchFilter());
    }
}
