/* (c) 2015 - 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest;

import org.geoserver.geofence.GeofenceBaseTest;
import org.geoserver.geofence.core.dao.DuplicateKeyException;
import org.geoserver.geofence.core.model.LayerAttribute;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.rest.xml.JaxbRule;
import org.geoserver.geofence.rest.xml.JaxbRuleList;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.exception.NotFoundServiceEx;
import org.geotools.gml3.bindings.GML3MockData;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class RulesRestControllerTest extends GeofenceBaseTest {

    protected RulesRestController controller;

    protected RuleAdminService adminService;

    @Override
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        controller = (RulesRestController) applicationContext.getBean("rulesRestController");
        adminService = (RuleAdminService) applicationContext.getBean("ruleAdminService");
    }

    @Test
    public void testInsertUpdateDelete() {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(5L);
        rule.setUserName("pipo");
        rule.setRoleName("clown");
        rule.setAddressRange("127.0.0.1/32");
        rule.setService("wfs");
        rule.setRequest("getFeature");
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("ALLOW");

        long id = controller.insert(rule).getBody();

        Rule realRule = adminService.get(id);

        assertEquals(rule.getPriority().longValue(), realRule.getPriority());
        assertEquals(rule.getUserName(), realRule.getUsername());
        assertEquals(rule.getRoleName(), realRule.getRolename());
        assertEquals(rule.getAddressRange(), realRule.getAddressRange().getCidrSignature());
        assertEquals(rule.getService().toUpperCase(), realRule.getService().toUpperCase());
        assertEquals(rule.getRequest().toUpperCase(), realRule.getRequest().toUpperCase());
        assertEquals(rule.getWorkspace(), realRule.getWorkspace());
        assertEquals(rule.getLayer(), realRule.getLayer());
        assertEquals(rule.getAccess(), realRule.getAccess().toString());

        JaxbRule ruleMods = new JaxbRule();
        ruleMods.setRoleName("acrobaat");

        controller.update(id, ruleMods);

        realRule = adminService.get(id);

        assertEquals(rule.getUserName(), realRule.getUsername());
        assertEquals(ruleMods.getRoleName(), realRule.getRolename());
        
        JaxbRule rule2 = new JaxbRule();
        rule2.setPriority(5L);
        rule2.setAccess("DENY");
        long id2 = controller.insert(rule2).getBody();
        
        realRule = adminService.get(id);
        assertEquals(6L, realRule.getPriority());
        
        //test changing to non-existing priority
        
        JaxbRule rule2Mods = new JaxbRule();
        rule2Mods.setPriority(3L);
        controller.update(id2, rule2Mods);
        
        realRule = adminService.get(id2);
        assertEquals(3L, realRule.getPriority());
        
        //test changing to existing priority
        
        rule2Mods = new JaxbRule();
        rule2Mods.setPriority(6L);
        controller.update(id2, rule2Mods);
        
        realRule = adminService.get(id2);
        assertEquals(6L, realRule.getPriority());
        realRule = adminService.get(id);
        assertEquals(7L, realRule.getPriority());

        //not found - will be translated by spring exception handler to code 404
        controller.delete(id);
        boolean notfound = false;
        try {
            adminService.get(id);
        } catch (NotFoundServiceEx e) {
            notfound = true;
        }
        assertTrue(notfound);
        
        //conflict - will be translated by spring exception handler to code 409
        boolean conflict = false;
        try {
        	controller.insert(rule2);
        } catch (DuplicateKeyException e) {
        	conflict = true;
        }
        assertTrue(conflict);
    }
    
    @Test
    public void testLimits() {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(5L);
        rule.setUserName("pipo");
        rule.setRoleName("clown");
        rule.setAddressRange("127.0.0.1/32");
        rule.setService("wfs");
        rule.setRequest("getFeature");
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("LIMIT");
        rule.setLimits(new JaxbRule.Limits());
        rule.getLimits().setAllowedArea(GML3MockData.multiPolygon());
        rule.getLimits().setCatalogMode("MIXED");
        
        Long id = controller.insert(rule).getBody();
        
        Rule realRule = adminService.get(id);

        assertEquals(rule.getLimits().getCatalogMode(), realRule.getRuleLimits().getCatalogMode().toString());
        assertEquals(rule.getLimits().getAllowedArea(), realRule.getRuleLimits().getAllowedArea());
        
        rule.getLimits().setCatalogMode("HIDE");
        
        controller.update(id, rule);
        
        realRule = adminService.get(id);
        
        assertEquals(rule.getLimits().getCatalogMode(), realRule.getRuleLimits().getCatalogMode().toString());
        
        rule.setLimits(null);
                
        controller.clearAndUpdate(id, rule);
        
        realRule = adminService.get(id);
        
        assertNull(realRule.getRuleLimits());
    }
    
    @Test
    public void testLayerDetails() {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(5L);
        rule.setUserName("pipo");
        rule.setRoleName("clown");
        rule.setAddressRange("127.0.0.1/32");
        rule.setService("wfs");
        rule.setRequest("getFeature");
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("ALLOW");
        rule.setLayerDetails(new JaxbRule.LayerDetails());
        rule.getLayerDetails().setAllowedArea(GML3MockData.multiPolygon());
        rule.getLayerDetails().getAllowedStyles().add("style1");
        rule.getLayerDetails().getAllowedStyles().add("style2");
        JaxbRule.LayerAttribute att = new JaxbRule.LayerAttribute();
        att.setName("layerAttribute1");
        att.setAccessType("READONLY");
        att.setDataType("dataType");
        rule.getLayerDetails().getAttributes().add(att);
        att = new JaxbRule.LayerAttribute();
        att.setName("layerAttribute2");
        att.setAccessType("READONLY");
        att.setDataType("dataType2");
        rule.getLayerDetails().getAttributes().add(att);
        rule.getLayerDetails().setCatalogMode("MIXED");
        rule.getLayerDetails().setCqlFilterRead("myFilterRead");
        rule.getLayerDetails().setCqlFilterWrite("myFilterWrite");
        rule.getLayerDetails().setDefaultStyle("myDefaultStyle");
        rule.getLayerDetails().setLayerType("VECTOR");
        
        Long id = controller.insert(rule).getBody();
        
        Rule realRule = adminService.get(id);

        assertEquals(rule.getLayerDetails().getAllowedArea(), realRule.getLayerDetails().getArea());
        assertEquals(rule.getLayerDetails().getCatalogMode(), realRule.getLayerDetails().getCatalogMode().toString());
        assertEquals(rule.getLayerDetails().getAllowedStyles(), realRule.getLayerDetails().getAllowedStyles());
        assertEquals(2, realRule.getLayerDetails().getAttributes().size());
        for (LayerAttribute la : realRule.getLayerDetails().getAttributes()) {
            if (la.getName().equals("layerAttribute2")) {
                assertEquals("READONLY", la.getAccess().toString());
            }
        }
        assertEquals(rule.getLayerDetails().getCqlFilterRead(), realRule.getLayerDetails().getCqlFilterRead());
        assertEquals(rule.getLayerDetails().getCqlFilterWrite(), realRule.getLayerDetails().getCqlFilterWrite());
        assertEquals(rule.getLayerDetails().getDefaultStyle(), realRule.getLayerDetails().getDefaultStyle());
        assertEquals(rule.getLayerDetails().getLayerType(), realRule.getLayerDetails().getType().toString());
        
        rule.getLayerDetails().setDefaultStyle("myDefaultStyle2");
        
        rule.getLayerDetails().getAttributes().clear();
        att = new JaxbRule.LayerAttribute();
        att.setName("layerAttribute2");
        att.setAccessType("READWRITE");
        att.setDataType("dataType");
        rule.getLayerDetails().getAttributes().add(att);
        att = new JaxbRule.LayerAttribute();
        att.setName("layerAttribute3");
        att.setAccessType("READWRITE");
        att.setDataType("dataType");
        rule.getLayerDetails().getAttributes().add(att);
        
        rule.getLayerDetails().getAllowedStyles().clear();
        rule.getLayerDetails().getAllowedStyles().add("style3");
        
        controller.update(id, rule);
        
        realRule = adminService.get(id);
        
        assertEquals(rule.getLayerDetails().getDefaultStyle(), realRule.getLayerDetails().getDefaultStyle());

        assertEquals(3, realRule.getLayerDetails().getAllowedStyles().size());
        assertEquals(3, realRule.getLayerDetails().getAttributes().size());
        
        for (LayerAttribute la : realRule.getLayerDetails().getAttributes()) {
            if (la.getName().equals("layerAttribute2")) {
                assertEquals("READWRITE", la.getAccess().toString());
            }
        }
                       
        controller.clearAndUpdate(id, rule);
                
        realRule = adminService.get(id);
        
        assertEquals(rule.getLayerDetails().getAllowedStyles(), realRule.getLayerDetails().getAllowedStyles());
        assertEquals(2, realRule.getLayerDetails().getAttributes().size());
    }

    @Test
    public void testMovingRules() {
        // create some rules for the test
        String prefix = UUID.randomUUID().toString();
        adminService.insert(new Rule(5, prefix + "-user5", prefix + "-role1", null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(2, prefix + "-user2", prefix + "-role1", null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(1, prefix + "-user1", prefix + "-role1", null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(4, prefix + "-user4", prefix + "-role2", null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(3, prefix + "-user3", prefix + "-role2", null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(6, prefix + "-user6", prefix + "-role6", null, null, null, null, null, null, GrantType.ALLOW));
        // get the rules so we can access their id
        JaxbRuleList originalRules = controller.get(0, 6, false, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        validateRules(originalRules, prefix, "user1", "user2", "user3", "user4", "user5", "user6");
        // check rules per page
        validateRules(0, prefix, "user1", "user2");
        validateRules(0, 1, 2);
        validateRules(1, prefix, "user3", "user4");
        validateRules(1, 3, 4);
        validateRules(2, prefix, "user5", "user6");
        validateRules(2, 5, 6);
        // moving rules for user1 and user2 to the last page
        ResponseEntity<JaxbRuleList> result = controller.move(7,
                originalRules.getRules().get(0).getId() + "," + originalRules.getRules().get(1).getId());
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
        result = controller.move(7,
                originalRules.getRules().get(2).getId() + "," + originalRules.getRules().get(3).getId());
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
        result = controller.move(6, originalRules.getRules().get(1).getId() + "," + originalRules.getRules().get(2).getId());
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

    /**
     * Helper method that will validate a move result.
     */
    private void validateResult(ResponseEntity<JaxbRuleList> result, HttpStatus expectedHttpStatus, int rules) {
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
        JaxbRuleList rules = controller.get(page, 2, false, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        validateRules(rules, prefix, expectedUsers);
    }

    /**
     * Helper method that will validate that the provided rules will match the provided user ids.
     */
    private void validateRules(JaxbRuleList rules, String prefix, String... expectedUsers) {
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
        JaxbRuleList rules = controller.get(page, 2, false, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        validateRules(rules, expectedPriorities);
    }

    /**
     * Helper method that will validate that the provided rules will match the provided priorities.
     */
    private void validateRules(JaxbRuleList rules, long... expectedPriorities) {
        assertThat(rules, notNullValue());
        assertThat(rules.getRules(), notNullValue());
        assertThat(rules.getRules().size(), is(expectedPriorities.length));
        for (int i = 0; i < expectedPriorities.length; i++) {
            assertThat(rules.getRules().get(i).getPriority(), is(expectedPriorities[i]));
        }
    }
}
