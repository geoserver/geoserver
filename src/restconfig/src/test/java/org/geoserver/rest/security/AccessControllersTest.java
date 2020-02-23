/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import net.sf.json.JSONObject;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.CatalogMode;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Test for {@link DataAccessController},{@link ServiceAccessController} and {@link
 * RestAccessController}
 *
 * @author christian
 */
@TestSetup(run = TestSetupFrequency.REPEAT)
public class AccessControllersTest extends SecurityRESTTestSupport {

    static final String BASE_URI = RestBaseController.ROOT_PATH + "/security/acl/";

    static final String DATA_URI = BASE_URI + "layers";

    static final String DATA_URI_XML = DATA_URI + ".xml";

    static final String DATA_URI_JSON = DATA_URI + ".json";

    static final String SERVICE_URI = BASE_URI + "services";

    static final String SERVICE_URI_XML = SERVICE_URI + ".xml";

    static final String SERVICE_URI_JSON = SERVICE_URI + ".json";

    static final String REST_URI = BASE_URI + "rest";

    static final String REST_URI_XML = REST_URI + ".xml";

    static final String REST_URI_JSON = REST_URI + ".json";

    static final String CATALOG_URI = BASE_URI + "catalog";

    static final String CATALOG_URI_XML = CATALOG_URI + ".xml";

    static final String CATALOG_URI_JSON = CATALOG_URI + ".json";

    private static final String TEST_ROLE1 = "TEST_ROLE1";

    private static final String TEST_ROLE2 = "TEST_ROLE2";

    private static final String TEST_ROLELIST = TEST_ROLE1 + "," + TEST_ROLE2;

    String createXMLBody(String[][] rules) {
        StringBuilder buff = new StringBuilder();
        buff.append("<").append(RuleMapXMLConverter.ROOTELEMENT).append(">\n");
        for (String[] rule : rules) {
            buff.append("<").append(RuleMapXMLConverter.RULEELEMENT).append(" ");
            buff.append(RuleMapXMLConverter.RESOURCEATTR).append("=\"").append(rule[0]);
            buff.append("\">");
            buff.append(rule[1]);
            buff.append("</").append(RuleMapXMLConverter.RULEELEMENT).append(">\n");
        }
        buff.append("</").append(RuleMapXMLConverter.ROOTELEMENT).append(">\n");
        return buff.toString();
    }

    String createJSONBody(String[][] rules) {
        JSONObject json = new JSONObject();
        for (String[] rule : rules) {
            json.put(rule[0], rule[1]);
        }
        return json.toString(1);
    }

    void checkXMLResponse(Document dom, String[][] rules) throws XpathException {
        assertEquals(RuleMapXMLConverter.ROOTELEMENT, dom.getDocumentElement().getNodeName());
        assertEquals(
                rules.length,
                dom.getDocumentElement()
                        .getElementsByTagName(RuleMapXMLConverter.RULEELEMENT)
                        .getLength());
        String pattern =
                "/"
                        + RuleMapXMLConverter.ROOTELEMENT
                        + "/"
                        + RuleMapXMLConverter.RULEELEMENT
                        + "[@"
                        + RuleMapXMLConverter.RESOURCEATTR
                        + "='XXX']";
        for (String[] rule : rules) {
            String exp = pattern.replace("XXX", rule[0]);
            String roles = xp.evaluate(exp, dom);
            assertTrue(checkRolesStringsForEquality(rule[1], roles));
        }
    }

    /**
     * Checks role strings for equality
     *
     * <p>e. g. ROLE1,ROLE2 is equal to ROLE2,ROLE1
     */
    boolean checkRolesStringsForEquality(String roleString1, String roleString2) {
        String[] roleArray1 = roleString1.split(",");
        String[] roleArray2 = roleString2.split(",");

        if (roleArray1.length != roleArray2.length) return false;

        Set<String> roleSet1 = new HashSet<>();
        for (String role : roleArray1) roleSet1.add(role.trim());

        Set<String> roleSet2 = new HashSet<>();
        for (String role : roleArray2) roleSet2.add(role.trim());

        for (String role : roleSet1) {
            if (!roleSet2.contains(role)) return false;
        }
        return true;
    }

