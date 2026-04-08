/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.it.wms;

import static org.geoserver.acl.domain.rules.CatalogMode.HIDE;
import static org.geoserver.acl.domain.rules.GrantType.ALLOW;
import static org.geoserver.acl.domain.rules.GrantType.DENY;
import static org.geoserver.acl.domain.rules.GrantType.LIMIT;
import static org.geoserver.acl.domain.rules.LayerDetails.LayerType.VECTOR;
import static org.geoserver.acl.domain.rules.SpatialFilterType.CLIP;
import static org.geoserver.acl.domain.rules.SpatialFilterType.INTERSECT;
import static org.geoserver.catalog.LayerGroupInfo.Mode.NAMED;
import static org.geoserver.catalog.LayerGroupInfo.Mode.OPAQUE_CONTAINER;
import static org.geoserver.catalog.LayerGroupInfo.Mode.SINGLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.geoserver.acl.domain.rules.CatalogMode;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.acl.domain.rules.RuleLimits;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings("unused")
public class GetMapIntegrationTest extends AbstractAclWMSIntegrationTest {

    private final String areWKT =
            "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))";

    private String getMapRequest(String layerName) {
        return "wms?layers=%s&styles=&request=getmap&service=wms&width=100&height=100&format=image/png&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002"
                .formatted(layerName);
    }

    /**
     * Tests LayerGroup rule with allowed area. The allowedArea defined for the layerGroup should be applied to the
     * contained layer also.
     */
    @Test
    public void testLimitRuleWithAllowedAreaLayerGroup() throws Exception {
        Rule limit = support.addRule(1, LIMIT, null, "ROLE_ANONYMOUS", "WMS", null, null, "lakes_and_places");
        RuleLimits limits = support.setRuleLimits(limit, HIDE, areWKT, 4326);

        Rule allow = support.addRule(999, ALLOW, null, null, null, null, null, null);

        // check the group works without workspace qualification;
        LayerGroupInfo group = addLakesPlacesLayerGroup(SINGLE, "lakes_and_places");

        login("anonymousUser", "", "ROLE_ANONYMOUS");
        String url = getMapRequest(group.getName());
        BufferedImage image = getAsImage(url, "image/png");
        BufferedImage expectedImage = loadExpectedImage("layer-group-allowed-area.png");
        ImageAssert.assertEquals(expectedImage, image, 500);
    }

    /**
     * Tests LayerGroup rule with allowed area. The allowedArea defined for the layerGroup should be applied to the
     * contained layer too.
     */
    @Test
    public void testLimitAndAllowRuleEnlargementLayerGroup() throws Exception {
        // check the group works without workspace qualification;
        LayerGroupInfo group = addLakesPlacesLayerGroup(SINGLE, "lakes_and_places");

        Rule limit = support.addRule(1, LIMIT, null, "ROLE_ONE", "WMS", null, null, "lakes_and_places");
        Rule allow = support.addRule(2, ALLOW, null, null, null, null, null, null);
        support.setRuleLimits(limit, HIDE, areWKT, 4326);

        login("someUser", "pwd", "ROLE_ONE", "ROLE_TWO");
        String url = getMapRequest(group.getName());
        // ROLE_ONE matches both rules, ROLE_TWO only the allow rule, ROLE_ONE wins with the
        // stricter settings
        BufferedImage expectedImage = loadExpectedImage("layer-group-allowed-area.png");
        BufferedImage image = getAsImage(url, "image/png");
        ImageAssert.assertEquals(expectedImage, image, 500);
    }

    /** Tests that the user cannot access based on the roles of other users */
    @Test
    public void testRoleOnlyMatch() throws Exception {
        LayerGroupInfo group = addLakesPlacesLayerGroup(NAMED, "lakes_and_places");

        Rule deny = support.addRule(2, DENY, "john", null, "WMS", null, null, null);
        Rule allow = support.addRule(1, ALLOW, "jane", null, "WMS", null, null, null);

        login("john", "", "ROLE_USER");
        String url = getMapRequest("Lakes");
        MockHttpServletResponse resp = getAsServletResponse(url);

        assertTrue(resp.getContentAsString().contains("Could not find layer Lakes"));
    }

    /** Tests that the user can access with rules assigned personally and not to a role */
    @Test
    public void testAccessWithoutRole() throws Exception {
        LayerGroupInfo group = addLakesPlacesLayerGroup(NAMED, "lakes_and_places");

        Rule allow = support.addRule(1, ALLOW, "john", null, "WMS", null, null, null);

        login("john", "");
        String url = getMapRequest("Lakes");
        MockHttpServletResponse resp = getAsServletResponse(url);

        assertEquals(200, resp.getStatus());
    }

