/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.CreateLdapServerRule;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.MapModel;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.ldap.LDAPSecurityServiceConfig;
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
public class LDAPAuthProviderPanelTest extends AbstractSecurityWicketTestSupport {

    private static final String USER_FORMAT = "uid={0},ou=People,dc=example,dc=com";

    private static final String USER_FILTER = "(telephonenumber=1)";

    private static final String USER_DN_PATTERN = "uid={0},ou=People";

    LDAPAuthProviderPanel current;

    String relBase = "panel:";
    String base = "form:" + relBase;

    LDAPSecurityServiceConfig config;

    FeedbackPanel feedbackPanel = null;

    private static final String ldapServerUrl = LDAPTestUtils.LDAP_SERVER_URL;
    private static final String basePath = LDAPTestUtils.LDAP_BASE_PATH;

    @ClassRule public static CreateLdapServerRule serverRule = new CreateLdapServerRule();

    @After
    public void tearDown() throws Exception {}

    protected void setupPanel(
            final String userDnPattern,
            String userFilter,
            String userFormat,
            String userGroupService) {
        config = new LDAPSecurityServiceConfig();
        config.setName("test");
        config.setServerURL(ldapServerUrl + "/" + basePath);
        config.setUserDnPattern(userDnPattern);
        config.setUserFilter(userFilter);
        config.setUserFormat(userFormat);
        config.setUserGroupServiceName(userGroupService);
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

    protected void setupPanel(LDAPSecurityServiceConfig theConfig) {
        this.config = theConfig;
        tester.startPage(
                new LDAPFormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 7319919840443122283L;

                            public Component buildComponent(String id) {

                                return current =
                                        new LDAPAuthProviderPanel(
                                                id, new Model<LDAPSecurityServiceConfig>(config));
                            };
                        },
                        new CompoundPropertyModel<Object>(config)));
    }

    @Test
    public void testTestConnectionWithDnLookup() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(USER_DN_PATTERN, null, null, null);
        testSuccessfulConnection();
    }

    @Test
    public void testTestConnectionWitUserGroupService() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(USER_DN_PATTERN, null, null, "default");
        testSuccessfulConnection();
    }

    @Test
    public void testTestConnectionWithUserFilter() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(null, USER_FILTER, USER_FORMAT, null);
        testSuccessfulConnection();
    }

    @Test
    public void testTestConnectionFailedWithDnLookup() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(USER_DN_PATTERN, null, null, null);
        testFailedConnection();
    }

    @Test
    public void testTestConnectionFailedWithUserFilter() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(null, USER_FILTER, USER_FORMAT, null);
        testFailedConnection();
    }

    private void testSuccessfulConnection() throws Exception {
        authenticate("admin", "admin");

        tester.assertNoErrorMessage();
        String success =
                new StringResourceModel(
                                LDAPAuthProviderPanel.class.getSimpleName()
                                        + ".connectionSuccessful")
                        .getObject();
        tester.assertInfoMessages((Serializable[]) new String[] {success});
    }

    private void testFailedConnection() throws Exception {
        authenticate("admin", "wrong");

        tester.assertNoInfoMessage();
        tester.assertContains("AuthenticationException");
    }

    private void authenticate(String username, String password) {
        TextField<?> userField =
                ((TextField<?>) tester.getComponentFromLastRenderedPage(base + "testCx:username"));
        userField.setDefaultModel(new Model<String>(username));
        TextField<?> passwordField =
                ((TextField<?>) tester.getComponentFromLastRenderedPage(base + "testCx:password"));
        passwordField.setDefaultModel(new Model<String>(password));

        Map<String, String> map = new HashMap<String, String>();
        map.put("username", username);
        map.put("password", password);

        tester.getComponentFromLastRenderedPage("form:panel:testCx")
                .setDefaultModel(new MapModel<String, String>(map));

        tester.clickLink(base + "testCx:test", true);
    }

    private class LDAPFormTestPage extends FormTestPage {
        public LDAPFormTestPage(ComponentBuilder builder, CompoundPropertyModel<Object> model) {
            super(builder, model);
        }

        private static final long serialVersionUID = 3150973967583096118L;

        @Override
        protected void onBeforeRender() {
            feedbackPanel = new FeedbackPanel("topFeedback");
            feedbackPanel.setOutputMarkupId(true);
            addOrReplace(feedbackPanel);
            super.onBeforeRender();
        }
    }
}