    void checkJSONResponse(JSONObject json, String[][] rules) {
        for (String[] rule : rules) {
            String roles = json.getString(rule[0]);
            assertTrue(checkRolesStringsForEquality(rule[1], roles));
        }
    }

    String[][] getDefaultLayerRules() {
        return new String[][] {{"*.*.r", "*"}, {"*.*.w", "*"}};
    }

    String[][] getDefaultServiceRules() {
        return new String[][] {{"*.*", "*"}};
    }

    String[][] getDefaultRestRules() {
        return new String[][] {{"/**:GET", "ADMIN"}, {"/**:POST,DELETE,PUT", "ADMIN"}};
    }

    String[][] getDefaultRestRulesForDelete() {
        return new String[][] {{"%2F**:GET", "ADMIN"}, {"%2F**:POST,DELETE,PUT", "ADMIN"}};
    }

    @Test
    public void testGet() throws Exception {

        String[][] layerRules = getDefaultLayerRules();

        JSONObject json = (JSONObject) getAsJSON(DATA_URI_JSON);
        // System.out.println(json.toString(1));
        checkJSONResponse(json, layerRules);

        Document dom = getAsDOM(DATA_URI_XML);
        // print(dom);
        checkXMLResponse(dom, layerRules);

        String[][] serviceRules = getDefaultServiceRules();
        json = (JSONObject) getAsJSON(SERVICE_URI_JSON);
        checkJSONResponse(json, serviceRules);
        dom = getAsDOM(SERVICE_URI_XML);
        checkXMLResponse(dom, serviceRules);

        String[][] restRules = getDefaultRestRules();
        dom = getAsDOM(REST_URI_XML);
        checkXMLResponse(dom, restRules);
        json = (JSONObject) getAsJSON(REST_URI_JSON);
        checkJSONResponse(json, restRules);
    }

    @Test
    public void testDelete() throws Exception {

        String[][] layerRules = getDefaultLayerRules();

        assertEquals(200, deleteAsServletResponse(DATA_URI + "/" + layerRules[0][0]).getStatus());
        assertEquals(404, deleteAsServletResponse(DATA_URI + "/" + layerRules[0][0]).getStatus());

        assertEquals(404, deleteAsServletResponse(SERVICE_URI + "/wfs.getFeature").getStatus());

        String[][] restRules = getDefaultRestRulesForDelete();

        assertEquals(200, deleteAsServletResponse(REST_URI + "/" + restRules[0][0]).getStatus());
        assertEquals(404, deleteAsServletResponse(REST_URI + "/" + restRules[0][0]).getStatus());
    }

