/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import static org.junit.Assert.*;

import java.util.List;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.role.NewRolePage;
import org.junit.Test;

public class EditServiceAccessRulePageTest extends AbstractSecurityWicketTestSupport {

    EditServiceAccessRulePage page;

    @Test
    public void testFill() throws Exception {

        initializeForXML();
        // insertValues();
        tester.startPage(page = new EditServiceAccessRulePage(getRule("wms.GetMap")));
        tester.assertRenderedPage(EditServiceAccessRulePage.class);

        tester.assertModelValue("form:service", "wms");
        tester.assertModelValue("form:method", "GetMap");

        // Does not work with Palette
        // tester.assertModelValue("form:roles:roles:recorder","ROLE_AUTHENTICATED");

        tester.assertModelValue("form:roles:anyRole", Boolean.FALSE);
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("roles:anyRole", true);

        // open new role dialog again to ensure that the current state is not lost
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(EditServiceAccessRulePage.class);

        form = tester.newFormTester("form");
        form.submit("save");

        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(ServiceAccessRulePage.class);

        ServiceAccessRule rule = getRule("wms.GetMap");
        assertNotNull(rule);
        assertEquals(1, rule.getRoles().size());
        assertEquals(GeoServerRole.ANY_ROLE, rule.getRoles().iterator().next());
    }

    @Test
    public void testEmptyRoles() throws Exception {
        initializeForXML();
        initializeServiceRules();
        tester.startPage(page = new EditServiceAccessRulePage(getRule("wms.GetMap")));

        FormTester form = tester.newFormTester("form");
        form.setValue("roles:palette:recorder", "");
        form.submit("save");

        // print(tester.getLastRenderedPage(),true,true);
        assertTrue(testErrorMessagesWithRegExp(".*no role.*"));
        tester.assertRenderedPage(EditServiceAccessRulePage.class);
    }

    @Test
    public void testReadOnlyRoleService() throws Exception {
        initializeForXML();
        activateRORoleService();
        tester.startPage(page = new EditServiceAccessRulePage(getRule("wms.GetMap")));
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

    ServiceAccessRule getRule(String key) {
        for (ServiceAccessRule rule : ServiceAccessRuleDAO.get().getRules()) {
            if (key.equals(rule.getKey())) {
                return rule;
            }
        }
        return null;
    }
}
