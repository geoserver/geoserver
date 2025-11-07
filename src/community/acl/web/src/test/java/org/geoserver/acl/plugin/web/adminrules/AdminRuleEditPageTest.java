/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.adminrules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.acl.domain.adminrules.AdminGrantType;
import org.geoserver.acl.domain.adminrules.AdminRule;
import org.geoserver.acl.plugin.web.adminrules.model.AdminRuleEditModel;
import org.geoserver.acl.plugin.web.adminrules.model.MutableAdminRule;
import org.geoserver.acl.plugin.web.support.AclWicketTestSupport;
import org.junit.Test;
import org.springframework.util.SerializationUtils;

public class AdminRuleEditPageTest extends AclWicketTestSupport {

    @Test
    public void testSecuredPage() {
        logout();
        assertThrows(
                RestartResponseException.class,
                () -> tester.startPage(new AdminRuleEditPage(new AdminRuleEditModel())));
    }

    @Test
    public void testNewRule() {
        AdminRuleEditModel pageModel = new AdminRuleEditModel();
        @SuppressWarnings("unused")
        AdminRuleEditPage page = tester.startPage(new AdminRuleEditPage(pageModel));
        tester.assertRenderedPage(AdminRuleEditPage.class);
        //		print(page, true, true);

        FormTester ft = tester.newFormTester("form");

        ft.setValue("roleName", "ROLE_ADMINISTRATOR");
        ft.setValue("userName", "admin");
        ft.setValue("workspace", "testws");
        ft.select("access", 1);

        ft.submit("save");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(AdminRulesACLPage.class);

        MutableAdminRule modelObject = pageModel.getModel().getObject();
        assertNotNull(modelObject.getId());
        assertEquals(1, modelObject.getPriority());
        assertEquals("ROLE_ADMINISTRATOR", modelObject.getRoleName());
        assertEquals("admin", modelObject.getUserName());
        assertEquals("testws", modelObject.getWorkspace());
        assertEquals(AdminGrantType.ADMIN, modelObject.getAccess());

        AdminRule expected = modelObject.toRule();
        AdminRule actual = pageModel.loadDomainRule();
        assertEquals(expected, actual);
    }

    @Test
    public void testEditRule() {
        final AdminRule userRule = adminService().insert(AdminRule.user().withPriority(10));
        adminService().insert(AdminRule.admin().withPriority(11).withRolename("ROLE_ADMINISTRATOR"));

        AdminRuleEditModel pageModel = new AdminRuleEditModel(new MutableAdminRule(userRule));

        @SuppressWarnings("unused")
        AdminRuleEditPage page = tester.startPage(new AdminRuleEditPage(pageModel));
        tester.assertRenderedPage(AdminRuleEditPage.class);
        // print(page, true, true);

        FormTester ft = tester.newFormTester("form");

        ft.setValue("priority", "1");
        ft.setValue("roleName", "ROLE_EDITOR");
        ft.setValue("userName", "John");
        ft.setValue("workspace", "testws");
        ft.select("access", 0); // 0==USER, 1==ADMIN

        ft.submit("save");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(AdminRulesACLPage.class);

        MutableAdminRule modelObject = pageModel.getModel().getObject();
        assertEquals(userRule.getId(), modelObject.getId());
        assertEquals(1, modelObject.getPriority());
        assertEquals("ROLE_EDITOR", modelObject.getRoleName());
        assertEquals("John", modelObject.getUserName());
        assertEquals("testws", modelObject.getWorkspace());
        assertEquals(AdminGrantType.USER, modelObject.getAccess());

        AdminRule expected = modelObject.toRule();
        AdminRule actual = pageModel.loadDomainRule();
        assertEquals(expected, actual);
    }

    @Test
    public void testSerializable() {

        final AdminRule userRule = adminService().insert(AdminRule.user().withPriority(10));
        adminService().insert(AdminRule.admin().withPriority(11).withRolename("ROLE_ADMINISTRATOR"));

        AdminRuleEditModel pageModel = new AdminRuleEditModel(new MutableAdminRule(userRule));

        AdminRuleEditPage page = tester.startPage(new AdminRuleEditPage(pageModel));

        byte[] serialized = SerializationUtils.serialize(page);
        @SuppressWarnings("deprecation")
        AdminRuleEditPage deserialized = (AdminRuleEditPage) SerializationUtils.deserialize(serialized);
        assertNotNull(deserialized);
    }
}