    @Test
    public void testXMLPost() throws Exception {

        // layer rules
        String[][] rules = getDefaultLayerRules();
        String[][] toBeAdded = {rules[0], {"ws.layer1.r", TEST_ROLELIST}}; // conflict
        assertEquals(
                409,
                postAsServletResponse(DATA_URI_XML, createXMLBody(toBeAdded), "text/xml")
                        .getStatus());

        // check if nothing changed
        Document dom = getAsDOM(DATA_URI_XML);
        checkXMLResponse(dom, rules);

        // add
        String[][] toBeAdded2 = {{"ws.layer1.w", TEST_ROLE1}, {"ws.layer1.r", TEST_ROLELIST}};
        String[][] expected = {rules[0], rules[1], toBeAdded2[0], toBeAdded2[1]};
        assertEquals(
                200,
                postAsServletResponse(DATA_URI_XML, createXMLBody(toBeAdded2), "text/xml")
                        .getStatus());

        dom = getAsDOM(DATA_URI_XML);
        checkXMLResponse(dom, expected);

        // service rules

        rules = getDefaultServiceRules();
        toBeAdded2 = new String[][] {{"ws.*", TEST_ROLE1}, {"ws2.GetFeature", TEST_ROLELIST}};
        assertEquals(
                200,
                postAsServletResponse(SERVICE_URI_XML, createXMLBody(toBeAdded2), "text/xml")
                        .getStatus());
        expected = new String[][] {rules[0], toBeAdded2[0], toBeAdded2[1]};

        dom = getAsDOM(SERVICE_URI_XML);
        checkXMLResponse(dom, expected);

        // check conflict
        assertEquals(
                409,
                postAsServletResponse(SERVICE_URI_XML, createXMLBody(toBeAdded2), "text/xml")
                        .getStatus());

        // REST rules

        rules = getDefaultRestRules();
        toBeAdded = new String[][] {rules[0], {"/myworkspace/**:GET", TEST_ROLELIST}}; // conflict
        assertEquals(
                409,
                postAsServletResponse(REST_URI_XML, createXMLBody(toBeAdded), "text/xml")
                        .getStatus());

        // check if nothing changed
        dom = getAsDOM(REST_URI_XML);
        checkXMLResponse(dom, rules);

        // add
        toBeAdded2 =
                new String[][] {
                    {"/myworkspace/**:PUT,POST", TEST_ROLE1}, {"/myworkspace/**:GET", TEST_ROLELIST}
                };
        expected = new String[][] {rules[0], rules[1], toBeAdded2[0], toBeAdded2[1]};
        assertEquals(
                200,
                postAsServletResponse(REST_URI_XML, createXMLBody(toBeAdded2), "text/xml")
                        .getStatus());

        dom = getAsDOM(REST_URI_XML);
        checkXMLResponse(dom, expected);
    }

    @Test
    public void testJSONPost() throws Exception {

        // layer rules
        String[][] rules = getDefaultLayerRules();
        String[][] toBeAdded = {rules[0], {"ws.layer1.r", TEST_ROLELIST}}; // conflict
        assertEquals(
                409,
                postAsServletResponse(DATA_URI_JSON, createJSONBody(toBeAdded), "text/json")
                        .getStatus());

        // check if nothing changed
        JSONObject json = (JSONObject) getAsJSON(DATA_URI_JSON);
        checkJSONResponse(json, rules);

        // add
        String[][] toBeAdded2 = {{"ws.layer1.w", TEST_ROLE1}, {"ws.layer1.r", TEST_ROLELIST}};
        String[][] expected = {rules[0], rules[1], toBeAdded2[0], toBeAdded2[1]};
        assertEquals(
                200,
                postAsServletResponse(DATA_URI_JSON, createJSONBody(toBeAdded2), "text/json")
                        .getStatus());

        json = (JSONObject) getAsJSON(DATA_URI_JSON);
        checkJSONResponse(json, expected);

        // service rules

        rules = getDefaultServiceRules();
        toBeAdded2 = new String[][] {{"ws.*", TEST_ROLE1}, {"ws2.GetFeature", TEST_ROLELIST}};
        assertEquals(
                200,
                postAsServletResponse(SERVICE_URI_JSON, createJSONBody(toBeAdded2), "text/json")
                        .getStatus());
        expected = new String[][] {rules[0], toBeAdded2[0], toBeAdded2[1]};

        json = (JSONObject) getAsJSON(SERVICE_URI_JSON);
        checkJSONResponse(json, expected);

        // check conflict
        assertEquals(
                409,
                postAsServletResponse(SERVICE_URI_JSON, createJSONBody(toBeAdded2), "text/json")
                        .getStatus());

        // REST rules

        rules = getDefaultRestRules();
        toBeAdded = new String[][] {rules[0], {"/myworkspace/**:GET", TEST_ROLELIST}}; // conflict
        assertEquals(
                409,
                postAsServletResponse(REST_URI_JSON, createJSONBody(toBeAdded), "text/json")
                        .getStatus());

        // check if nothing changed
        json = (JSONObject) getAsJSON(REST_URI_JSON);
        checkJSONResponse(json, rules);

        // add
        toBeAdded2 =
                new String[][] {
                    {"/myworkspace/**:PUT,POST", TEST_ROLE1}, {"/myworkspace/**:GET", TEST_ROLELIST}
                };
        expected = new String[][] {rules[0], rules[1], toBeAdded2[0], toBeAdded2[1]};
        assertEquals(
                200,
                postAsServletResponse(REST_URI_JSON, createJSONBody(toBeAdded2), "text/json")
                        .getStatus());

        json = (JSONObject) getAsJSON(REST_URI_JSON);
        checkJSONResponse(json, expected);
    }

