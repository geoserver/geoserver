/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.geofence.ServicesTest;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.RuleLimits;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.server.rest.RulesRestController;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.RuleReaderServiceImpl;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.services.dto.ShortRule;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

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
                new Rule(
                        0,
                        "cite",
                        null,
                        null,
                        null,
                        "wms",
                        null,
                        null,
                        "cite",
                        null,
                        GrantType.ALLOW);
        Rule sfRule =
                new Rule(
                        1,
                        "cite",
                        null,
                        null,
                        null,
                        "wms",
                        null,
                        null,
                        "sf",
                        null,
                        GrantType.ALLOW);
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

    @Test
    public void testAllowedAreaSRIDIsPreserved() throws ParseException {
        // test that when retrieving an AccessInfo from the rule service,
        // the original SRID is returned in the AllowedArea wkt representation.
        Rule rule1 =
                new Rule(
                        999, null, null, null, null, null, null, null, null, null, GrantType.ALLOW);
        adminService.insert(rule1);

        Rule rule2 =
                new Rule(
                        0,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "layerGroup",
                        GrantType.LIMIT);
        Long idRule2 = adminService.insert(rule2);
        RuleLimits ruleLimits = new RuleLimits();
        ruleLimits.setCatalogMode(CatalogMode.HIDE);
        MultiPolygon mp =
                (MultiPolygon)
                        new WKTReader()
                                .read(
                                        "MULTIPOLYGON(((0.0016139656066815888 -0.0006386457758059581,0.0019599705696027314 -0.0006386457758059581,0.0019599705696027314 -0.0008854090051601674,0.0016139656066815888 -0.0008854090051601674,0.0016139656066815888 -0.0006386457758059581)))");
        mp.setSRID(3857);
        ruleLimits.setAllowedArea(mp);
        adminService.setLimits(idRule2, ruleLimits);

        RuleFilter filter = new RuleFilter();
        filter.setLayer("layerGroup");
        AccessInfo accessInfo = geofenceService.getAccessInfo(filter);
        String srid = accessInfo.getAreaWkt().split(";")[0].split("=")[1];
        assertEquals("3857", srid);
    }
}
