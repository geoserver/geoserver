/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.internal;

import org.geoserver.geofence.ServicesTest;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.RuleReaderServiceImpl;
import org.junit.Test;

/***
 * 
 * @author Niels Charlier
 *
 */
public class InternalServicesTest extends ServicesTest {

    @Override
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();

        RuleAdminService adminService = (RuleAdminService) applicationContext
                .getBean("ruleAdminService");
        adminService.insert(
                new Rule(0, "cite", null, null, null, "wms", null, "cite", null, GrantType.ALLOW));
        adminService.insert(
                new Rule(1, "cite", null, null, null, "wms", null, "sf", null, GrantType.ALLOW));
    }

    @Test
    public void testConfigurationInternal() {

        assertTrue(configManager.getConfiguration().isInternal());

        assertTrue(geofenceService instanceof RuleReaderServiceImpl);

    }

}
