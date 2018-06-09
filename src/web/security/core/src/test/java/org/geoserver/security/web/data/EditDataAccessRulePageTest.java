/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import static org.junit.Assert.*;

import java.util.List;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.MockData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.role.NewRolePage;
import org.geoserver.test.RunTestSetup;
import org.junit.Before;
import org.junit.Test;

public class EditDataAccessRulePageTest extends AbstractSecurityWicketTestSupport {

    EditDataAccessRulePage page;
    String ruleName =
            MockData.CITE_PREFIX
                    + "."
                    + MockData.LAKES.getLocalPart()
                    + "."
                    + AccessMode.WRITE.getAlias();

    @Before
    public void init() throws Exception {
        initializeForXML();
        clearServices();
        DataAccessRuleDAO.get().clear();
    }

    @Test
    public void testFill() throws Exception {

        tester.startPage(page = new EditDataAccessRulePage(getRule(ruleName)));
        tester.assertRenderedPage(EditDataAccessRulePage.class);

        tester.assertModelValue("form:root", MockData.CITE_PREFIX);
        tester.assertModelValue(
                "form:layerContainer:layerAndLabel:layer", MockData.LAKES.getLocalPart());
        tester.assertModelValue("form:accessMode", AccessMode.WRITE);

        // Does not work with Palette
        // tester.assertModelValue("form:roles:roles:recorder", { ROLE_WMS,ROLE_WFS });

        tester.assertModelValue("form:roles:anyRole", Boolean.FALSE);
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("roles:anyRole", true);

        // open new role dialog again to ensure that the current state is not lost
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(EditDataAccessRulePage.class);

        form = tester.newFormTester("form");
        form.setValue("roles:anyRole", true);
        form.submit("save");

        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(DataSecurityPage.class);

        DataAccessRule rule = getRule(ruleName);
        assertNotNull(rule);
        assertEquals(1, rule.getRoles().size());
        assertEquals(GeoServerRole.ANY_ROLE, rule.getRoles().iterator().next());
    }

    @Test
    @RunTestSetup
    public void testEmptyRoles() throws Exception {
        // initializeForXML();
        initializeServiceRules();
        tester.startPage(page = new EditDataAccessRulePage(getRule(ruleName)));

        FormTester form = tester.newFormTester("form");
        form.setValue("roles:palette:recorder", "");

        form.submit("save");
        tester.assertRenderedPage(EditDataAccessRulePage.class);
        // print(tester.getLastRenderedPage(),true,true);
        assertTrue(testErrorMessagesWithRegExp(".*no role.*"));
        tester.assertRenderedPage(EditDataAccessRulePage.class);
    }

    @Test
    public void testReadOnlyRoleService() throws Exception {
        // initializeForXML();
        activateRORoleService();
        tester.startPage(page = new EditDataAccessRulePage(getRule(ruleName)));
        tester.assertInvisible("form:roles:addRole");
    }

    protected int indexOf(List<? extends String> strings, String searchValue) {
        int index = 0;
        for (String s : strings) {
            if (s.equals(searchValue)) return index;
            index++;
        }
        assertTrue(index != -1);
        return -1;
    }

    DataAccessRule getRule(String key) {
        DataAccessRuleDAO.get().reload();
        for (DataAccessRule rule : DataAccessRuleDAO.get().getRules()) {
            if (key.equals(rule.getKey())) {
                return rule;
            }
        }
        return null;
    }
}