    @Test
    public void testJSONPut() throws Exception {

        // layer rules
        String[][] rules = getDefaultLayerRules();
        String[][] toBeModified = {rules[0], {"ws.layer1.r", TEST_ROLELIST}}; // conflict
        assertEquals(
                409,
                putAsServletResponse(DATA_URI_JSON, createJSONBody(toBeModified), "text/json")
                        .getStatus());

        // check if nothing changed
        JSONObject json = (JSONObject) getAsJSON(DATA_URI_JSON);
        checkJSONResponse(json, rules);

        // modify
        String[][] toBeModified2 = {{rules[0][0], TEST_ROLE1}, {rules[1][0], TEST_ROLELIST}};
        assertEquals(
                200,
                putAsServletResponse(DATA_URI_JSON, createJSONBody(toBeModified2), "text/json")
                        .getStatus());

        json = (JSONObject) getAsJSON(DATA_URI_JSON);
        checkJSONResponse(json, toBeModified2);

        // service rules

        rules = getDefaultServiceRules();
        toBeModified2 =
                new String[][] {
                    {"ws.*", TEST_ROLE1}, {"ws2.GetFeature", TEST_ROLELIST}
                }; // conflict
        assertEquals(
                409,
                putAsServletResponse(SERVICE_URI_JSON, createJSONBody(toBeModified2), "text/json")
                        .getStatus());

        json = (JSONObject) getAsJSON(SERVICE_URI_JSON);
        checkJSONResponse(json, rules);

        assertEquals(
                200,
                putAsServletResponse(
                                SERVICE_URI_JSON, createJSONBody(new String[][] {}), "text/json")
                        .getStatus());

        // REST rules

        rules = getDefaultRestRules();
        toBeModified =
                new String[][] {rules[0], {"/myworkspace/**:GET", TEST_ROLELIST}}; // conflict
        assertEquals(
                409,
                putAsServletResponse(REST_URI_JSON, createJSONBody(toBeModified), "text/json")
                        .getStatus());

        // check if nothing changed
        json = (JSONObject) getAsJSON(REST_URI_JSON);
        checkJSONResponse(json, rules);

        // modify
        toBeModified2 = new String[][] {{rules[0][0], TEST_ROLE1}, {rules[1][0], TEST_ROLELIST}};
        assertEquals(
                200,
                putAsServletResponse(REST_URI_JSON, createJSONBody(toBeModified2), "text/json")
                        .getStatus());

        json = (JSONObject) getAsJSON(REST_URI_JSON);
        checkJSONResponse(json, toBeModified2);
    }

    @Test
    public void testXMLPut() throws Exception {

        // layer rules
        String[][] rules = getDefaultLayerRules();
        String[][] toBeModified = {rules[0], {"ws.layer1.r", TEST_ROLELIST}}; // conflict
        assertEquals(
                409,
                putAsServletResponse(DATA_URI_XML, createXMLBody(toBeModified), "text/xml")
                        .getStatus());

        // check if nothing changed
        Document dom = getAsDOM(DATA_URI_XML);
        checkXMLResponse(dom, rules);

        // modify
        String[][] toBeModified2 = {{rules[0][0], TEST_ROLE1}, {rules[1][0], TEST_ROLELIST}};
        assertEquals(
                200,
                putAsServletResponse(DATA_URI_XML, createXMLBody(toBeModified2), "text/xml")
                        .getStatus());

        dom = getAsDOM(DATA_URI_XML);
        checkXMLResponse(dom, toBeModified2);

        // service rules

        rules = getDefaultServiceRules();
        toBeModified2 =
                new String[][] {
                    {"ws.*", TEST_ROLE1}, {"ws2.GetFeature", TEST_ROLELIST}
                }; // conflict
        assertEquals(
                409,
                putAsServletResponse(SERVICE_URI_XML, createXMLBody(toBeModified2), "text/xml")
                        .getStatus());

        dom = getAsDOM(SERVICE_URI_XML);
        checkXMLResponse(dom, rules);

        assertEquals(
                200,
                putAsServletResponse(SERVICE_URI_XML, createXMLBody(new String[][] {}), "text/xml")
                        .getStatus());

        // REST rules

        rules = getDefaultRestRules();
        toBeModified =
                new String[][] {rules[0], {"/myworkspace/**:GET", TEST_ROLELIST}}; // conflict
        assertEquals(
                409,
                putAsServletResponse(REST_URI_XML, createXMLBody(toBeModified), "text/xml")
                        .getStatus());

        // check if nothing changed
        dom = getAsDOM(REST_URI_XML);
        checkXMLResponse(dom, rules);

        // modify
        toBeModified2 = new String[][] {{rules[0][0], TEST_ROLE1}, {rules[1][0], TEST_ROLELIST}};
        assertEquals(
                200,
                putAsServletResponse(REST_URI_XML, createXMLBody(toBeModified2), "text/xml")
                        .getStatus());

        dom = getAsDOM(REST_URI_XML);
        checkXMLResponse(dom, toBeModified2);
    }

