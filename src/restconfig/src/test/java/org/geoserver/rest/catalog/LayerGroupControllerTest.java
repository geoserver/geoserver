/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.*;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class LayerGroupControllerTest extends CatalogRESTTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addStyle(
                "singleStyleGroup",
                "singleStyleGroup.sld",
                CatalogIntegrationTest.class,
                getCatalog());
    }

    @Before
    public void revertChanges() throws Exception {
        removeLayer("sf", "Lakes");
        removeLayerGroup(null, "nestedLayerGroupTest");
        removeLayerGroup(null, "citeLayerGroup");
        removeLayerGroup(null, "sfLayerGroup");
        removeLayerGroup("sf", "workspaceLayerGroup");
        removeLayerGroup(null, "newLayerGroup");
        removeLayerGroup(null, "newLayerGroupWithTypeCONTAINER");
        removeLayerGroup(null, "newLayerGroupWithTypeEO");
        removeLayerGroup(null, "newLayerGroupWithStyleGroup");
        removeLayerGroup(null, "doubleLayerGroup");

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("sfLayerGroup");
        lg.getLayers().add(catalog.getLayerByName("sf:PrimitiveGeoFeature"));
        lg.getLayers().add(catalog.getLayerByName("sf:AggregateGeoFeature"));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));
        catalog.add(lg);

        LayerGroupInfo lg2 = catalog.getFactory().createLayerGroup();
        lg2.setName("citeLayerGroup");
        List<PublishedInfo> layers = lg2.getLayers();
        layers.add(catalog.getLayerByName("cite:Bridges"));
        layers.add(catalog.getLayerByName("cite:Buildings"));
        layers.add(catalog.getLayerByName("cite:Forests"));
        layers.add(catalog.getLayerByName("cite:Lakes"));
        layers.add(catalog.getLayerByName("cite:Ponds"));
        layers.add(catalog.getLayerByName("cite:Streams"));

        List<StyleInfo> styles = lg2.getStyles();
        styles.add(null);
        styles.add(null);
        styles.add(null);
        styles.add(null);
        styles.add(null);
        styles.add(null);

        lg2.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));

        catalog.add(lg2);

        // add some keywords to the CITE layer group
        addKeywordsToLayerGroup("citeLayerGroup");
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups.xml");
        assertEquals(
                catalog.getLayerGroups().size(),
                dom.getElementsByTagName("layerGroup").getLength());
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        getAsDOM(RestBaseController.ROOT_PATH + "/layergroups.html");
    }

    @Test
    public void testGetAllFromWorkspace() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/layergroups.xml");
        assertEquals("layerGroups", dom.getDocumentElement().getNodeName());

        assertXpathEvaluatesTo("0", "count(//layerGroup)", dom);

        addLayerGroupToWorkspace();

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/layergroups.xml");
        assertEquals("layerGroups", dom.getDocumentElement().getNodeName());

        assertXpathEvaluatesTo("1", "count(//layerGroup)", dom);
        assertXpathExists("//layerGroup/name[text() = 'workspaceLayerGroup']", dom);
    }

    void addLayerGroupToWorkspace() {
        Catalog cat = getCatalog();

        LayerGroupInfo lg = cat.getFactory().createLayerGroup();
        lg.setName("workspaceLayerGroup");
        lg.setWorkspace(cat.getWorkspaceByName("sf"));
        lg.getLayers().add(cat.getLayerByName("sf:PrimitiveGeoFeature"));
        lg.getStyles().add(null);
        cat.add(lg);
    }

    @Test
    public void testGetAsXML() throws Exception {

        print(get(RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup.xml"));
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup.xml");
        print(dom);

        assertEquals("layerGroup", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("sfLayerGroup", "/layerGroup/name", dom);
        assertXpathEvaluatesTo("2", "count(//published)", dom);
        assertXpathEvaluatesTo("2", "count(//style)", dom);
        // check layer link
        assertThat(
                xp.evaluate("//published[name='sf:PrimitiveGeoFeature']/atom:link/@href", dom),
                endsWith(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layers/PrimitiveGeoFeature.xml"));
        assertThat(
                xp.evaluate("//published[name='sf:PrimitiveGeoFeature']/atom:link/@type", dom),
                equalTo("application/xml"));
        // check style link
        assertThat(
                xp.evaluate("//style[1]/atom:link/@href", dom),
                endsWith(RestBaseController.ROOT_PATH + "/styles/point.xml"));
        assertThat(xp.evaluate("//style[1]/atom:link/@type", dom), equalTo("application/xml"));

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups/citeLayerGroup.xml");
        assertEquals("layerGroup", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("citeLayerGroup", "/layerGroup/name", dom);
        assertXpathEvaluatesTo("6", "count(//published)", dom);
        assertXpathEvaluatesTo("6", "count(//style)", dom);
        assertXpathEvaluatesTo("2", "count(//layerGroup/keywords/string)", dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//layerGroup/keywords[string='keyword1\\@language=en\\;\\@vocabulary=vocabulary1\\;'])",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//layerGroup/keywords[string='keyword2\\@language=pt\\;\\@vocabulary=vocabulary2\\;'])",
                dom);
        // check keywords were encoded

    }

    @Test
    public void testGetAsXMLNestedLinks() throws Exception {
        LayerGroupInfo cite = catalog.getLayerGroupByName("citeLayerGroup");
        cite.getLayers().add(catalog.getLayerGroupByName("sfLayerGroup"));
        cite.getStyles().add(null);
        catalog.save(cite);

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups/citeLayerGroup.xml");
        assertEquals("layerGroup", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("citeLayerGroup", "/layerGroup/name", dom);
        assertXpathEvaluatesTo("7", "count(//published)", dom);
        assertXpathEvaluatesTo("7", "count(//style)", dom);
        assertXpathEvaluatesTo("7", "count(//published/atom:link)", dom);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        print(get(RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup.json"));
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup.json");
        JSONArray arr =
                ((JSONObject) json)
                        .getJSONObject("layerGroup")
                        .getJSONObject("publishables")
                        .getJSONArray("published");
        assertEquals(2, arr.size());
        arr =
                ((JSONObject) json)
                        .getJSONObject("layerGroup")
                        .getJSONObject("styles")
                        .getJSONArray("style");
        assertEquals(2, arr.size());

        print(get(RestBaseController.ROOT_PATH + "/layergroups/citeLayerGroup.json"));
        json = getAsJSON(RestBaseController.ROOT_PATH + "/layergroups/citeLayerGroup.json");
        arr =
                ((JSONObject) json)
                        .getJSONObject("layerGroup")
                        .getJSONObject("publishables")
                        .getJSONArray("published");
        assertEquals(6, arr.size());
        arr =
                ((JSONObject) json)
                        .getJSONObject("layerGroup")
                        .getJSONObject("styles")
                        .getJSONArray("style");
        assertEquals(6, arr.size());

        // GEOS-7873
        LayerGroupInfo lg2 = catalog.getLayerGroupByName("citeLayerGroup");
        List<StyleInfo> styles = lg2.getStyles();
        styles.set(1, catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        styles.set(3, catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        catalog.save(lg2);

        print(get(RestBaseController.ROOT_PATH + "/layergroups/citeLayerGroup.json"));
        json = getAsJSON(RestBaseController.ROOT_PATH + "/layergroups/citeLayerGroup.json");
        JSONObject layerGroup = ((JSONObject) json).getJSONObject("layerGroup");
        arr = layerGroup.getJSONObject("publishables").getJSONArray("published");
        assertEquals(6, arr.size());
        arr = layerGroup.getJSONObject("styles").getJSONArray("style");
        assertEquals(6, arr.size());
        // check keywords were correctly encoded
        assertThat(layerGroup.containsKey("keywords"), is(true));
        JSONObject keywordsObject = layerGroup.getJSONObject("keywords");
        assertThat(keywordsObject.containsKey("string"), is(true));
        JSONArray keywords = keywordsObject.getJSONArray("string");
        assertThat(keywords.size(), is(2));
        // created a list of keywords so we can check is content with hamcrest
        List<Object> keywordsList = new ArrayList<>();
        keywordsList.addAll(keywords);
        assertThat(
                keywordsList,
                containsInAnyOrder(
                        "keyword1\\@language=en\\;\\@vocabulary=vocabulary1\\;",
                        "keyword2\\@language=pt\\;\\@vocabulary=vocabulary2\\;"));
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM(RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup.html");
    }

    @Test
    public void testRoundTripXML() throws Exception {
        LayerGroupInfo before = getCatalog().getLayerGroupByName("sfLayerGroup");

        // get and re-write, does not go boom
        String xml = getAsString(RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup.xml");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        // check nothing actually changed
        LayerGroupInfo after = getCatalog().getLayerGroupByName("sfLayerGroup");
        assertEquals(before, after);
    }

    @Test
    public void testRoundTripJSON() throws Exception {
        LayerGroupInfo before = getCatalog().getLayerGroupByName("sfLayerGroup");

        // get and re-write, does not go boom
        String json = getAsString(RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup.json");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup",
                        json,
                        "application/json");
        assertEquals(200, response.getStatus());

        // check nothing actually changed
        LayerGroupInfo after = getCatalog().getLayerGroupByName("sfLayerGroup");
        assertEquals(before, after);
    }

    @Test
    public void testWorkspaceRoundTripXML() throws Exception {
        addLayerGroupToWorkspace();
        LayerGroupInfo before = getCatalog().getLayerGroupByName("workspaceLayerGroup");

        // get and re-write, does not go boom
        String xml =
                getAsString(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup.xml");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        // check nothing actually changed
        LayerGroupInfo after = getCatalog().getLayerGroupByName("workspaceLayerGroup");
        assertEquals(before, after);
    }

    @Test
    public void testWorkspaceRoundTripJSON() throws Exception {
        addLayerGroupToWorkspace();
        LayerGroupInfo before = getCatalog().getLayerGroupByName("workspaceLayerGroup");

        // get and re-write, does not go boom
        String json =
                getAsString(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup.json");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup",
                        json,
                        "application/json");
        assertEquals(200, response.getStatus());

        // check nothing actually changed
        LayerGroupInfo after = getCatalog().getLayerGroupByName("workspaceLayerGroup");
        assertEquals(before, after);
    }

    @Test
    public void testGetWrongLayerGroup() throws Exception {
        // Parameters for the request
        String ws = "sf";
        String lg = "foooooo";
        // Request path
        String requestPath = RestBaseController.ROOT_PATH + "/layergroups/" + lg + ".html";
        String requestPath2 =
                RestBaseController.ROOT_PATH + "/workspaces/" + ws + "/layergroups/" + lg + ".html";
        // Exception path
        String exception = "No such layer group " + lg;
        String exception2 = "No such layer group " + lg + " in workspace " + ws;

        // CASE 1: No workspace set

        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(exception));

        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(exception));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());

        // CASE 2: workspace set

        // First request should thrown an exception
        response = getAsServletResponse(requestPath2);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(exception2));

        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath2 + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(exception2));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    public void testGetFromWorkspace() throws Exception {
        MockHttpServletResponse resp =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup.xml");
        assertEquals(404, resp.getStatus());

        addLayerGroupToWorkspace();

        resp =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup.xml");
        assertEquals(200, resp.getStatus());

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup.xml");
        assertXpathEvaluatesTo("workspaceLayerGroup", "/layerGroup/name", dom);
        assertXpathEvaluatesTo("sf", "/layerGroup/workspace/name", dom);
    }

    @Test
    public void testPost() throws Exception {
        String xml =
                "<layerGroup>"
                        + "    <name>newLayerGroup</name>"
                        + "    <layers>"
                        + "        <layer>Ponds</layer>"
                        + "        <layer>Forests</layer>"
                        + "    </layers>"
                        + "    <styles>"
                        + "        <style>polygon</style>"
                        + "        <style>point</style>"
                        + "    </styles>"
                        + "    <keywords>"
                        + "        <string>keyword1\\@language=en\\;\\@vocabulary=vocabulary1\\;</string>"
                        + "        <string>keyword2\\@language=pt\\;\\@vocabulary=vocabulary2\\;</string>"
                        + "    </keywords>"
                        + "</layerGroup>";
        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups", xml);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());

        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/layergroups/newLayerGroup"));

        LayerGroupInfo lg = catalog.getLayerGroupByName("newLayerGroup");
        assertNotNull(lg);

        assertEquals(2, lg.getLayers().size());
        assertEquals("Ponds", lg.getLayers().get(0).getName());
        assertEquals("Forests", lg.getLayers().get(1).getName());

        assertEquals(2, lg.getStyles().size());
        assertEquals("polygon", lg.getStyles().get(0).getName());
        assertEquals("point", lg.getStyles().get(1).getName());

        assertNotNull(lg.getBounds());

        // expected keywords
        Keyword keyword1 = new Keyword("keyword1");
        keyword1.setLanguage("en");
        keyword1.setVocabulary("vocabulary1");
        Keyword keyword2 = new Keyword("keyword2");
        keyword2.setLanguage("pt");
        keyword2.setVocabulary("vocabulary2");
        // check that the keywords were correctly added
        assertThat(lg.getKeywords().size(), is(2));
        assertThat(lg.getKeywords(), containsInAnyOrder(keyword1, keyword2));
        // creation date
        assertNotNull(lg.getDateCreated());
    }

    @Test
    public void testPostWithStyleGroups() throws Exception {
        // right now styleGroups need declared bounds to work
        String xml =
                "<layerGroup>"
                        + "    <name>newLayerGroupWithStyleGroup</name>"
                        + "    <layers>"
                        + "        <layer>Ponds</layer>"
                        + "        <layer></layer>"
                        + "    </layers>"
                        + "    <styles>"
                        + "        <style>polygon</style>"
                        + "        <style>singleStyleGroup</style>"
                        + "    </styles>"
                        + "    <keywords>"
                        + "        <string>keyword1\\@language=en\\;\\@vocabulary=vocabulary1\\;</string>"
                        + "        <string>keyword2\\@language=pt\\;\\@vocabulary=vocabulary2\\;</string>"
                        + "    </keywords>"
                        + "</layerGroup>";
        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups", xml);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());

        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/layergroups/newLayerGroupWithStyleGroup"));

        LayerGroupInfo lg = catalog.getLayerGroupByName("newLayerGroupWithStyleGroup");
        assertNotNull(lg);

        assertEquals(2, lg.getLayers().size());
        assertEquals("Ponds", lg.getLayers().get(0).getName());
        assertNull(lg.getLayers().get(1));

        assertEquals(2, lg.getStyles().size());
        assertEquals("polygon", lg.getStyles().get(0).getName());
        assertEquals("singleStyleGroup", lg.getStyles().get(1).getName());

        assertNotNull(lg.getBounds());

        // expected keywords
        Keyword keyword1 = new Keyword("keyword1");
        keyword1.setLanguage("en");
        keyword1.setVocabulary("vocabulary1");
        Keyword keyword2 = new Keyword("keyword2");
        keyword2.setLanguage("pt");
        keyword2.setVocabulary("vocabulary2");
        // check that the keywords were correctly added
        assertThat(lg.getKeywords().size(), is(2));
        assertThat(lg.getKeywords(), containsInAnyOrder(keyword1, keyword2));
    }

    @Test
    public void testPostWithNestedGroups() throws Exception {
        String xml =
                "<layerGroup>"
                        + "<name>nestedLayerGroupTest</name>"
                        + "<publishables>"
                        + "<published type=\"layer\">Ponds</published>"
                        + "<published type=\"layer\">Forests</published>"
                        + "<published type=\"layerGroup\">sfLayerGroup</published>"
                        + "</publishables>"
                        + "<styles>"
                        + "<style>polygon</style>"
                        + "<style>point</style>"
                        + "<style></style>"
                        + "</styles>"
                        + "</layerGroup>";

        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups", xml);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());

        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/layergroups/nestedLayerGroupTest"));

        LayerGroupInfo lg = catalog.getLayerGroupByName("nestedLayerGroupTest");
        assertNotNull(lg);

        assertEquals(3, lg.getLayers().size());
        assertEquals("Ponds", lg.getLayers().get(0).getName());
        assertEquals("Forests", lg.getLayers().get(1).getName());
        assertEquals("sfLayerGroup", lg.getLayers().get(2).getName());
        assertEquals(3, lg.getStyles().size());
        assertEquals("polygon", lg.getStyles().get(0).getName());
        assertEquals("point", lg.getStyles().get(1).getName());
        assertNull(lg.getStyles().get(2));

        assertNotNull(lg.getBounds());
    }

    @Test
    public void testPostWithTypeContainer() throws Exception {
        String xml =
                "<layerGroup>"
                        + "<name>newLayerGroupWithTypeCONTAINER</name>"
                        + "<mode>CONTAINER</mode>"
                        + "<layers>"
                        + "<layer>Ponds</layer>"
                        + "<layer>Forests</layer>"
                        + "</layers>"
                        + "<styles>"
                        + "<style>polygon</style>"
                        + "<style>point</style>"
                        + "</styles>"
                        + "</layerGroup>";

        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups", xml);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());

        LayerGroupInfo lg = catalog.getLayerGroupByName("newLayerGroupWithTypeCONTAINER");
        assertNotNull(lg);

        assertEquals(LayerGroupInfo.Mode.CONTAINER, lg.getMode());
    }

    @Test
    public void testPostWithTypeEO() throws Exception {
        String xml =
                "<layerGroup>"
                        + "<name>newLayerGroupWithTypeEO</name>"
                        + "<mode>EO</mode>"
                        + "<rootLayer>Ponds</rootLayer>"
                        + "<rootLayerStyle>polygon</rootLayerStyle>"
                        + "<layers>"
                        + "<layer>Forests</layer>"
                        + "</layers>"
                        + "<styles>"
                        + "<style>point</style>"
                        + "</styles>"
                        + "</layerGroup>";

        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups", xml);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());

        LayerGroupInfo lg = catalog.getLayerGroupByName("newLayerGroupWithTypeEO");
        assertNotNull(lg);

        assertEquals(LayerGroupInfo.Mode.EO, lg.getMode());
        assertEquals("Ponds", lg.getRootLayer().getName());
        assertEquals("polygon", lg.getRootLayerStyle().getName());
    }

    @Test
    public void testPostNoStyles() throws Exception {

        String xml =
                "<layerGroup>"
                        + "<name>newLayerGroup</name>"
                        + "<layers>"
                        + "<layer>Ponds</layer>"
                        + "<layer>Forests</layer>"
                        + "</layers>"
                        + "</layerGroup>";

        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups", xml);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());

        LayerGroupInfo lg = catalog.getLayerGroupByName("newLayerGroup");
        assertNotNull(lg);

        assertEquals(2, lg.getLayers().size());
        assertEquals("Ponds", lg.getLayers().get(0).getName());
        assertEquals("Forests", lg.getLayers().get(1).getName());

        assertEquals(2, lg.getStyles().size());
        assertNull(lg.getStyles().get(0));
        assertNull(lg.getStyles().get(1));
    }

    @Test
    public void testPostToWorkspace() throws Exception {
        Catalog cat = getCatalog();
        assertNotNull(cat.getWorkspaceByName("sf"));
        assertNull(cat.getLayerGroupByName("sf", "workspaceLayerGroup"));

        String xml =
                "<layerGroup>"
                        + "<name>workspaceLayerGroup</name>"
                        + "<layers>"
                        + "<layer>PrimitiveGeoFeature</layer>"
                        + "</layers>"
                        + "</layerGroup>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/layergroups", xml);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(cat.getLayerGroupByName("sf", "workspaceLayerGroup"));
    }

    @Test
    public void testPut() throws Exception {
        String xml =
                "<layerGroup>"
                        + "<name>sfLayerGroup</name>"
                        + "<styles>"
                        + "<style>polygon</style>"
                        + "<style>line</style>"
                        + "</styles>"
                        + "<attribution>"
                        + "  <logoWidth>101</logoWidth>"
                        + "  <logoHeight>102</logoHeight>"
                        + "</attribution>"
                        + "<metadataLinks>   "
                        + "<metadataLink>"
                        + "  <id>1</id>"
                        + "  <type>text/html</type>"
                        + "  <metadataType>FGDC</metadataType>"
                        + "  <content>http://my/metadata/link/1</content>"
                        + "</metadataLink>    "
                        + "<metadataLink>"
                        + "  <id>2</id>"
                        + "  <type>text/html</type>"
                        + "  <metadataType>FGDC</metadataType>"
                        + "  <content>http://my/metadata/link/2</content>"
                        + "</metadataLink>    "
                        + "</metadataLinks>"
                        + "</layerGroup>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        LayerGroupInfo lg = catalog.getLayerGroupByName("sfLayerGroup");

        assertEquals(2, lg.getLayers().size());
        assertEquals(2, lg.getStyles().size());
        assertEquals("polygon", lg.getStyles().get(0).getName());
        assertEquals("line", lg.getStyles().get(1).getName());
        assertEquals(101, lg.getAttribution().getLogoWidth());
        assertEquals(102, lg.getAttribution().getLogoHeight());
        assertEquals(2, lg.getMetadataLinks().size());
        assertNotNull(lg.getDateModified());
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        LayerGroupInfo lg = catalog.getLayerGroupByName("sfLayerGroup");
        boolean isQueryDisabled = lg.isQueryDisabled();

        lg.setQueryDisabled(true);
        catalog.save(lg);

        String xml = "<layerGroup>" + "<name>sfLayerGroup</name>" + "</layerGroup>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        lg = catalog.getLayerGroupByName("sfLayerGroup");

        assertTrue(lg.isQueryDisabled());
        lg.setQueryDisabled(isQueryDisabled);
        catalog.save(lg);
    }

    @Test
    public void testPutToWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        assertNull(cat.getLayerGroupByName("sf", "workspaceLayerGroup").getStyles().get(0));

        String xml =
                "<layerGroup>" + "<styles>" + "<style>line</style>" + "</styles>" + "</layerGroup>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup",
                        xml,
                        "application/xml");
        assertEquals(200, response.getStatus());
        assertEquals(
                "line",
                cat.getLayerGroupByName("sf", "workspaceLayerGroup").getStyles().get(0).getName());
    }

    @Test
    public void testPutToWorkspaceChangeWorkspace() throws Exception {
        testPostToWorkspace();

        String xml = "<layerGroup>" + "<workspace>cite</workspace>" + "</layerGroup>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup",
                        xml,
                        "application/xml");
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpServletResponse response =
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups/sfLayerGroup");
        assertEquals(200, response.getStatus());
        response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH + "/layergroups/citeLayerGroup");
        assertEquals(200, response.getStatus());

        assertEquals(0, catalog.getLayerGroups().size());
    }

    @Test
    public void testDeleteFromWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        assertNotNull(cat.getLayerGroupByName("sf", "workspaceLayerGroup"));

        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup");
        assertEquals(200, response.getStatus());

        assertNull(cat.getLayerGroupByName("sf", "workspaceLayerGroup"));
    }

    @Test
    public void testLayerGroupDuplicateLayerNames() throws Exception {
        // Create a Lakes layer in the sf workspace
        Catalog catalog = getCatalog();
        FeatureTypeInfo lakesFt = catalog.getFactory().createFeatureType();
        lakesFt.setName("Lakes");
        lakesFt.setNamespace(catalog.getNamespaceByPrefix("sf"));
        lakesFt.setStore(catalog.getDefaultDataStore(catalog.getWorkspaceByName("sf")));
        lakesFt.setNativeBoundingBox(
                new ReferencedEnvelope(-10, 10, -10, 10, DefaultGeographicCRS.WGS84));

        catalog.add(lakesFt);
        lakesFt = catalog.getFeatureTypeByName("sf", "Lakes");

        LayerInfo lakes = catalog.getFactory().createLayer();
        lakes.setResource(lakesFt);

        catalog.add(lakes);

        assertNotNull(catalog.getLayerByName("sf:Lakes"));
        assertNotNull(catalog.getLayerByName("cite:Lakes"));

        // POST a new layer group consisting of sf:Lakes and cite:Lakes
        String xml =
                "<layerGroup>"
                        + "<name>doubleLayerGroup</name>"
                        + "<layers>"
                        + "<layer>sf:Lakes</layer>"
                        + "<layer>cite:Lakes</layer>"
                        + "</layers>"
                        + "</layerGroup>";

        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups", xml);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());

        // Verify the new layer group in the catalog
        LayerGroupInfo lg = catalog.getLayerGroupByName("doubleLayerGroup");
        assertNotNull(lg);

        assertEquals(2, lg.getLayers().size());
        assertEquals("Lakes", lg.getLayers().get(0).getName());
        assertEquals("sf:Lakes", lg.getLayers().get(0).prefixedName());
        assertEquals("Lakes", lg.getLayers().get(1).getName());
        assertEquals("cite:Lakes", lg.getLayers().get(1).prefixedName());

        // GET layer group and verify layer names
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups/doubleLayerGroup.xml");
        print(dom);

        assertEquals("layerGroup", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("doubleLayerGroup", "/layerGroup/name", dom);
        assertXpathEvaluatesTo("2", "count(//published)", dom);
        assertXpathEvaluatesTo("2", "count(//style)", dom);

        // verify layer order
        assertXpathEvaluatesTo("sf:Lakes", "//publishables/published[1]/name", dom);
        assertXpathEvaluatesTo("cite:Lakes", "//publishables/published[2]/name", dom);
        // verify layer links
        assertThat(
                xp.evaluate("//published[name='sf:Lakes']/atom:link/@href", dom),
                endsWith(RestBaseController.ROOT_PATH + "/workspaces/sf/layers/Lakes.xml"));
        assertThat(
                xp.evaluate("//published[name='sf:Lakes']/atom:link/@type", dom),
                equalTo("application/xml"));
        assertThat(
                xp.evaluate("//published[name='cite:Lakes']/atom:link/@href", dom),
                endsWith(RestBaseController.ROOT_PATH + "/workspaces/cite/layers/Lakes.xml"));
        assertThat(
                xp.evaluate("//published[name='cite:Lakes']/atom:link/@type", dom),
                equalTo("application/xml"));
    }

    @Test
    public void testLayersStylesInWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        StyleInfo s = cat.getFactory().createStyle();
        s.setName("s1");
        s.setWorkspace(cat.getWorkspaceByName("sf"));
        s.setFilename("s1.sld");
        cat.add(s);

        s = cat.getFactory().createStyle();
        s.setName("s2");
        s.setWorkspace(cat.getWorkspaceByName("sf"));
        s.setFilename("s2.sld");
        cat.add(s);

        String xml =
                "<layerGroup>"
                        + "<layers>"
                        + "<layer>PrimitiveGeoFeature</layer>"
                        + "<layer>AggregateGeoFeature</layer>"
                        + "</layers>"
                        + "<styles>"
                        + "<style>"
                        + "<name>s1</name>"
                        + "<workspace>sf</workspace>"
                        + "</style>"
                        + "<style>"
                        + "<name>s2</name>"
                        + "<workspace>sf</workspace>"
                        + "</style>"
                        + "</styles>"
                        + "</layerGroup>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        LayerGroupInfo lg = cat.getLayerGroupByName("sf", "workspaceLayerGroup");
        assertEquals(2, lg.getLayers().size());
        assertEquals(2, lg.getStyles().size());

        assertEquals("PrimitiveGeoFeature", lg.getLayers().get(0).getName());
        assertEquals("AggregateGeoFeature", lg.getLayers().get(1).getName());

        assertEquals("s1", lg.getStyles().get(0).getName());
        assertNotNull(lg.getStyles().get(0).getWorkspace());
        assertEquals("sf", lg.getStyles().get(0).getWorkspace().getName());

        assertEquals("s2", lg.getStyles().get(1).getName());
        assertNotNull(lg.getStyles().get(1).getWorkspace());
        assertEquals("sf", lg.getStyles().get(1).getWorkspace().getName());

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/layergroups/workspaceLayerGroup.xml");
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/workspaces/sf/styles/s1.xml",
                "//style[name = 'sf:s1']/atom:link/@href",
                dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/workspaces/sf/styles/s2.xml",
                "//style[name = 'sf:s2']/atom:link/@href",
                dom);
    }
}
