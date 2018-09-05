/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.internal;

import static org.junit.Assert.assertTrue;

import org.geoserver.geofence.ServicesTest;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.server.rest.RulesRestController;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.RuleReaderServiceImpl;
import org.geoserver.geofence.services.dto.ShortRule;
import org.junit.Before;
import org.junit.Test;

/** @author Niels Charlier */
public class InternalServicesTest extends ServicesTest {

    protected RulesRestController controller;

    protected RuleAdminService adminService;

    @Before
    public void initGeoFenceControllers() {
        controller = (RulesRestController) applicationContext.getBean("rulesRestController");
        adminService = (RuleAdminService) applicationContext.getBean("ruleAdminService");

        if (adminService.getCountAll() > 0) {
            for (ShortRule r : adminService.getAll()) {
                adminService.delete(r.getId());
            }
        }

        Rule citeRule =
                new Rule(0, "cite", null, null, null, "wms", null, "cite", null, GrantType.ALLOW);
        Rule sfRule =
                new Rule(1, "cite", null, null, null, "wms", null, "sf", null, GrantType.ALLOW);
        adminService.insert(citeRule);
        adminService.insert(sfRule);
    }

    @Test
    public void testConfigurationInternal() {
        assertTrue(configManager.getConfiguration().isInternal());
        if (geofenceService != null) {
            assertTrue(geofenceService instanceof RuleReaderServiceImpl);
        }
    }
}