    @Test
    public void testCatalogMode() throws Exception {
        JSONObject json = (JSONObject) getAsJSON(CATALOG_URI_JSON);
        String mode = (String) json.get(CatalogModeController.MODE_ELEMENT);
        assertEquals(CatalogMode.HIDE.toString(), mode);

        Document dom = getAsDOM(CATALOG_URI_XML);
        print(dom);
        assertEquals(CatalogModeController.XML_ROOT_ELEM, dom.getDocumentElement().getNodeName());
        NodeList nl = dom.getElementsByTagName(CatalogModeController.MODE_ELEMENT);
        assertEquals(1, nl.getLength());
        mode = nl.item(0).getTextContent();
        assertEquals(CatalogMode.HIDE.toString(), mode);

        String jsonTemplate = "'{'\"" + CatalogModeController.MODE_ELEMENT + "\":\"{0}\"'}'";
        String xmlTemplate = "<" + CatalogModeController.XML_ROOT_ELEM + ">" + "\n";
        xmlTemplate += " <" + CatalogModeController.MODE_ELEMENT + ">{0}";
        xmlTemplate += "</" + CatalogModeController.MODE_ELEMENT + ">" + "\n";
        xmlTemplate += "</" + CatalogModeController.XML_ROOT_ELEM + ">" + "\n";

        assertEquals(
                404,
                putAsServletResponse(CATALOG_URI_JSON, "{\"modexxxxx\": \"HIDE\"}", "text/json")
                        .getStatus());
        assertEquals(
                422,
                putAsServletResponse(
                                CATALOG_URI_JSON,
                                MessageFormat.format(jsonTemplate, "ABC"),
                                "text/json")
                        .getStatus());
        assertEquals(
                422,
                putAsServletResponse(
                                CATALOG_URI_XML,
                                MessageFormat.format(xmlTemplate, "ABC"),
                                "text/xml")
                        .getStatus());

        assertEquals(
                200,
                putAsServletResponse(
                                CATALOG_URI_JSON,
                                MessageFormat.format(jsonTemplate, CatalogMode.MIXED.toString()),
                                "text/json")
                        .getStatus());
        json = (JSONObject) getAsJSON(CATALOG_URI_JSON);
        mode = (String) json.get(CatalogModeController.MODE_ELEMENT);
        assertEquals(CatalogMode.MIXED.toString(), mode);

        assertEquals(
                200,
                putAsServletResponse(
                                CATALOG_URI_XML,
                                MessageFormat.format(xmlTemplate, CatalogMode.CHALLENGE.toString()),
                                "text/xml")
                        .getStatus());
        dom = getAsDOM(CATALOG_URI_XML);
        nl = dom.getElementsByTagName(CatalogModeController.MODE_ELEMENT);
        mode = nl.item(0).getTextContent();
        assertEquals(CatalogMode.CHALLENGE.toString(), mode);
    }

