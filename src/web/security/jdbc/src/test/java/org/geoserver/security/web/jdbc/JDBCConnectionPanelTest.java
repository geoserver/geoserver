/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc;

import static org.junit.Assert.*;

import java.util.Arrays;
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
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 1L;

                            public Component buildComponent(String id) {
                                return current = new JDBCConnectionPanel(id, new Model(config));
                            };
                        },
                        new CompoundPropertyModel(config)));
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
        theConfig.setConnectURL("jdbc:foo");

        setupPanel(theConfig);
        tester.assertRenderedPage(FormTestPage.class);

        tester.clickLink("form:panel:cxTest", true);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
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
            for (String c :
                    Arrays.asList(
                            new String[] {
                                "driverClassName", "connectURL", "userName", "password"
                            })) {
                tester.assertComponent(base + c, FormComponent.class);
                tester.assertVisible(base + c);
            }
        }
    }
}
