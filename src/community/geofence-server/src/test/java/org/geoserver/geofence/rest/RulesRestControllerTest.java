/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest;

import org.geoserver.geofence.GeofenceBaseTest;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.rest.xml.JaxbRule;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.exception.NotFoundServiceEx;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;

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

}
