/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.server.integration;

import static org.geoserver.geofence.server.integration.GeofenceGetMapIntegrationTest.deleteRules;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.core.model.enums.LayerType;
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

    @Test
    public void testLegendGraphicLayerGroupStyle() throws Exception {
        Long ruleId1 = null;
        Long ruleId2 = null;
        LayerGroupInfo group = null;
        String layerGroupName = "lakes_and_places_legend";
        try {
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1, ruleService);
            ruleId2 =
                    addRule(
                            GrantType.ALLOW,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            "cite",
                            "Forests",
                            0,
                            ruleService);

            List<String> allowedStyles = Arrays.asList("Lakes", "NamedPlaces");
            addLayerDetails(
                    ruleService,
                    ruleId2,
                    new HashSet<>(allowedStyles),
                    Collections.emptySet(),
                    CatalogMode.HIDE,
                    null,
                    null,
                    LayerType.VECTOR);

            addLakesPlacesLayerGroup(LayerGroupInfo.Mode.SINGLE, layerGroupName);

            login("admin", "geoserver", "ROLE_ADMINISTRATOR");
            group = getCatalog().getLayerGroupByName(layerGroupName);

            // not among the allowed styles, adding it to a layergroup style
            StyleInfo polygonStyle = getCatalog().getStyleByName("polygon");
            LayerInfo forest = getCatalog().getLayerByName(getLayerId(MockData.FORESTS));
            forest.getStyles().add(polygonStyle);
            getCatalog().save(forest);
            List<StyleInfo> styles = new ArrayList<>();
            styles.add(polygonStyle);
            addLayerGroupStyle(group, "forests_style", Arrays.asList(forest), styles);
            logout();

            login("anonymousUser", "", "ROLE_ANONYMOUS");
            String url =
                    "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                            + "&layer="
                            + group.getName()
                            + "&style="
                            + "&format=image/png&width=20&height=20";
            MockHttpServletResponse response = getAsServletResponse(url);
            // default lg style should not fail
            assertEquals(response.getContentType(), "image/png");

            url =
                    "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                            + "&layer="
                            + group.getName()
                            + "&style=forests_style"
                            + "&format=image/png&width=20&height=20";
            response = getAsServletResponse(url);
            // should fail the forests_style contains the not allowed polygon style
            assertEquals(getBaseMimeType(response.getContentType()), "application/vnd.ogc.se_xml");
            assertTrue(
                    response.getContentAsString().contains("style is not available on this layer"));
        } finally {
            deleteRules(ruleService, ruleId1, ruleId2);
            logout();
            removeLayerGroup(group);
        }
    }
}
