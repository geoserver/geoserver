/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Test;

public class JDBCConnectionPanelTest extends AbstractSecurityWicketTestSupport {

    JDBCConnectionPanel<JDBCSecurityServiceConfig> current;

    String relBase = "panel:cxPanelContainer:cxPanel:";
    String base = "form:" + relBase;

    JDBCSecurityServiceConfig config;

    protected void setupPanel(final boolean jndi) {
        config = new JDBCUserGroupServiceConfig();
        config.setJndi(jndi);
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

    protected void setupPanel(JDBCSecurityServiceConfig theConfig) {
        this.config = theConfig;
        tester.startPage(new FormTestPage(
                new ComponentBuilder() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Component buildComponent(String id) {
                        return current = new JDBCConnectionPanel<>(id, new Model<>(config));
                    }
                },
                new CompoundPropertyModel<>(config)));
    }

    @Test
    public void testJNDI() throws Exception {
        setupPanel(true);
        tester.assertRenderedPage(FormTestPage.class);
        assertTrue(config.isJndi());

        assertVisibility(true);

        FormTester ftester = tester.newFormTester("form");
        ftester.setValue(relBase + "jndiName", "jndiurl");
        ftester.submit();

        tester.assertNoErrorMessage();
        assertEquals("jndiurl", config.getJndiName());
    }

    @Test
    public void testConnectionTestJNDI() throws Exception {
        JDBCUserGroupServiceConfig theConfig = new JDBCUserGroupServiceConfig();
        theConfig.setJndi(true);
        theConfig.setJndiName("jndiurl");

        setupPanel(theConfig);
        tester.assertRenderedPage(FormTestPage.class);
        tester.clickLink("form:panel:cxTest", true);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
    }

    @Test
    public void testBasic() throws Exception {
        setupPanel(false);
        tester.assertRenderedPage(FormTestPage.class);
        assertFalse(config.isJndi());

        assertVisibility(false);

        FormTester ftester = tester.newFormTester("form");
        ftester.setValue(relBase + "userName", "user1");
        ftester.setValue(relBase + "password", "pw");
        ftester.setValue(relBase + "driverClassName", "org.h2.Driver");
        ftester.setValue(relBase + "connectURL", "jdbc:h2");
        ftester.submit();

        tester.assertNoErrorMessage();
        assertEquals("user1", config.getUserName());
        assertEquals("pw", config.getPassword());
        assertEquals("org.h2.Driver", config.getDriverClassName());
        assertEquals("jdbc:h2", config.getConnectURL());
    }

    @Test
    public void testConncetionTestBasic() throws Exception {
        JDBCUserGroupServiceConfig theConfig = new JDBCUserGroupServiceConfig();
        theConfig.setUserName("user1");
        theConfig.setPassword("pw");
        theConfig.setDriverClassName("org.h2.Driver");
        // passes the URL check, then fails to connect
        theConfig.setConnectURL("jdbc:h2:tcp://localhost:1/db");

        setupPanel(theConfig);
        tester.assertRenderedPage(FormTestPage.class);

        tester.clickLink("form:panel:cxTest", true);
        List<Serializable> errors = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, errors.size());
        assertThat(errors.get(0).toString(), startsWith("org.h2.jdbc.JdbcSQLException: Connection is broken"));
    }

    @Test
    public void testConnectionTestEmptyUrl() throws Exception {
        JDBCUserGroupServiceConfig theConfig = new JDBCUserGroupServiceConfig();
        theConfig.setUserName("sa");
        theConfig.setDriverClassName("org.h2.Driver");

        setupPanel(theConfig);
        tester.clickLink("form:panel:cxTest", true);

        // Wicket reports the required field, the connection test adds no second message
        List<Serializable> errors = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, errors.size());
        assertEquals("Field 'Connection URL' is required.", errors.get(0).toString());
    }

    @Test
    public void testConnectionTestUnknownParameter() throws Exception {
        JDBCUserGroupServiceConfig theConfig = new JDBCUserGroupServiceConfig();
        theConfig.setUserName("sa");
        theConfig.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        theConfig.setConnectURL("jdbc:hsqldb:mem:x;textdb.allow_full_path=true");

        setupPanel(theConfig);
        tester.clickLink("form:panel:cxTest", true);

        List<Serializable> errors = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(
                List.of("The JDBC URL is invalid, parameter textdb.allow_full_path has not been recognized for driver"
                        + " org.hsqldb.jdbc.JDBCDriver. Add it with GEOSERVER_JDBC_EXTRA_PARAMETERS if it is needed"),
                errors);
    }

    @Test
    public void testConnectionTestBasicOK() throws Exception {
        JDBCUserGroupServiceConfig theConfig = new JDBCUserGroupServiceConfig();
        theConfig.setUserName("user1");
        theConfig.setPassword("pw");
        theConfig.setDriverClassName("org.h2.Driver");
        theConfig.setConnectURL("jdbc:h2:file:target/db");

        setupPanel(theConfig);
        tester.assertRenderedPage(FormTestPage.class);
        tester.clickLink("form:panel:cxTest", true);
        assertEquals(1, tester.getMessages(FeedbackMessage.INFO).size());
    }

    protected void assertVisibility(boolean jndi) {
        if (jndi) {
            tester.assertComponent(base + "jndiName", TextField.class);
            tester.assertVisible(base + "jndiName");
        } else {
            for (String c : Arrays.asList(new String[] {"driverClassName", "connectURL", "userName", "password"})) {
                tester.assertComponent(base + c, FormComponent.class);
                tester.assertVisible(base + c);
            }
        }
    }
}