    /** Tests that the user can access based on any role */
    @Test
    public void testAnyRoleMatch() throws Exception {
        LayerGroupInfo group = addLakesPlacesLayerGroup(NAMED, "lakes_and_places");

        Rule allow = support.addRule(1, ALLOW, null, "ROLE_WMS", null, null, null, null);

        login("john", "", "ROLE_WMS");
        String url = getMapRequest("Lakes");
        MockHttpServletResponse resp = getAsServletResponse(url);

        assertEquals(200, resp.getStatus());

        // check that user is able to access the layer
        MediaType actual = MediaType.parseMediaType(resp.getContentType());
        assertTrue(MediaType.IMAGE_PNG.isCompatibleWith(actual));
        logout();

        login("jane", "", "ROLE_USER");
        MockHttpServletResponse resp2 = getAsServletResponse(url);

        assertEquals(200, resp2.getStatus());
        // check that user is not allowed to access the layer
        assertTrue(resp2.getContentAsString().contains("Could not find layer Lakes"));
    }

    @Test
    public void testDenyRuleOnLayerGroup() throws Exception {
        support.addRule(2, ALLOW, null, null, null, null, null, null);
        support.addRule(1, DENY, null, "ROLE_ANONYMOUS", "WMS", null, null, CONTAINER_GROUP);

        // check the group works without workspace qualification
        login("anonymousUser", "", "ROLE_ANONYMOUS");
        String url = getMapRequest(CONTAINER_GROUP);

        MockHttpServletResponse resp = getAsServletResponse(url);
        assertTrue(resp.getContentAsString().contains("Could not find layer containerGroup"));
    }

    /** Test that direct access to layer in layerGroup is not allowed if container is opaque */
    @Test
    public void testLayerDirectAccessInOpaqueLayerGroup() throws Exception {

        support.addRule(0, ALLOW, null, null, null, null, null, null);

        // check the group works without workspace qualification
        login("anonymousUser", "", "ROLE_ANONYMOUS");

        setupOpaqueGroup(getCatalog());

        LayerGroupInfo group = getCatalog().getLayerGroupByName(OPAQUE_GROUP);

        for (PublishedInfo pi : group.layers()) {
            String url = getMapRequest(pi.prefixedName());
            MockHttpServletResponse resp = getAsServletResponse(url);
            assertTrue(resp.getContentAsString().contains("Could not find layer " + pi.prefixedName()));
        }
    }

    /** Tests that layer contained in NamedTree LayerGroup has the AccessInfo overridden when directly access */
    @Test
    public void testDirectAccessLayerWithNamedTreeContainer() throws Exception {
        Rule allow = support.addRule(2, ALLOW, null, null, null, null, null, null);
        Rule limit = support.addRule(1, LIMIT, null, "ROLE_ANONYMOUS", "WMS", null, null, "lakes_and_places");
        support.setRuleLimits(limit, HIDE, areWKT, 4326);

        // check the group works without workspace qualification;
        LayerGroupInfo group = addLakesPlacesLayerGroup(NAMED, "lakes_and_places");

        login("anonymousUser", "", "ROLE_ANONYMOUS");
        String url = getMapRequest("NamedPlaces");
        BufferedImage image = getAsImage(url, "image/png");
        BufferedImage expectedImage = loadExpectedImage("places-allowed-area.png");
        ImageAssert.assertEquals(expectedImage, image, 500);
    }

    private BufferedImage loadExpectedImage(String resourceName) throws IOException {
        URL expectedResponse = getClass().getResource(resourceName);
        return ImageIO.read(expectedResponse);
    }