    @Test
    public void testCatalogModeXXE() throws Exception {
        String resource = getClass().getResource("secret.txt").toExternalForm();
        String xml = "<!DOCTYPE " + CatalogModeController.XML_ROOT_ELEM + " [";
        xml += "<!ELEMENT " + CatalogModeController.XML_ROOT_ELEM + " ANY>";
        xml += "<!ENTITY xxe SYSTEM \"" + resource + "\">]>";
        xml += "<" + CatalogModeController.XML_ROOT_ELEM + ">" + "\n";
        xml += " <" + CatalogModeController.MODE_ELEMENT + ">&xxe;";
        xml += "</" + CatalogModeController.MODE_ELEMENT + ">" + "\n";
        xml += "</" + CatalogModeController.XML_ROOT_ELEM + ">" + "\n";

        MockHttpServletResponse resp = putAsServletResponse(CATALOG_URI_XML, xml, "text/xml");
        assertEquals(400, resp.getStatus());
        assertThat(resp.getContentAsString(), not(containsString("HELLO WORLD")));
    }

    @Test
    public void testInvalidRules() throws Exception {

        // layer rules

        String[][] rules = getDefaultLayerRules();
        String[][] toBeAdded = {{"ws.layer1.r.c", TEST_ROLELIST}};
        assertEquals(
                422,
                postAsServletResponse(DATA_URI_XML, createXMLBody(toBeAdded), "text/xml")
                        .getStatus());
        // check if nothing changed
        Document dom = getAsDOM(DATA_URI_XML);
        checkXMLResponse(dom, rules);

        toBeAdded = new String[][] {{"ws.layer1.x", TEST_ROLELIST}}; // conflict
        assertEquals(
                422,
                postAsServletResponse(DATA_URI_XML, createXMLBody(toBeAdded), "text/xml")
                        .getStatus());
        // check if nothing changed
        dom = getAsDOM(DATA_URI_XML);
        checkXMLResponse(dom, rules);

        toBeAdded = new String[][] {{"*.layer1.r", TEST_ROLELIST}}; // conflict
        assertEquals(
                422,
                postAsServletResponse(DATA_URI_XML, createXMLBody(toBeAdded), "text/xml")
                        .getStatus());
        // check if nothing changed
        dom = getAsDOM(DATA_URI_XML);
        checkXMLResponse(dom, rules);

        toBeAdded = new String[][] {{"ws.layer1.a", TEST_ROLELIST}}; // conflict
        assertEquals(
                422,
                postAsServletResponse(DATA_URI_XML, createXMLBody(toBeAdded), "text/xml")
                        .getStatus());
        // check if nothing changed
        dom = getAsDOM(DATA_URI_XML);
        checkXMLResponse(dom, rules);

        // services
        rules = getDefaultServiceRules();
        toBeAdded = new String[][] {{"ws.getMap.c", TEST_ROLELIST}}; // conflict
        assertEquals(
                422,
                postAsServletResponse(SERVICE_URI_XML, createXMLBody(toBeAdded), "text/xml")
                        .getStatus());
        // check if nothing changed
        dom = getAsDOM(SERVICE_URI_XML);
        checkXMLResponse(dom, rules);

        toBeAdded = new String[][] {{"*.getMap", TEST_ROLELIST}}; // conflict
        assertEquals(
                422,
                postAsServletResponse(SERVICE_URI_XML, createXMLBody(toBeAdded), "text/xml")
                        .getStatus());
        // check if nothing changed
        dom = getAsDOM(SERVICE_URI_XML);
        checkXMLResponse(dom, rules);

        rules = getDefaultRestRules();
        toBeAdded = new String[][] {rules[0], {"/myworkspace/**!!!GET", TEST_ROLELIST}}; // conflict
        assertEquals(
                422,
                postAsServletResponse(REST_URI_XML, createXMLBody(toBeAdded), "text/xml")
                        .getStatus());

        // check if nothing changed
        dom = getAsDOM(REST_URI_XML);
        checkXMLResponse(dom, rules);
    }

