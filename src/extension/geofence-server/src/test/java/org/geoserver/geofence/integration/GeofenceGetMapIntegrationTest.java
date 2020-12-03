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
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.image.test.ImageAssert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
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
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1);
            ruleId2 =
                    addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "lakes_and_places",
                            0);
            String areWKT =
                    "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))";
            addRuleLimits(ruleId2, CatalogMode.HIDE, areWKT, 4326);
            // check the group works without workspace qualification;
            group = addLakesPlacesLayerGroup(LayerGroupInfo.Mode.SINGLE);

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
            deletesAllRules(ruleId1, ruleId2);
            logout();
            removeLakesPlacesLayerGroup(group);
        }
    }

    @Test
    public void testDenyRuleOnLayerGroup() throws Exception {
        // test deny rule on layerGroup
        Long ruleId1 = null;
        Long ruleId2 = null;
        try {
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1);
            ruleId2 =
                    addRule(
                            GrantType.DENY,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            CONTAINER_GROUP,
                            0);
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
            deletesAllRules(ruleId1, ruleId2);
            logout();
        }
    }

    @Test
    public void testLayerDirectAccessInOpaqueLayerGroup() throws Exception {
        // test that direct access to layer in layerGroup is not allowed if container is opaque
        Long ruleId = null;
        try {
            ruleId = addRule(GrantType.ALLOW, null, null, null, null, null, null, 0);
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
            deletesAllRules(ruleId);
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
            ruleId1 = addRule(GrantType.ALLOW, null, null, null, null, null, null, 1);
            ruleId2 =
                    addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "lakes_and_places",
                            0);
            String areWKT =
                    "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))";
            addRuleLimits(ruleId2, CatalogMode.HIDE, areWKT, 4326);
            // check the group works without workspace qualification;
            group = addLakesPlacesLayerGroup(LayerGroupInfo.Mode.NAMED);

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
            deletesAllRules(ruleId1, ruleId2);
            logout();
            removeLakesPlacesLayerGroup(group);
        }
    }

    private long addRule(
            GrantType access,
            String username,
            String roleName,
            String service,
            String request,
            String workspace,
            String layer,
            long priority) {

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

    private void addRuleLimits(long ruleId, CatalogMode mode, String allowedArea, Integer srid)
            throws org.locationtech.jts.io.ParseException {
        RuleLimits limits = new RuleLimits();
        limits.setCatalogMode(mode);
        MultiPolygon allowedAreaGeom = (MultiPolygon) new WKTReader().read(allowedArea);
        if (srid != null) allowedAreaGeom.setSRID(srid);
        limits.setAllowedArea(allowedAreaGeom);
        ruleService.setLimits(ruleId, limits);
    }

    private void deletesAllRules(Long... ids) {
        for (Long id : ids) {
            if (id != null) ruleService.delete(id);
        }
    }

    private LayerGroupInfo addLakesPlacesLayerGroup(LayerGroupInfo.Mode mode) throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        LayerGroupInfo group = createLakesPlacesLayerGroup(getCatalog(), mode, null);
        logout();
        return group;
    }

    private void removeLakesPlacesLayerGroup(LayerGroupInfo group) throws Exception {
        if (group != null) {
            login("admin", "geoserver", "ROLE_ADMINISTRATOR");
            getCatalog().remove(group);
            logout();
        }
    }
}
