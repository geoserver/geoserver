/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.integration;

import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.RuleLimits;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.core.model.enums.SpatialFilterType;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.image.test.ImageAssert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.mock.web.MockHttpServletResponse;

public class GeofenceGetMapIntegrationTest extends WMSTestSupport {

    private RuleAdminService ruleService;

    @Before
    public void before() {
        ruleService = (RuleAdminService) applicationContext.getBean("ruleAdminService");
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
    }

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
            group = addLakesPlacesLayerGroup(LayerGroupInfo.Mode.SINGLE, "lakes_and_places");

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
            ImageAssert.assertEquals(image, expectedImage, 500);
        } finally {
            deleteRules(ruleService, ruleId1, ruleId2);
            logout();
            removeLayerGroup(group);
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
        // tests that layer contained in NamedTree LayerGroup has the AccessInfo overrided
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
            group = addLakesPlacesLayerGroup(LayerGroupInfo.Mode.NAMED, "lakes_and_places");

            login("anonymousUser", "", "ROLE_ANONYMOUS");
            String url =
                    "wms?request=getmap&service=wms"
                            + "&layers=cite:NamedPlaces"
                            + "&width=100&height=100&format=image/png"
                            + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
            BufferedImage image = getAsImage(url, "image/png");
            URL expectedResponse = getClass().getResource("places-allowed-area.png");
            BufferedImage expectedImage = ImageIO.read(expectedResponse);
            ImageAssert.assertEquals(image, expectedImage, 500);
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
            ImageAssert.assertEquals(image, expectedImage, 500);
        } finally {
            deleteRules(ruleService, ruleId1, ruleId2, ruleId3);
            logout();
        }
    }

    static long addRule(
            GrantType access,
            String username,
            String roleName,
            String service,
            String request,
            String workspace,
            String layer,
            long priority,
            RuleAdminService ruleService) {

        Rule rule = new Rule();
        rule.setAccess(access);
        rule.setUsername(username);
        rule.setRolename(roleName);
        rule.setService(service);
        rule.setRequest(request);
        rule.setWorkspace(workspace);
        rule.setLayer(layer);
        rule.setPriority(priority);
        return ruleService.insert(rule);
    }

    static void addRuleLimits(
            long ruleId,
            CatalogMode mode,
            String allowedArea,
            Integer srid,
            RuleAdminService ruleService)
            throws ParseException {
        addRuleLimits(ruleId, mode, allowedArea, srid, null, ruleService);
    }

    static void addRuleLimits(
            long ruleId,
            CatalogMode mode,
            String allowedArea,
            Integer srid,
            SpatialFilterType spatialFilterType,
            RuleAdminService ruleService)
            throws org.locationtech.jts.io.ParseException {
        RuleLimits limits = new RuleLimits();
        limits.setCatalogMode(mode);
        MultiPolygon allowedAreaGeom = (MultiPolygon) new WKTReader().read(allowedArea);
        if (srid != null) allowedAreaGeom.setSRID(srid);
        limits.setAllowedArea(allowedAreaGeom);
        if (spatialFilterType == null) spatialFilterType = SpatialFilterType.INTERSECT;
        limits.setSpatialFilterType(spatialFilterType);
        ruleService.setLimits(ruleId, limits);
    }

    static void deleteRules(RuleAdminService ruleService, Long... ids) {
        for (Long id : ids) {
            if (id != null) ruleService.delete(id);
        }
    }

    private LayerGroupInfo addLakesPlacesLayerGroup(LayerGroupInfo.Mode mode, String name)
            throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        LayerGroupInfo group = createLakesPlacesLayerGroup(getCatalog(), name, mode, null);
        logout();
        return group;
    }

    private void removeLayerGroup(LayerGroupInfo... groups) {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        for (LayerGroupInfo group : groups) {
            if (group != null) {
                getCatalog().remove(group);
            }
        }
        logout();
    }
}
