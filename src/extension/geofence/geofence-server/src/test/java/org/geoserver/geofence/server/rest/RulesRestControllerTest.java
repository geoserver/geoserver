/* (c) 2015 - 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.geofence.GeofenceBaseTest;
import org.geoserver.geofence.core.dao.DuplicateKeyException;
import org.geoserver.geofence.core.model.IPAddressRange;
import org.geoserver.geofence.core.model.LayerAttribute;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.server.rest.xml.JaxbRule;
import org.geoserver.geofence.server.rest.xml.JaxbRuleList;
import org.geoserver.geofence.server.xstream.GeoFenceServerXStreamInitializer;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geoserver.geofence.services.exception.NotFoundServiceEx;
import org.geoserver.rest.RestBaseController;
import org.geotools.gml3.bindings.GML3MockData;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

public class RulesRestControllerTest extends GeofenceBaseTest {

    protected RulesRestController controller;

    protected RuleAdminService adminService;

    @Before
    public void initGeoFenceControllers() {
        controller = (RulesRestController) applicationContext.getBean("rulesRestController");
        adminService = (RuleAdminService) applicationContext.getBean("ruleAdminService");
    }

    @Test
    public void testInsertUpdateDelete() {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(5L);
        rule.setUserName("pippo");
        rule.setRoleName("clown");
        rule.setAddressRange("127.0.0.1/32");
        rule.setService("wfs");
        rule.setRequest("getFeature");
        rule.setSubfield("subfield");
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("ALLOW");

        long id = prepareGeoFenceTestRules(rule);

        Rule realRule = adminService.get(id);

        assertEquals(rule.getPriority().longValue(), realRule.getPriority());
        assertEquals(rule.getUserName(), realRule.getUsername());
        assertEquals(rule.getRoleName(), realRule.getRolename());
        assertEquals(rule.getAddressRange(), realRule.getAddressRange().getCidrSignature());
        assertEquals(rule.getService().toUpperCase(), realRule.getService().toUpperCase());
        assertEquals(rule.getRequest().toUpperCase(), realRule.getRequest().toUpperCase());
        assertEquals(rule.getSubfield().toUpperCase(), realRule.getSubfield().toUpperCase());
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
        long id2 = Long.parseLong(controller.insert(rule2));

        realRule = adminService.get(id);
        assertEquals(6L, realRule.getPriority());

        // test changing to non-existing priority

        JaxbRule rule2Mods = new JaxbRule();
        rule2Mods.setPriority(3L);
        controller.update(id2, rule2Mods);

        realRule = adminService.get(id2);
        assertEquals(3L, realRule.getPriority());

        // test changing to existing priority

        rule2Mods = new JaxbRule();
        rule2Mods.setPriority(6L);
        controller.update(id2, rule2Mods);

        realRule = adminService.get(id2);
        assertEquals(6L, realRule.getPriority());
        realRule = adminService.get(id);
        assertEquals(7L, realRule.getPriority());

        // not found - will be translated by spring exception handler to code 404
        controller.delete(id);
        boolean notfound = false;
        try {
            adminService.get(id);
        } catch (NotFoundServiceEx e) {
            notfound = true;
        }
        assertTrue(notfound);

        // conflict - will be translated by spring exception handler to code 409
        boolean conflict = false;
        try {
            controller.insert(rule2);
        } catch (DuplicateKeyException e) {
            conflict = true;
        }
        assertTrue(conflict);
    }

    @Test
    public void testLimits() {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(5L);
        rule.setUserName("pippo");
        rule.setRoleName("clown");
        rule.setAddressRange("127.0.0.1/32");
        rule.setService("wfs");
        rule.setRequest("getFeature");
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("LIMIT");
        rule.setLimits(new JaxbRule.Limits());
        rule.getLimits().setAllowedArea(GML3MockData.multiPolygon());
        rule.getLimits().setCatalogMode("MIXED");

        long id = prepareGeoFenceTestRules(rule);

        Rule realRule = adminService.get(id);

        assertEquals(
                rule.getLimits().getCatalogMode(),
                realRule.getRuleLimits().getCatalogMode().toString());
        try {
            String allowedArea = rule.getLimits().getAllowedArea();
            Geometry multiPolygon = new WKTReader().read(allowedArea.split(";")[1]);
            multiPolygon.setSRID(4326);
            assertEquals(multiPolygon, realRule.getRuleLimits().getAllowedArea());
        } catch (ParseException e) {
            fail(e.getLocalizedMessage());
        }

        rule.getLimits().setCatalogMode("HIDE");

        controller.update(id, rule);

        realRule = adminService.get(id);

        assertEquals(
                rule.getLimits().getCatalogMode(),
                realRule.getRuleLimits().getCatalogMode().toString());

        rule.setLimits(null);

        controller.clearAndUpdate(id, rule);

        realRule = adminService.get(id);

        assertNull(realRule.getRuleLimits());
    }

    @Test
    public void testLayerDetails() {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(5L);
        rule.setUserName("pippo");
        rule.setRoleName("clown");
        rule.setAddressRange("127.0.0.1/32");
        rule.setService("wfs");
        rule.setRequest("getFeature");
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("ALLOW");
        rule.setLayerDetails(new JaxbRule.LayerDetails());
        rule.getLayerDetails().setAllowedArea(GML3MockData.multiPolygon());
        rule.getLayerDetails().getAllowedStyles().add("style1");
        rule.getLayerDetails().getAllowedStyles().add("style2");
        JaxbRule.LayerAttribute att = new JaxbRule.LayerAttribute();
        att.setName("layerAttribute1");
        att.setAccessType("READONLY");
        att.setDataType("dataType");
        rule.getLayerDetails().getAttributes().add(att);
        att = new JaxbRule.LayerAttribute();
        att.setName("layerAttribute2");
        att.setAccessType("READONLY");
        att.setDataType("dataType2");
        rule.getLayerDetails().getAttributes().add(att);
        rule.getLayerDetails().setCatalogMode("MIXED");
        rule.getLayerDetails().setCqlFilterRead("myFilterRead");
        rule.getLayerDetails().setCqlFilterWrite("myFilterWrite");
        rule.getLayerDetails().setDefaultStyle("myDefaultStyle");
        rule.getLayerDetails().setLayerType("VECTOR");

        long id = prepareGeoFenceTestRules(rule);

        Rule realRule = adminService.get(id);
        try {
            String allowedArea = rule.getLayerDetails().getAllowedArea();
            Geometry multiPolygon = new WKTReader().read(allowedArea.split(";")[1]);
            multiPolygon.setSRID(4326);
            assertEquals(multiPolygon, realRule.getLayerDetails().getArea());
        } catch (ParseException e) {
            fail(e.getLocalizedMessage());
        }
        assertEquals(
                rule.getLayerDetails().getCatalogMode(),
                realRule.getLayerDetails().getCatalogMode().toString());
        assertEquals(
                rule.getLayerDetails().getAllowedStyles(),
                realRule.getLayerDetails().getAllowedStyles());
        assertEquals(2, realRule.getLayerDetails().getAttributes().size());
        for (LayerAttribute la : realRule.getLayerDetails().getAttributes()) {
            if (la.getName().equals("layerAttribute2")) {
                assertEquals("READONLY", la.getAccess().toString());
            }
        }
        assertEquals(
                rule.getLayerDetails().getCqlFilterRead(),
                realRule.getLayerDetails().getCqlFilterRead());
        assertEquals(
                rule.getLayerDetails().getCqlFilterWrite(),
                realRule.getLayerDetails().getCqlFilterWrite());
        assertEquals(
                rule.getLayerDetails().getDefaultStyle(),
                realRule.getLayerDetails().getDefaultStyle());
        assertEquals(
                rule.getLayerDetails().getLayerType(),
                realRule.getLayerDetails().getType().toString());

        rule.getLayerDetails().setDefaultStyle("myDefaultStyle2");

        rule.getLayerDetails().getAttributes().clear();
        att = new JaxbRule.LayerAttribute();
        att.setName("layerAttribute2");
        att.setAccessType("READWRITE");
        att.setDataType("dataType");
        rule.getLayerDetails().getAttributes().add(att);
        att = new JaxbRule.LayerAttribute();
        att.setName("layerAttribute3");
        att.setAccessType("READWRITE");
        att.setDataType("dataType");
        rule.getLayerDetails().getAttributes().add(att);

        rule.getLayerDetails().getAllowedStyles().clear();
        rule.getLayerDetails().getAllowedStyles().add("style3");

        controller.update(id, rule);

        realRule = adminService.get(id);

        assertEquals(
                rule.getLayerDetails().getDefaultStyle(),
                realRule.getLayerDetails().getDefaultStyle());

        assertEquals(1, realRule.getLayerDetails().getAllowedStyles().size());
        assertEquals(2, realRule.getLayerDetails().getAttributes().size());

        for (LayerAttribute la : realRule.getLayerDetails().getAttributes()) {
            if (la.getName().equals("layerAttribute2")) {
                assertEquals("READWRITE", la.getAccess().toString());
            }
        }

        controller.clearAndUpdate(id, rule);

        realRule = adminService.get(id);

        assertEquals(
                rule.getLayerDetails().getAllowedStyles(),
                realRule.getLayerDetails().getAllowedStyles());
        assertEquals(2, realRule.getLayerDetails().getAttributes().size());
    }

    @Test
    public void testMovingRules() {
        // create some rules for the test
        String prefix = UUID.randomUUID().toString();
        adminService.insert(new Rule(
                5, prefix + "-user5", prefix + "-role1", null, null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(
                2, prefix + "-user2", prefix + "-role1", null, null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(
                1, prefix + "-user1", prefix + "-role1", null, null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(
                4, prefix + "-user4", prefix + "-role2", null, null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(
                3, prefix + "-user3", prefix + "-role2", null, null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(
                6, prefix + "-user6", prefix + "-role6", null, null, null, null, null, null, null, GrantType.ALLOW));
        // get the rules so we can access their id
        JaxbRuleList originalRules = controller.get(
                0, 6, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);
        validateRules(originalRules, prefix, "user1", "user2", "user3", "user4", "user5", "user6");
        // check rules per page
        validateRules(0, prefix, "user1", "user2");
        validateRules(0, 1, 2);
        validateRules(1, prefix, "user3", "user4");
        validateRules(1, 3, 4);
        validateRules(2, prefix, "user5", "user6");
        validateRules(2, 5, 6);
        // moving rules for user1 and user2 to the last page
        ResponseEntity<JaxbRuleList> result = controller.move(
                7,
                originalRules.getRules().get(0).getId()
                        + ","
                        + originalRules.getRules().get(1).getId());
        validateResult(result, HttpStatus.OK, 2);
        validateRules(result.getBody(), prefix, "user1", "user2");
        validateRules(result.getBody(), 7L, 8L);
        // check rules per page
        validateRules(0, prefix, "user3", "user4");
        validateRules(0, 3, 4);
        validateRules(1, prefix, "user5", "user6");
        validateRules(1, 5, 6);
        validateRules(2, prefix, "user1", "user2");
        validateRules(2, 7, 8);
        // moving rules for user3 and user4 to the second page
        result = controller.move(
                7,
                originalRules.getRules().get(2).getId()
                        + ","
                        + originalRules.getRules().get(3).getId());
        validateResult(result, HttpStatus.OK, 2);
        validateRules(result.getBody(), prefix, "user3", "user4");
        validateRules(result.getBody(), 7L, 8L);
        // check rules per page
        validateRules(0, prefix, "user5", "user6");
        validateRules(0, 5, 6);
        validateRules(1, prefix, "user3", "user4");
        validateRules(1, 7, 8);
        validateRules(2, prefix, "user1", "user2");
        validateRules(2, 9, 10);
        // moving rule for user1 to first page
        result = controller.move(
                5, String.valueOf(originalRules.getRules().get(0).getId()));
        validateResult(result, HttpStatus.OK, 1);
        validateRules(result.getBody(), prefix, "user1");
        validateRules(result.getBody(), 5L);
        // check rules per page
        validateRules(0, prefix, "user1", "user5");
        validateRules(0, 5, 6);
        validateRules(1, prefix, "user6", "user3");
        validateRules(1, 7, 8);
        validateRules(2, prefix, "user4", "user2");
        validateRules(2, 9, 11);
        // moving rules for user2 and user 3 to first and second page
        result = controller.move(
                6,
                originalRules.getRules().get(1).getId()
                        + ","
                        + originalRules.getRules().get(2).getId());
        validateResult(result, HttpStatus.OK, 2);
        validateRules(result.getBody(), prefix, "user3", "user2");
        validateRules(result.getBody(), 6L, 7L);
        // check rules per page
        validateRules(0, prefix, "user1", "user3");
        validateRules(0, 5, 6);
        validateRules(1, prefix, "user2", "user5");
        validateRules(1, 7, 8);
        validateRules(2, prefix, "user6", "user4");
        validateRules(2, 9, 11);
    }

    @Test
    public void testRestControllerPaths() throws Exception {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(5L);
        rule.setUserName("pippo");
        rule.setRoleName("clown");
        rule.setAddressRange("127.0.0.1/32");
        rule.setService("wfs");
        rule.setRequest("getFeature");
        rule.setSubfield(null);
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("ALLOW");

        long id = prepareGeoFenceTestRules(rule);

        JSONObject json = (JSONObject) getAsJSON(RestBaseController.ROOT_PATH + "/geofence/rules.json", 200);
        // print(json);

        assertNotNull(id);
        assertEquals(1, json.getInt("count"));

        json = (JSONObject) getAsJSON(RestBaseController.ROOT_PATH + "/geofence/rules.json", 200);
        // print(json);

        assertEquals(1, json.getInt("count"));

        JSONArray jsonRules = json.getJSONArray("rules");
        // print(jsonRules);

        assertNotNull(jsonRules);
        assertEquals(1, jsonRules.size());

        final String jsonRuleBody = "{\n"
                + "  'Rule': {\n"
                + "    'priority': 0,\n"
                + "    'userName': null,\n"
                + "    'roleName': null,\n"
                + "    'addressRange': null,\n"
                + "    'workspace': 'geonode',\n"
                + "    'layer': 'DE_USNG_UTM18',\n"
                + "    'service': null,\n"
                + "    'request': null,\n"
                + "    'subfield': null,\n"
                + "    'access': 'ALLOW',\n"
                + "    'limits': null,\n"
                + "    'layerDetails': {\n"
                + "      'layerType': 'VECTOR',\n"
                + "      'defaultStyle': 'DE_USNG_UTM18',\n"
                + "      'cqlFilterRead': 'Northings >= 100',\n"
                + "      'cqlFilterWrite': null,\n"
                + "      'allowedArea': 'SRID=4326;MULTIPOLYGON (((-180 -90, -180 90, 180 90, 180 -90, -180 -90)))',\n"
                + "      'catalogMode': null,\n"
                + "      'allowedStyles': [],\n"
                + "      'attributes': [\n"
                + "        {\n"
                + "          'name': 'Eastings',\n"
                + "          'dataType': 'java.lang.String',\n"
                + "          'accessType': 'READWRITE'\n"
                + "        },\n"
                + "        {\n"
                + "          'name': 'the_geom',\n"
                + "          'dataType': 'org.locationtech.jts.geom.MultiPolygon',\n"
                + "          'accessType': 'READONLY'\n"
                + "        },\n"
                + "        {\n"
                + "          'name': 'GRID1MIL',\n"
                + "          'dataType': 'java.lang.String',\n"
                + "          'accessType': 'NONE'\n"
                + "        },\n"
                + "        {\n"
                + "          'name': 'GRID100K',\n"
                + "          'dataType': 'java.lang.String',\n"
                + "          'accessType': 'READONLY'\n"
                + "        },\n"
                + "        {\n"
                + "          'name': 'Northings',\n"
                + "          'dataType': 'java.lang.String',\n"
                + "          'accessType': 'NONE'\n"
                + "        },\n"
                + "        {\n"
                + "          'name': 'USNG',\n"
                + "          'dataType': 'java.lang.String',\n"
                + "          'accessType': 'NONE'\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  }\n"
                + "}";

        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/geofence/rules", jsonRuleBody, "text/json");
        assertEquals(201, response.getStatus());

        json = (JSONObject) getAsJSON(RestBaseController.ROOT_PATH + "/geofence/rules.json", 200);
        // print(json);

        assertEquals(2, json.getInt("count"));

        jsonRules = json.getJSONArray("rules");
        // print(jsonRules);

        assertNotNull(jsonRules);
        assertEquals(2, jsonRules.size());

        JSONObject jsonRule = null;
        for (Object jsonObj : jsonRules) {
            assertNotNull(jsonObj);
            assertTrue(jsonObj instanceof JSONObject);
            jsonRule = (JSONObject) jsonObj;
            print(jsonRule);

            if (jsonRule.getString("layer").equals("DE_USNG_UTM18")) {
                assertEquals("geonode", jsonRule.getString("workspace"));
                assertEquals("DE_USNG_UTM18", jsonRule.getString("layer"));
                assertEquals("ALLOW", jsonRule.getString("access"));

                JSONObject layerDetails = jsonRule.getJSONObject("layerDetails");
                assertNotNull(layerDetails);
                assertEquals("VECTOR", layerDetails.getString("layerType"));
                assertEquals("DE_USNG_UTM18", layerDetails.getString("defaultStyle"));
                assertEquals("Northings >= 100", layerDetails.getString("cqlFilterRead"));
                assertEquals(
                        "SRID=4326;MULTIPOLYGON (((-180 -90, -180 90, 180 90, 180 -90, -180 -90)))",
                        layerDetails.getString("allowedArea"));
                break;
            } else {
                jsonRule = null;
            }
        }

        assertNotNull(jsonRule);

        json = (JSONObject)
                getAsJSON(RestBaseController.ROOT_PATH + "/geofence/rules/id/" + jsonRule.getInt("id") + ".json", 200);
        // print(json);

        assertEquals(json.toString(), jsonRule.toString());

        response =
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/geofence/rules/id/" + jsonRule.getInt("id"));
        assertEquals(200, response.getStatus());

        json = (JSONObject) getAsJSON(RestBaseController.ROOT_PATH + "/geofence/rules.json", 200);
        // print(json);

        assertEquals(1, json.getInt("count"));
    }

    @Test
    public void testLimitsSRIDAndSpatialFilterType() {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(5L);
        rule.setAccess("LIMIT");
        rule.setLimits(new JaxbRule.Limits());
        MultiPolygon mPoly = GML3MockData.multiPolygon();
        mPoly.setSRID(3003);
        rule.getLimits().setAllowedArea(mPoly);
        rule.getLimits().setSpatialFilterType("CLIP");
        rule.getLimits().setCatalogMode("MIXED");

        long id = prepareGeoFenceTestRules(rule);

        Rule realRule = adminService.get(id);

        assertEquals(
                rule.getLimits().getCatalogMode(),
                realRule.getRuleLimits().getCatalogMode().toString());

        assertEquals(realRule.getRuleLimits().getAllowedArea().getSRID(), 3003);

        assertTrue(rule.getLimits().getAllowedArea().contains("SRID=3003"));

        assertEquals(
                rule.getLimits().getSpatialFilterType(),
                realRule.getRuleLimits().getSpatialFilterType().toString());
        rule.getLimits().setSpatialFilterType("INTERSECT");

        controller.update(id, rule);

        realRule = adminService.get(id);

        assertEquals(
                rule.getLimits().getSpatialFilterType(),
                realRule.getRuleLimits().getSpatialFilterType().toString());
        JaxbRuleList list = controller.get(
                null, null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
        JaxbRule r = list.getRules().get(0);
        JaxbRule.Limits limits = r.getLimits();
        assertEquals(limits.getSpatialFilterType(), "INTERSECT");
        assertTrue(limits.getAllowedArea().contains("SRID"));
    }

    @Test
    public void testLayerDetailSRIDAndSpatialFilterType() {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(5L);
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("ALLOW");
        rule.setLayerDetails(new JaxbRule.LayerDetails());
        MultiPolygon multiPolygon = GML3MockData.multiPolygon();
        multiPolygon.setSRID(3002);
        rule.getLayerDetails().setAllowedArea(multiPolygon);
        rule.getLayerDetails().setSpatialFilterType("INTERSECT");
        rule.getLayerDetails().setDefaultStyle("myDefaultStyle");
        rule.getLayerDetails().setLayerType("VECTOR");

        long id = prepareGeoFenceTestRules(rule);

        Rule realRule = adminService.get(id);
        assertEquals(realRule.getLayerDetails().getArea().getSRID(), 3002);
        assertTrue(rule.getLayerDetails().getAllowedArea().contains("SRID=3002"));
        assertEquals(
                rule.getLayerDetails().getSpatialFilterType(),
                realRule.getLayerDetails().getSpatialFilterType().toString());

        rule.getLayerDetails().setSpatialFilterType("CLIP");
        controller.update(id, rule);

        realRule = adminService.get(id);
        assertEquals(
                rule.getLayerDetails().getSpatialFilterType(),
                realRule.getLayerDetails().getSpatialFilterType().toString());
        JaxbRuleList list = controller.get(
                null, null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
        JaxbRule r = list.getRules().get(0);
        JaxbRule.LayerDetails details = r.getLayerDetails();
        assertEquals(details.getSpatialFilterType(), "CLIP");
        assertTrue(details.getAllowedArea().contains("SRID"));
    }

    @Test
    public void testGetByIdWithNullLayerTypeAndAllowedArea() {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(7L);
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("ALLOW");
        rule.setLayerDetails(new JaxbRule.LayerDetails());
        long id = prepareGeoFenceTestRules(rule);
        JaxbRule r = controller.get(id);
        assertNotNull(r);
        assertNotNull(r.getLayerDetails());
        assertNull(r.getLayerDetails().getLayerType());
        JaxbRuleList list = controller.get(
                null, null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
        r = list.getRules().get(0);
        assertNotNull(r);
        assertNotNull(r.getLayerDetails());
        assertNull(r.getLayerDetails().getLayerType());
    }

    @Test
    public void testLocalPersisterDeserialization() throws IOException {
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        GeoFenceServerXStreamInitializer initializer = new GeoFenceServerXStreamInitializer();
        xpf.addInitializer(initializer);

        XStreamPersister persister = xpf.createXMLPersister();
        controller.configurePersister(persister, null);
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Rule>"
                + "<access>LIMIT</access>"
                + "<layer>DE_USNG_UTM18</layer>"
                + "<limits>"
                + "     <allowedArea>SRID=4326;MULTIPOLYGON (((-75 -90, -75 90, 75 90, 75 -90, -75 -90)))</allowedArea>"
                + "     <catalogMode>HIDDEN</catalogMode>"
                + "</limits>"
                + "<priority>1</priority>"
                + "<workspace>geonode</workspace>"
                + "</Rule>";

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(UTF_8));

        JaxbRule rule = persister.load(bais, JaxbRule.class);

        assertNotNull(rule);

        assertEquals("LIMIT", rule.getAccess());
        assertEquals("DE_USNG_UTM18", rule.getLayer());
        assertEquals("geonode", rule.getWorkspace());
        assertEquals(1, rule.getPriority().intValue());

        assertNotNull(rule.getLimits());

        assertEquals(
                "SRID=4326;MULTIPOLYGON (((-75 -90, -75 90, 75 90, 75 -90, -75 -90)))",
                rule.getLimits().getAllowedArea());

        assertEquals("HIDDEN", rule.getLimits().getCatalogMode());
    }

    @Test
    public void testRulesXMLPayload() throws Exception {

        this.adminService.getAll().stream()
                .mapToLong(ShortRule::getId)
                .peek(id -> LOGGER.warning("deleting " + id))
                .forEach(adminService::delete);

        JaxbRule rule = new JaxbRule();
        rule.setPriority(7L);
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("ALLOW");
        rule.setRoleName("ROLE_EDITOR");
        rule.setLayerDetails(new JaxbRule.LayerDetails());
        final long id = prepareGeoFenceTestRules(rule);

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" //
                + "<Rules count=\"1\">" //
                + "<Rule id=\""
                + id
                + "\">" //
                + "<access>ALLOW</access>" //
                + "<layer>layer</layer>" //
                + "<layerDetails>" //
                + "<spatialFilterType>INTERSECT</spatialFilterType>" //
                + "</layerDetails>" //
                + "<priority>7</priority>" //
                + "<roleName>ROLE_EDITOR</roleName>" //
                + "<workspace>workspace</workspace>" //
                + "</Rule>" //
                + "</Rules>";

        String response = super.getAsString("/rest/geofence/rules");
        XMLAssert.assertXMLEqual(expected, response);
    }

    @Test
    public void testIpFiltering() {

        // Clean up
        for (ShortRule r : adminService.getAll()) {
            adminService.delete(r.getId());
        }

        IPAddressRange r1 = new IPAddressRange("192.168.0.0/16");
        IPAddressRange r2 = new IPAddressRange("192.168.10.0/24");
        IPAddressRange r3 = new IPAddressRange("10.0.0.0/8");

        adminService.insert(new Rule(1, null, null, null, null, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(2, null, null, null, r1, null, null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(3, null, null, null, r1, "s1", null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(4, null, null, null, r2, "s2", null, null, null, null, GrantType.ALLOW));
        adminService.insert(new Rule(5, null, null, null, r3, null, null, null, null, null, GrantType.ALLOW));

        validateRules(getByIpService(null, null, null, null), 1l, 2l, 3l, 4l, 5l);
        validateRules(getByIpService("1.1.1.1", true, null, null), 1l); // NOPMD
        validateRules(getByIpService("1.1.1.1", false, null, null)); // NOPMD
        validateRules(getByIpService("192.168.1.1", true, null, null), 1l, 2l, 3l); // NOPMD
        validateRules(getByIpService("192.168.1.1", false, null, null), 2l, 3l); // NOPMD
        validateRules(getByIpService("192.168.1.1", true, "s2", true), 1l, 2l); // NOPMD
        validateRules(getByIpService("192.168.1.1", false, null, null), 2l, 3l); // NOPMD
        validateRules(getByIpService("192.168.10.1", true, null, null), 1l, 2l, 3l, 4); // NOPMD
    }

    JaxbRuleList getByIpService(String ip, Boolean ipIncludeDefault, String service, Boolean serviceIncludeDefault) {
        return controller.get(
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ip,
                ipIncludeDefault,
                service,
                serviceIncludeDefault,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /** Helper method that checks if the rule already exists and create a new one by returning its ID. */
    protected long prepareGeoFenceTestRules(JaxbRule rule) {
        if (adminService.getCountAll() > 0) {
            for (ShortRule r : adminService.getAll()) {
                controller.delete(r.getId());
            }
        }

        long id = Long.parseLong(controller.insert(rule));
        return id;
    }

    /** Helper method that will validate a move result. */
    private void validateResult(ResponseEntity<JaxbRuleList> result, HttpStatus expectedHttpStatus, int rules) {
        assertThat(result, notNullValue());
        assertThat(result.getStatusCode(), is(expectedHttpStatus));
        if (rules > 0) {
            assertThat(result.getBody(), notNullValue());
            assertThat(result.getBody().getRules().size(), is(rules));
        } else {
            assertThat(result.getBody(), nullValue());
        }
    }

    /** Helper method that will validate the rules present in a certain page based on the user id. */
    private void validateRules(int page, String prefix, String... expectedUsers) {
        JaxbRuleList rules = controller.get(
                page, 2, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
        validateRules(rules, prefix, expectedUsers);
    }

    /** Helper method that will validate that the provided rules will match the provided user ids. */
    private void validateRules(JaxbRuleList rules, String prefix, String... expectedUsers) {
        assertThat(rules, notNullValue());
        assertThat(rules.getRules(), notNullValue());
        assertThat(rules.getRules().size(), is(expectedUsers.length));
        for (int i = 0; i < expectedUsers.length; i++) {
            assertThat(rules.getRules().get(i).getUserName(), is(prefix + "-" + expectedUsers[i]));
        }
    }

    /** Helper method that will validate the rules present in a certain page based on the priority. */
    private void validateRules(int page, long... expectedPriorities) {
        JaxbRuleList rules = controller.get(
                page, 2, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
        validateRules(rules, expectedPriorities);
    }

    /** Helper method that will validate that the provided rules will match the provided priorities. */
    private void validateRules(JaxbRuleList rules, long... expectedPriorities) {
        assertThat(rules, notNullValue());
        assertThat(rules.getRules(), notNullValue());
        assertThat(rules.getRules().size(), is(expectedPriorities.length));
        for (int i = 0; i < expectedPriorities.length; i++) {
            assertThat(rules.getRules().get(i).getPriority(), is(expectedPriorities[i]));
        }
    }
}
