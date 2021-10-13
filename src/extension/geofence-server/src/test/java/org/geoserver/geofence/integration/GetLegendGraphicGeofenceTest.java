/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.integration;

import static org.geoserver.geofence.integration.GeofenceGetMapIntegrationTest.deleteRules;
import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetLegendGraphicGeofenceTest extends GeofenceWMSTestSupport {

    @Test
    public void testLegendGraphicNestedGroups() throws Exception {
        Long ruleId1 = null;
        LayerGroupInfo group = null;
        LayerGroupInfo nested = null;
        try {
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1, ruleService);

            addLakesPlacesLayerGroup(LayerGroupInfo.Mode.SINGLE, "nested");

            addLakesPlacesLayerGroup(LayerGroupInfo.Mode.OPAQUE_CONTAINER, "container");

            login("admin", "geoserver", "ROLE_ADMINISTRATOR");
            group = getCatalog().getLayerGroupByName("container");
            nested = getCatalog().getLayerGroupByName("nested");
            group.getLayers().add(nested);
            group.getStyles().add(null);
            getCatalog().save(group);
            logout();

            login("anonymousUser", "", "ROLE_ANONYMOUS");
            String url =
                    "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                            + "&layer="
                            + group.getName()
                            + "&style="
                            + "&format=image/png&width=20&height=20";
            MockHttpServletResponse response = getAsServletResponse(url);
            assertEquals(response.getContentType(), "image/png");

        } finally {
            deleteRules(ruleService, ruleId1);
            logout();
            removeLayerGroup(group);
            removeLayerGroup(nested);
        }
    }
}