    /**
     * Tests that when for a vector layer have been defined two spatial filters, one with Intersects type and one with
     * Clip type, the filters are both applied.
     */
    @Test
    public void testClipAndIntersectSpatialFilters() throws Exception {
        Rule allow = support.addRule(999, ALLOW, null, null, null, null, null, null);
        Rule limit = support.addRule(20, LIMIT, null, "ROLE_ANONYMOUS", "WMS", null, "cite", "BasicPolygons");

        String clipWKT =
                "MultiPolygon (((-2.01345454545454672 5.93445454545454698, -2.00454545454545574 4.30409090909090963, -0.2049090909090916 4.31300000000000061, 1.00672727272727203 5.57809090909091054, 0.97999999999999998 5.98790909090909285, -2.01345454545454672 5.93445454545454698)))";
        support.setRuleLimits(limit, HIDE, clipWKT, 4326, CLIP);

        Rule r3 = support.addRule(21, LIMIT, null, "ROLE_ANONYMOUS2", "WMS", null, "cite", "BasicPolygons");

        String intersectsWKT =
                "MultiPolygon (((-2.41436363636363804 1.47100000000000009, 1.77290909090909077 1.23936363636363645, 1.47890909090909028 -0.40881818181818197, -2.83309090909091044 -0.18609090909090931, -2.41436363636363804 1.47100000000000009)))";
        support.setRuleLimits(r3, HIDE, intersectsWKT, 4326, INTERSECT);

        login("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
        String url = "wms?request=getmap&service=wms&layers=cite:BasicPolygons"
                + "&width=100&height=100&format=image/png&srs=epsg:4326&bbox=-2.0,-1.0,2.0,6.0";

        BufferedImage image = getAsImage(url, "image/png");
        BufferedImage expectedImage = loadExpectedImage("clip_and_intersects.png");
        ImageAssert.assertEquals(expectedImage, image, 500);
    }

    /**
     * Tests that when accessing a layer contained in NamedTree LayerGroup defined under a workspace NPE is not thrown
     * and the layer AccessInfo overridden
     */
    @Test
    public void testDirectAccessLayerWithNonGlobalNamedTreeContainer() throws Exception {
        Rule allow = support.addRule(2, ALLOW, null, null, null, null, null, null);
        Rule limit = support.addRule(1, LIMIT, null, "ROLE_ANONYMOUS", "WMS", null, null, "lakes_and_places");
        support.setRuleLimits(limit, CatalogMode.HIDE, areWKT, 4326);

        // check the group works without workspace qualification;
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
        LayerGroupInfo group = createLakesPlacesLayerGroup(getCatalog(), "lakes_and_places", ws, NAMED, null);

        login("anonymousUser", "", "ROLE_ANONYMOUS");
        String url = getMapRequest("cite:NamedPlaces");
        BufferedImage image = getAsImage(url, "image/png");
        BufferedImage expectedImage = loadExpectedImage("places-allowed-area.png");
        ImageAssert.assertEquals(expectedImage, image, 500);
    }

    @Test
    public void testAccessManagerNotFailsGetMapNestedGroup() throws Exception {
        Rule r1 = support.addRule(1, ALLOW, null, null, null, null, null, null);

        addLakesPlacesLayerGroup(SINGLE, "nested");

        addLakesPlacesLayerGroup(OPAQUE_CONTAINER, "container");

        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        LayerGroupInfo group = getCatalog().getLayerGroupByName("container");
        LayerGroupInfo nested = getCatalog().getLayerGroupByName("nested");
        group.getLayers().add(nested);
        group.getStyles().add(null);
        getCatalog().save(group);
        logout();

        login("anonymousUser", "", "ROLE_ANONYMOUS");
        String url = getMapRequest(group.getName());
        MockHttpServletResponse response = getAsServletResponse(url);
        MediaType actual = MediaType.parseMediaType(response.getContentType());
        assertTrue(actual.toString(), MediaType.IMAGE_PNG.isCompatibleWith(actual));
    }

    @Test
    public void testLayerGroupAndStyleRules() throws Exception {
        final String layerGroupName = "lakes_and_places_style";

        Rule r1 = support.addRule(2, ALLOW, null, null, null, null, null, null);
        Rule r2 = support.addRule(1, ALLOW, null, "ROLE_ANONYMOUS", "WMS", null, "cite", "Forests");

        // setting the allowed styles
        Set<String> allowedStyles = Set.of("Lakes", "NamedPlaces");
        support.setLayerDetails(r2, allowedStyles, Set.of(), HIDE, null, null, VECTOR);

        addLakesPlacesLayerGroup(SINGLE, layerGroupName);

        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        LayerGroupInfo group = getCatalog().getLayerGroupByName(layerGroupName);

        // polygon is not among the allowed styles
        StyleInfo polygonStyle = getCatalog().getStyleByName("polygon");
        LayerInfo forest = getCatalog().getLayerByName(getLayerId(MockData.FORESTS));
        forest.getStyles().add(polygonStyle);
        getCatalog().save(forest);

        // layergroup style containing style not among the allowed ones
        addLayerGroupStyle(group, "forests_style", List.of(forest), List.of(polygonStyle));
        logout();

        login("anonymousUser", "", "ROLE_ANONYMOUS");
        String url = getMapRequest(group.getName());
        MockHttpServletResponse response = getAsServletResponse(url);
        // first request default style should work
        MediaType expected = MediaType.IMAGE_PNG;
        MediaType actual = MediaType.parseMediaType(response.getContentType());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getSubtype(), actual.getSubtype());

        url = getMapRequest(group.getName()).replace("styles=&", "styles=forests_style&");

        response = getAsServletResponse(url);
        // should get an error since the polygon style is contained in the lg forest_style
        expected = MediaType.TEXT_XML;
        actual = MediaType.parseMediaType(response.getContentType());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getSubtype(), actual.getSubtype());

        assertTrue(response.getContentAsString().contains("style is not available on this layer"));
    }

    @Override
    protected BufferedImage getAsImage(String path, String mime) throws Exception {
        MockHttpServletResponse resp = getAsServletResponse(path);
        MediaType expected = MediaType.parseMediaType(mime);
        MediaType actual = MediaType.parseMediaType(resp.getContentType());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getSubtype(), actual.getSubtype());
        try (InputStream is = getBinaryInputStream(resp)) {
            return ImageIO.read(is);
        }
    }
}
