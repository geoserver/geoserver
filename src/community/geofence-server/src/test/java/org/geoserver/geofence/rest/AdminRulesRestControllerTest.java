/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.geoserver.geofence.GeofenceBaseTest;
import org.geoserver.geofence.core.dao.DuplicateKeyException;
import org.geoserver.geofence.core.model.AdminRule;
import org.geoserver.geofence.core.model.enums.AdminGrantType;
import org.geoserver.geofence.rest.xml.JaxbAdminRule;
import org.geoserver.geofence.rest.xml.JaxbAdminRuleList;
import org.geoserver.geofence.server.rest.AdminRulesRestController;
import org.geoserver.geofence.services.AdminRuleAdminService;
import org.geoserver.geofence.services.exception.NotFoundServiceEx;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AdminRulesRestControllerTest extends GeofenceBaseTest {

    protected AdminRulesRestController controller;

    protected AdminRuleAdminService adminService;

    @Before
    public void initGeoFenceControllers() {
        controller =
                (AdminRulesRestController) applicationContext.getBean("adminRulesRestController");
        adminService = (AdminRuleAdminService) applicationContext.getBean("adminRuleAdminService");
    }

    @Test
    public void testInsertUpdateDelete() {
        JaxbAdminRule rule = new JaxbAdminRule();
        rule.setPriority(5L);
        rule.setUserName("test_user");
        rule.setRoleName("test_role");
        rule.setWorkspace("workspace");
        rule.setAccess(AdminGrantType.ADMIN.name());

        long id = controller.insert(rule).getBody();

        AdminRule realRule = adminService.get(id);

        assertEquals(rule.getPriority().longValue(), realRule.getPriority());
        assertEquals(rule.getUserName(), realRule.getUsername());
        assertEquals(rule.getRoleName(), realRule.getRolename());
        assertEquals(rule.getWorkspace(), realRule.getWorkspace());
        assertEquals(rule.getAccess(), realRule.getAccess().name());

        // only update the role
        JaxbAdminRule ruleMods = new JaxbAdminRule();
        ruleMods.setRoleName("acrobaat");

        controller.update(id, ruleMods);

        realRule = adminService.get(id);

        assertEquals(rule.getUserName(), realRule.getUsername());
        assertEquals(ruleMods.getRoleName(), realRule.getRolename());

        // insert another rule with same priority
        JaxbAdminRule rule2 = new JaxbAdminRule();
        rule2.setPriority(5L);
        rule2.setAccess(AdminGrantType.USER.name());
        long id2 = controller.insert(rule2).getBody();

        realRule = adminService.get(id);
        assertEquals(6L, realRule.getPriority());

        // test changing to non-existing priority

        JaxbAdminRule rule2Mods = new JaxbAdminRule();
        rule2Mods.setPriority(3L);
        controller.update(id2, rule2Mods);

        realRule = adminService.get(id2);
        assertEquals(3L, realRule.getPriority());

        // test changing to existing priority

        rule2Mods = new JaxbAdminRule();
        rule2Mods.setPriority(6L);
        controller.update(id2, rule2Mods);

        realRule = adminService.get(id2);
        assertEquals(6L, realRule.getPriority());
        realRule = adminService.get(id);
        assertEquals(7L, realRule.getPriority());

        // not found - will be translated by spring exception handler to code 404
        controller.delete(id);
        boolean notfound = false;
        try {
            adminService.get(id);
        } catch (NotFoundServiceEx e) {
            notfound = true;
        }
        assertTrue(notfound);

        // conflict - will be translated by spring exception handler to code 409
        boolean conflict = false;
        try {
            controller.insert(rule2);
        } catch (DuplicateKeyException e) {
            conflict = true;
        }
        assertTrue(conflict);
    }

    @Test
    public void testMovingRules() {
        // create some rules for the test
        String prefix = UUID.randomUUID().toString();
        adminService.insert(
                new AdminRule(
                        5,
                        prefix + "-user5",
                        prefix + "-role1",
                        null,
                        null,
                        null,
                        AdminGrantType.ADMIN));
        adminService.insert(
                new AdminRule(
                        2,
                        prefix + "-user2",
                        prefix + "-role1",
                        null,
                        null,
                        null,
                        AdminGrantType.ADMIN));
        adminService.insert(
                new AdminRule(
                        1,
                        prefix + "-user1",
                        prefix + "-role1",
                        null,
                        null,
                        null,
                        AdminGrantType.ADMIN));
        adminService.insert(
                new AdminRule(
                        4,
                        prefix + "-user4",
                        prefix + "-role2",
                        null,
                        null,
                        null,
                        AdminGrantType.ADMIN));
        adminService.insert(
                new AdminRule(
                        3,
                        prefix + "-user3",
                        prefix + "-role2",
                        null,
                        null,
                        null,
                        AdminGrantType.ADMIN));
        adminService.insert(
                new AdminRule(
                        6,
                        prefix + "-user6",
                        prefix + "-role6",
                        null,
                        null,
                        null,
                        AdminGrantType.ADMIN));
        // get the rules so we can access their id
        JaxbAdminRuleList originalRules =
                controller.get(0, 6, false, null, null, null, null, null, null);
        validateRules(originalRules, prefix, "user1", "user2", "user3", "user4", "user5", "user6");
        // check rules per page
        validateRules(0, prefix, "user1", "user2");
        validateRules(0, 1, 2);
        validateRules(1, prefix, "user3", "user4");
        validateRules(1, 3, 4);
        validateRules(2, prefix, "user5", "user6");
        validateRules(2, 5, 6);
        // moving rules for user1 and user2 to the last page
        ResponseEntity<JaxbAdminRuleList> result =
                controller.move(
                        7,
                        originalRules.getRules().get(0).getId()
                                + ","
                                + originalRules.getRules().get(1).getId());
        validateResult(result, HttpStatus.OK, 2);
        validateRules(result.getBody(), prefix, "user1", "user2");
        validateRules(result.getBody(), 7L, 8L);
        // check rules per page
        validateRules(0, prefix, "user3", "user4");
        validateRules(0, 3, 4);
        validateRules(1, prefix, "user5", "user6");
        validateRules(1, 5, 6);
        validateRules(2, prefix, "user1", "user2");
        validateRules(2, 7, 8);
        // moving rules for user3 and user4 to the second page
        result =
                controller.move(
                        7,
                        originalRules.getRules().get(2).getId()
                                + ","
                                + originalRules.getRules().get(3).getId());
        validateResult(result, HttpStatus.OK, 2);
        validateRules(result.getBody(), prefix, "user3", "user4");
        validateRules(result.getBody(), 7L, 8L);
        // check rules per page
        validateRules(0, prefix, "user5", "user6");
        validateRules(0, 5, 6);
        validateRules(1, prefix, "user3", "user4");
        validateRules(1, 7, 8);
        validateRules(2, prefix, "user1", "user2");
        validateRules(2, 9, 10);
        // moving rule for user1 to first page
        result = controller.move(5, String.valueOf(originalRules.getRules().get(0).getId()));
        validateResult(result, HttpStatus.OK, 1);
        validateRules(result.getBody(), prefix, "user1");
        validateRules(result.getBody(), 5L);
        // check rules per page
        validateRules(0, prefix, "user1", "user5");
        validateRules(0, 5, 6);
        validateRules(1, prefix, "user6", "user3");
        validateRules(1, 7, 8);
        validateRules(2, prefix, "user4", "user2");
        validateRules(2, 9, 11);
        // moving rules for user2 and user 3 to first and second page
        result =
                controller.move(
                        6,
                        originalRules.getRules().get(1).getId()
                                + ","
                                + originalRules.getRules().get(2).getId());
        validateResult(result, HttpStatus.OK, 2);
        validateRules(result.getBody(), prefix, "user3", "user2");
        validateRules(result.getBody(), 6L, 7L);
        // check rules per page
        validateRules(0, prefix, "user1", "user3");
        validateRules(0, 5, 6);
        validateRules(1, prefix, "user2", "user5");
        validateRules(1, 7, 8);
        validateRules(2, prefix, "user6", "user4");
        validateRules(2, 9, 11);
    }

    /** Helper method that will validate a move result. */
    private void validateResult(
            ResponseEntity<JaxbAdminRuleList> result, HttpStatus expectedHttpStatus, int rules) {
        assertThat(result, notNullValue());
        assertThat(result.getStatusCode(), is(expectedHttpStatus));
        if (rules > 0) {
            assertThat(result.getBody(), notNullValue());
            assertThat(result.getBody().getRules().size(), is(rules));
        } else {
            assertThat(result.getBody(), nullValue());
        }
    }

    /**
     * Helper method that will validate the rules present in a certain page based on the user id.
     */
    private void validateRules(int page, String prefix, String... expectedUsers) {
        JaxbAdminRuleList rules =
                controller.get(page, 2, false, null, null, null, null, null, null);
        validateRules(rules, prefix, expectedUsers);
    }

    /**
     * Helper method that will validate that the provided rules will match the provided user ids.
     */
    private void validateRules(JaxbAdminRuleList rules, String prefix, String... expectedUsers) {
        assertThat(rules, notNullValue());
        assertThat(rules.getRules(), notNullValue());
        assertThat(rules.getRules().size(), is(expectedUsers.length));
        for (int i = 0; i < expectedUsers.length; i++) {
            assertThat(rules.getRules().get(i).getUserName(), is(prefix + "-" + expectedUsers[i]));
        }
    }

    /**
     * Helper method that will validate the rules present in a certain page based on the priority.
     */
    private void validateRules(int page, long... expectedPriorities) {
        JaxbAdminRuleList rules =
                controller.get(page, 2, false, null, null, null, null, null, null);
        validateRules(rules, expectedPriorities);
    }

    /**
     * Helper method that will validate that the provided rules will match the provided priorities.
     */
    private void validateRules(JaxbAdminRuleList rules, long... expectedPriorities) {
        assertThat(rules, notNullValue());
        assertThat(rules.getRules(), notNullValue());
        assertThat(rules.getRules().size(), is(expectedPriorities.length));
        for (int i = 0; i < expectedPriorities.length; i++) {
            assertThat(rules.getRules().get(i).getPriority(), is(expectedPriorities[i]));
        }
    }
}