    @Test
    public void testNotAuthorized() throws Exception {
        logout();

        assertEquals(403, getAsServletResponse(DATA_URI_XML).getStatus());
        assertEquals(403, getAsServletResponse(DATA_URI_JSON).getStatus());
        assertEquals(403, getAsServletResponse(SERVICE_URI_XML).getStatus());
        assertEquals(403, getAsServletResponse(SERVICE_URI_JSON).getStatus());
        assertEquals(403, getAsServletResponse(REST_URI_XML).getStatus());
        assertEquals(403, getAsServletResponse(REST_URI_JSON).getStatus());

        // layer rules
        String[][] dataRules = {{"ws.layer1.w", TEST_ROLE1}, {"ws.layer1.r", TEST_ROLELIST}};
        String[][] serviceRules =
                new String[][] {{"ws.*", TEST_ROLE1}, {"ws2.GetFeature", TEST_ROLELIST}};
        String[][] restRules = new String[][] {{"/myworkspace/**:GET", TEST_ROLELIST}}; // conflict

        assertEquals(
                403,
                putAsServletResponse(DATA_URI_XML, createXMLBody(dataRules), "text/xml")
                        .getStatus());
        assertEquals(
                403,
                putAsServletResponse(DATA_URI_JSON, createJSONBody(dataRules), "text/json")
                        .getStatus());
        assertEquals(
                403,
                putAsServletResponse(SERVICE_URI_XML, createXMLBody(serviceRules), "text/xml")
                        .getStatus());
        assertEquals(
                403,
                putAsServletResponse(SERVICE_URI_JSON, createJSONBody(serviceRules), "text/json")
                        .getStatus());
        assertEquals(
                403,
                putAsServletResponse(REST_URI_XML, createXMLBody(restRules), "text/xml")
                        .getStatus());
        assertEquals(
                403,
                putAsServletResponse(REST_URI_JSON, createJSONBody(restRules), "text/json")
                        .getStatus());

        assertEquals(
                403,
                postAsServletResponse(DATA_URI_XML, createXMLBody(dataRules), "text/xml")
                        .getStatus());
        assertEquals(
                403,
                postAsServletResponse(DATA_URI_JSON, createJSONBody(dataRules), "text/json")
                        .getStatus());
        assertEquals(
                403,
                postAsServletResponse(SERVICE_URI_XML, createXMLBody(serviceRules), "text/xml")
                        .getStatus());
        assertEquals(
                403,
                postAsServletResponse(SERVICE_URI_JSON, createJSONBody(serviceRules), "text/json")
                        .getStatus());
        assertEquals(
                403,
                postAsServletResponse(REST_URI_XML, createXMLBody(restRules), "text/xml")
                        .getStatus());
        assertEquals(
                403,
                postAsServletResponse(REST_URI_JSON, createJSONBody(restRules), "text/json")
                        .getStatus());

        assertEquals(403, deleteAsServletResponse(DATA_URI + "/fakerule").getStatus());
        assertEquals(403, deleteAsServletResponse(SERVICE_URI + "/fakerule").getStatus());
        assertEquals(403, deleteAsServletResponse(REST_URI + "/fakerule").getStatus());

        String jsonTemplate = "{\"" + CatalogModeController.MODE_ELEMENT + "\":\"MIXED\"}";
        String xmlTemplate = "<" + CatalogModeController.XML_ROOT_ELEM + ">" + "\n";
        xmlTemplate += " <" + CatalogModeController.MODE_ELEMENT + ">MIXED";
        xmlTemplate += "</" + CatalogModeController.MODE_ELEMENT + ">" + "\n";
        xmlTemplate += "</" + CatalogModeController.XML_ROOT_ELEM + ">" + "\n";
        assertEquals(403, getAsServletResponse(CATALOG_URI_XML).getStatus());
        assertEquals(403, getAsServletResponse(CATALOG_URI_JSON).getStatus());
        assertEquals(
                403, putAsServletResponse(CATALOG_URI_XML, xmlTemplate, "text/xml").getStatus());
        assertEquals(
                403, putAsServletResponse(CATALOG_URI_JSON, jsonTemplate, "text/json").getStatus());
    }
}
