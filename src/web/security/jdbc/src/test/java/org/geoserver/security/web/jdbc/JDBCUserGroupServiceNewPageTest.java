/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.UserGroupRoleServicesPage;
import org.geoserver.security.web.usergroup.UserGroupServicesPanel;
import org.junit.Test;

public class JDBCUserGroupServiceNewPageTest extends AbstractSecurityNamedServicePanelTest {

    private static final String CX = "panel:content:cx:cxPanelContainer:cxPanel:";

    @Override
    protected String getDetailsFormComponentId() {
        return "UserGroupTabbedPage:panel:namedConfig";
    }

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new UserGroupRoleServicesPage();
    }

    @Override
    protected String getBasePanelId() {
        return "panel:panel:userGroupServices";
    }

    @Override
    protected Integer getTabIndex() {
        return 0;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return UserGroupServicesPanel.class;
    }

    @Test
    public void testSaveUnknownParameter() throws Exception {
        List<Serializable> errors = save("jdbc:hsqldb:mem:x;textdb.allow_full_path=true", null);

        assertEquals(
                List.of("The JDBC URL is invalid, parameter textdb.allow_full_path has not been recognized for driver"
                        + " org.hsqldb.jdbc.JDBCDriver. Add it with GEOSERVER_JDBC_EXTRA_PARAMETERS if it is needed"),
                errors);
    }

    /** DDL_FILE_INVALID has no user interface key, so the message falls back to the exception text. */
    @Test
    public void testSaveFallbackMessage() throws Exception {
        List<Serializable> errors = save("jdbc:hsqldb:mem:x", "/tmp/ddl{0}.xml");

        assertEquals(List.of("Cannot open DDL file /tmp/ddl{0}.xml"), errors);
    }

    /** Fills a new JDBC user/group service with the HSQLDB driver, saves it, and returns the error messages. */
    private List<Serializable> save(String url, String ddlFile) throws Exception {
        initializeForXML();
        activatePanel();
        clickAddNew();
        setSecurityConfigClassName(JDBCUserGroupServicePanelInfo.class);

        newFormTester();
        setSecurityConfigName("jdbcService");
        formTester.setValue(
                "panel:content:passwordEncoderName",
                getPlainTextPasswordEncoder().getName());
        formTester.setValue("panel:content:passwordPolicyName", "default");
        formTester.setValue(CX + "driverClassName", "org.hsqldb.jdbc.JDBCDriver");
        formTester.setValue(CX + "connectURL", url);
        formTester.setValue(CX + "userName", "sa");
        if (ddlFile != null) {
            formTester.setValue("panel:content:creatingTables", true);
            formTester.setValue("panel:content:propertyFileNameDDL", ddlFile);
        }
        clickSave();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        return tester.getMessages(FeedbackMessage.ERROR);
    }
}
