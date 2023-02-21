/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.server.integration;

import static org.geoserver.catalog.LayerGroupInfo.Mode.NAMED;
import static org.geoserver.catalog.LayerGroupInfo.Mode.OPAQUE_CONTAINER;
import static org.geoserver.catalog.LayerGroupInfo.Mode.SINGLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.core.model.enums.LayerType;
import org.geoserver.geofence.core.model.enums.SpatialFilterType;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GeofenceGetMapIntegrationTest extends GeofenceWMSTestSupport {

    private GeoFenceConfigurationManager configurationManager;

    @Test
    public void testLimitRuleWithAllowedAreaLayerGroup() throws Exception {
        // tests LayerGroup rule with allowed area. The allowedArea defined for the layerGroup
        // should be
        // applied to the contained layen also.
        Long ruleId1 = null;
        Long ruleId2 = null;
        LayerGroupInfo group = null;
        try {
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1, ruleService);
            ruleId2 =
                    addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "lakes_and_places",
                            0,
                            ruleService);
            String areWKT =
                    "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))";
            addRuleLimits(ruleId2, CatalogMode.HIDE, areWKT, 4326, ruleService);
            // check the group works without workspace qualification;
            group = addLakesPlacesLayerGroup(SINGLE, "lakes_and_places");

            login("anonymousUser", "", "ROLE_ANONYMOUS");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers="
                            + group.getName()
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            BufferedImage image = getAsImage(url, "image/png");
            URL expectedResponse = getClass().getResource("layer-group-allowed-area.png");
            BufferedImage expectedImage = ImageIO.read(expectedResponse);
            ImageAssert.assertEquals(expectedImage, image, 500);
        } finally {
            deleteRules(ruleService, ruleId1, ruleId2);
            logout();
            removeLayerGroup(group);
        }
    }

    /**
     * Tests that the user cannot access based on the roles of other users
     *
     * @throws Exception
     */
    @Test
    public void testRoleOnlyMatch() throws Exception {

        configurationManager =
                applicationContext.getBean(
                        "geofenceConfigurationManager", GeoFenceConfigurationManager.class);
        GeoFenceConfiguration config = configurationManager.getConfiguration();
        config.setUseRolesToFilter(true);
        config.getRoles().add("ROLE_USER");

        LayerGroupInfo group = addLakesPlacesLayerGroup(NAMED, "lakes_and_places");

        Long ruleId1 = null;
        Long ruleId2 = null;
        try {
            ruleId1 =
                    addRule(GrantType.DENY, "john", null, "WMS", null, null, null, 1, ruleService);
            ruleId2 =
                    addRule(GrantType.ALLOW, "jane", null, "WMS", null, null, null, 0, ruleService);

            login("john", "", "ROLE_USER");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers=Lakes"
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            MockHttpServletResponse resp = getAsServletResponse(url);

            assertTrue(resp.getContentAsString().contains("Could not find layer Lakes"));
        } finally {
            deleteRules(ruleService, ruleId1, ruleId2);
            config.setUseRolesToFilter(false);
            config.getRoles().remove("ROLE_USER");
            removeLayerGroup(group);
            logout();
        }
    }

    /**
     * Tests that the user can access with rules assigned personally and not to a role
     *
     * @throws Exception
     */
    @Test
    public void testAccessWithoutRole() throws Exception {

        configurationManager =
                applicationContext.getBean(
                        "geofenceConfigurationManager", GeoFenceConfigurationManager.class);
        GeoFenceConfiguration config = configurationManager.getConfiguration();
        config.setUseRolesToFilter(true);

        LayerGroupInfo group = addLakesPlacesLayerGroup(NAMED, "lakes_and_places");

        Long ruleId1 = null;
        try {
            ruleId1 =
                    addRule(GrantType.ALLOW, "john", null, "WMS", null, null, null, 1, ruleService);

            login("john", "");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers=Lakes"
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            MockHttpServletResponse resp = getAsServletResponse(url);

            assertEquals(200, resp.getStatus());
        } finally {
            deleteRules(ruleService, ruleId1);
            config.setUseRolesToFilter(false);
            removeLayerGroup(group);
            logout();
        }
    }

    /**
     * Tests that the user can access based on any role
     *
     * @throws Exception
     */
    @Test
    public void testAnyRoleMatch() throws Exception {

        configurationManager =
                applicationContext.getBean(
                        "geofenceConfigurationManager", GeoFenceConfigurationManager.class);
        GeoFenceConfiguration config = configurationManager.getConfiguration();
        config.setUseRolesToFilter(true);
        config.getRoles().add("*");

        LayerGroupInfo group = addLakesPlacesLayerGroup(NAMED, "lakes_and_places");

        Long ruleId1 = null;
        try {
            ruleId1 =
                    addRule(
                            GrantType.ALLOW,
                            null,
                            "ROLE_WMS",
                            null,
                            null,
                            null,
                            null,
                            0,
                            ruleService);

            login("john", "", "ROLE_WMS");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers=Lakes"
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            MockHttpServletResponse resp = getAsServletResponse(url);

            assertEquals(200, resp.getStatus());

            // check that user is able to access the layer
            assertEquals("image/png", resp.getContentType());
            logout();

            login("jane", "", "ROLE_USER");
            MockHttpServletResponse resp2 = getAsServletResponse(url);

            assertEquals(200, resp2.getStatus());
            // check that user is not allowed to access the layer
            assertTrue(resp2.getContentAsString().contains("Could not find layer Lakes"));
        } finally {
            deleteRules(ruleService, ruleId1);
            config.setUseRolesToFilter(false);
            config.getRoles().remove("*");
            removeLayerGroup(group);
            logout();
        }
    }

    @Test
    public void testDenyRuleOnLayerGroup() throws Exception {
        // test deny rule on layerGroup
        Long ruleId1 = null;
        Long ruleId2 = null;
        try {
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1, ruleService);
            ruleId2 =
                    addRule(
                            GrantType.DENY,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            CONTAINER_GROUP,
                            0,
                            ruleService);
            // check the group works without workspace qualification
            login("anonymousUser", "", "ROLE_ANONYMOUS");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers="
                            + CONTAINER_GROUP
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            MockHttpServletResponse resp = getAsServletResponse(url);
            assertTrue(resp.getContentAsString().contains("Could not find layer containerGroup"));
        } finally {
            deleteRules(ruleService, ruleId1, ruleId2);
            logout();
        }
    }

    @Test
    public void testLayerDirectAccessInOpaqueLayerGroup() throws Exception {
        // test that direct access to layer in layerGroup is not allowed if container is opaque
        Long ruleId = null;
        try {
            ruleId = addRule(GrantType.ALLOW, null, null, null, null, null, null, 0, ruleService);
            // check the group works without workspace qualification
            login("anonymousUser", "", "ROLE_ANONYMOUS");
            setupOpaqueGroup(getCatalog());
            LayerGroupInfo group = getCatalog().getLayerGroupByName(OPAQUE_GROUP);
            for (PublishedInfo pi : group.layers()) {
                String url =
                        "wms?request=getmap&service=wms"
                                + "&layers="
                                + pi.prefixedName()
                                + "&width=100&height=100&format=image/png"
                                + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
                MockHttpServletResponse resp = getAsServletResponse(url);
                assertTrue(
                        resp.getContentAsString()
                                .contains("Could not find layer " + pi.prefixedName()));
            }
        } finally {
            deleteRules(ruleService, ruleId);
            logout();
        }
    }

    @Test
    public void testDirectAccessLayerWithNamedTreeContainer() throws Exception {
        // tests that layer contained in NamedTree LayerGroup has the AccessInfo overridden
        // when directly access
        Long ruleId1 = null;
        Long ruleId2 = null;
        LayerGroupInfo group = null;
        try {
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1, ruleService);
            ruleId2 =
                    addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "lakes_and_places",
                            0,
                            ruleService);
            String areWKT =
                    "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))";
            addRuleLimits(ruleId2, CatalogMode.HIDE, areWKT, 4326, ruleService);
            // check the group works without workspace qualification;
            group = addLakesPlacesLayerGroup(NAMED, "lakes_and_places");

            login("anonymousUser", "", "ROLE_ANONYMOUS");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers=cite:NamedPlaces"
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            BufferedImage image = getAsImage(url, "image/png");
            URL expectedResponse = getClass().getResource("places-allowed-area.png");
            BufferedImage expectedImage = ImageIO.read(expectedResponse);
            ImageAssert.assertEquals(expectedImage, image, 500);
        } finally {
            deleteRules(ruleService, ruleId1, ruleId2);
            logout();
            removeLayerGroup(group);
        }
    }

    @Test
    public void testClipAndIntersectSpatialFilters() throws Exception {
        // Tests that when for a vector layer have been defined two spatial filters, one
        // with Intersects type and one with Clip type, the filters are both applied.
        Long ruleId1 = null;
        Long ruleId2 = null;
        Long ruleId3 = null;
        try {
            ruleId1 =
                    addRule(GrantType.ALLOW, null, null, null, null, null, null, 999, ruleService);
            ruleId2 =
                    addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            "cite",
                            "BasicPolygons",
                            20,
                            ruleService);
            String clipWKT =
                    "MultiPolygon (((-2.01345454545454672 5.93445454545454698, -2.00454545454545574 4.30409090909090963, -0.2049090909090916 4.31300000000000061, 1.00672727272727203 5.57809090909091054, 0.97999999999999998 5.98790909090909285, -2.01345454545454672 5.93445454545454698)))";
            addRuleLimits(
                    ruleId2, CatalogMode.HIDE, clipWKT, 4326, SpatialFilterType.CLIP, ruleService);

            ruleId3 =
                    addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS2",
                            "WMS",
                            null,
                            "cite",
                            "BasicPolygons",
                            21,
                            ruleService);

            String intersectsWKT =
                    "MultiPolygon (((-2.41436363636363804 1.47100000000000009, 1.77290909090909077 1.23936363636363645, 1.47890909090909028 -0.40881818181818197, -2.83309090909091044 -0.18609090909090931, -2.41436363636363804 1.47100000000000009)))";
            addRuleLimits(
                    ruleId3,
                    CatalogMode.HIDE,
                    intersectsWKT,
                    4326,
                    SpatialFilterType.INTERSECT,
                    ruleService);

            login("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers=cite:BasicPolygons"
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-2.0,-1.0,2.0,6.0";

            BufferedImage image = getAsImage(url, "image/png");
            URL expectedResponse = getClass().getResource("clip_and_intersects.png");
            BufferedImage expectedImage = ImageIO.read(expectedResponse);
            ImageAssert.assertEquals(expectedImage, image, 500);
        } finally {
            deleteRules(ruleService, ruleId1, ruleId2, ruleId3);
            logout();
        }
    }

    @Test
    public void testDirectAccessLayerWithNonGlobalNamedTreeContainer() throws Exception {
        // tests that when accessing a layer contained in NamedTree LayerGroup
        // defined under a workspace NPE is not thrown and the layer AccessInfo overridden
        Long ruleId1 = null;
        Long ruleId2 = null;
        LayerGroupInfo group = null;
        try {
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1, ruleService);
            ruleId2 =
                    addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "lakes_and_places",
                            0,
                            ruleService);
            String areWKT =
                    "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))";
            addRuleLimits(ruleId2, CatalogMode.HIDE, areWKT, 4326, ruleService);
            // check the group works without workspace qualification;
            WorkspaceInfo ws = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
            group = createLakesPlacesLayerGroup(getCatalog(), "lakes_and_places", ws, NAMED, null);

            login("anonymousUser", "", "ROLE_ANONYMOUS");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers=cite:NamedPlaces"
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            BufferedImage image = getAsImage(url, "image/png");
            URL expectedResponse = getClass().getResource("places-allowed-area.png");
            BufferedImage expectedImage = ImageIO.read(expectedResponse);
            ImageAssert.assertEquals(expectedImage, image, 500);
        } finally {
            deleteRules(ruleService, ruleId1, ruleId2);
            logout();
            removeLayerGroup(group);
        }
    }

    @Test
    public void testGeofenceAccessManagerNotFailsGetMapNestedGroup() throws Exception {

        Long ruleId1 = null;
        LayerGroupInfo group = null;
        LayerGroupInfo nested = null;
        try {
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1, ruleService);

            addLakesPlacesLayerGroup(SINGLE, "nested");

            addLakesPlacesLayerGroup(OPAQUE_CONTAINER, "container");

            login("admin", "geoserver", "ROLE_ADMINISTRATOR");
            group = getCatalog().getLayerGroupByName("container");
            nested = getCatalog().getLayerGroupByName("nested");
            group.getLayers().add(nested);
            group.getStyles().add(null);
            getCatalog().save(group);
            logout();

            login("anonymousUser", "", "ROLE_ANONYMOUS");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers="
                            + group.getName()
                            + "&styles="
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            MockHttpServletResponse response = getAsServletResponse(url);
            assertEquals("image/png", response.getContentType());

        } finally {
            deleteRules(ruleService, ruleId1);
            logout();
            removeLayerGroup(group);
            removeLayerGroup(nested);
        }
    }

    @Test
    public void testLayerGroupAndStyleRules() throws Exception {

        Long r1 = null;
        Long r2 = null;
        LayerGroupInfo group = null;
        String layerGroupName = "lakes_and_places_style";
        try {
            r1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1, ruleService);
            r2 =
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

            // setting the allowed styles
            List<String> allowedStyles = Arrays.asList("Lakes", "NamedPlaces");
            addLayerDetails(
                    ruleService,
                    r2,
                    new HashSet<>(allowedStyles),
                    Collections.emptySet(),
                    CatalogMode.HIDE,
                    null,
                    null,
                    LayerType.VECTOR);

            addLakesPlacesLayerGroup(SINGLE, layerGroupName);

            login("admin", "geoserver", "ROLE_ADMINISTRATOR");
            group = getCatalog().getLayerGroupByName(layerGroupName);

            // polygon is not among the allowed styles
            StyleInfo polygonStyle = getCatalog().getStyleByName("polygon");
            LayerInfo forest = getCatalog().getLayerByName(getLayerId(MockData.FORESTS));
            forest.getStyles().add(polygonStyle);
            getCatalog().save(forest);
            List<StyleInfo> styles = new ArrayList<>();
            styles.add(polygonStyle);
            // layergroup style containing style not among the allowed ones
            addLayerGroupStyle(group, "forests_style", Arrays.asList(forest), styles);
            logout();

            login("anonymousUser", "", "ROLE_ANONYMOUS");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers="
                            + group.getName()
                            + "&styles="
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            MockHttpServletResponse response = getAsServletResponse(url);
            // first request default style should work
            assertEquals("image/png", response.getContentType());

            url =
                    "wms?request=getmap&service=wms"
                            + "&layers="
                            + group.getName()
                            + "&styles=forests_style"
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            response = getAsServletResponse(url);
            // should get an error since the polygon style is contained in the lg forest_style
            assertEquals("text/xml", getBaseMimeType(response.getContentType()));
            assertTrue(
                    response.getContentAsString().contains("style is not available on this layer"));
        } finally {
            deleteRules(ruleService, r1, r2);
            logout();
            removeLayerGroup(group);
        }
    }

    @Test
    public void testLimitAndAllowRuleEnlargementLayerGroup() throws Exception {
        // tests LayerGroup rule with allowed area. The allowedArea defined for the layerGroup
        // should be
        // applied to the contained layen also.
        Long ruleId1 = null;
        Long ruleId2 = null;
        LayerGroupInfo group = null;
        try {
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1, ruleService);
            ruleId2 =
                    addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ONE",
                            "WMS",
                            null,
                            null,
                            "lakes_and_places",
                            0,
                            ruleService);
            String areWKT =
                    "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))";
            addRuleLimits(ruleId2, CatalogMode.HIDE, areWKT, 4326, ruleService);

            // check the group works without workspace qualification;
            group = addLakesPlacesLayerGroup(SINGLE, "lakes_and_places");

            login("someUser", "", "ROLE_ONE", "ROLE_TWO");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers="
                            + group.getName()
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            BufferedImage image = getAsImage(url, "image/png");
            URL expectedResponse = getClass().getResource("lakes_and_places_full.png");
            BufferedImage expectedImage = ImageIO.read(expectedResponse);
            ImageAssert.assertEquals(image, expectedImage, 500);
        } finally {
            deleteRules(ruleService, ruleId1, ruleId2);
            logout();
            removeLayerGroup(group);
        }
    }
}
