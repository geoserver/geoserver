/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static java.lang.String.format;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.DIVIDED_ROUTES;
import static org.geoserver.data.test.MockData.ROAD_SEGMENTS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.JsonContext;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.community.css.web.CssHandler;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Style;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class StylesExtensionTest extends GeoServerSystemTestSupport {

    // xpath engine that will be used to check XML content
    protected static XpathEngine xpath;

    {
        // registering namespaces for the xpath engine
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("wmts", "http://www.opengis.net/wmts/1.0");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        final Catalog catalog = getCatalog();
        final LayerInfo roadSegments = catalog.getLayerByName(getLayerId(ROAD_SEGMENTS));
        catalog.save(roadSegments);

        testData.addStyle("stylegroup", "stylegroup.sld", StylesExtensionTest.class, catalog);
        final LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.getLayers().add(null);
        group.getStyles().add(catalog.getStyleByName("stylegroup"));
        group.setName("wmts-group");
        new LayerGroupHelper(group).calculateBounds();
        catalog.add(group);
    }

    @Before
    public void cleanup() {
        final Catalog catalog = getCatalog();
        final LayerInfo roadSegments = catalog.getLayerByName(getLayerId(ROAD_SEGMENTS));
        roadSegments.getStyles().clear();
        StyleInfo line = catalog.getStyleByName("line");
        roadSegments.getStyles().add(line);
        catalog.save(roadSegments);

        Optional.ofNullable(catalog.getStyleByName("simplePoint"))
                .ifPresent(s -> catalog.remove(s));
        Optional.ofNullable(catalog.getStyleByName("testPoint")).ifPresent(s -> catalog.remove(s));
        Optional.ofNullable(catalog.getStyleByName("circles")).ifPresent(s -> catalog.remove(s));
        Optional.ofNullable(catalog.getStyleByName("cssline")).ifPresent(s -> catalog.remove(s));
    }

    @Test
    public void testCapabilitiesLayerLinks() throws Exception {
        final Document dom = getAsDOM("gwc/service/wmts?request=GetCapabilities", 200);
        // print(dom);

        String template =
                "//wmts:Layer[ows:Identifier='cite:RoadSegments']/wmts:ResourceURL[@resourceType = '%s']";

        // layer styles resource
        final NamedNodeMap stylesAtts = getNodeAttributes(dom, format(template, "layerStyles"));
        assertEquals("text/json", attributeValue(stylesAtts, "format"));
        assertEquals(
                "http://localhost:8080/geoserver/gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/?f=text%2Fjson",
                attributeValue(stylesAtts, "template"));

        // default style
        final NamedNodeMap defaultStylesAtts =
                getNodeAttributes(dom, format(template, "defaultStyle"));
        assertEquals(SLDHandler.MIMETYPE_10, attributeValue(defaultStylesAtts, "format"));
        assertEquals(
                "http://localhost:8080/geoserver/gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/RoadSegments?f=application%2Fvnd.ogc.sld%2Bxml",
                attributeValue(defaultStylesAtts, "template"));

        // alternate style
        final NamedNodeMap alternateStylesAtts = getNodeAttributes(dom, format(template, "style"));
        assertEquals(SLDHandler.MIMETYPE_10, attributeValue(alternateStylesAtts, "format"));
        assertEquals(
                "http://localhost:8080/geoserver/gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/line?f=application%2Fvnd.ogc.sld%2Bxml",
                attributeValue(alternateStylesAtts, "template"));
    }

    @Test
    public void testCapabilitiesLinksVirtualWorkspace() throws Exception {
        final Document dom = getAsDOM("cite/gwc/service/wmts?request=GetCapabilities", 200);
        print(dom);

        String template =
                "//wmts:Layer[ows:Identifier='RoadSegments']/wmts:ResourceURL[@resourceType = '%s']";

        // layer styles resource
        final NamedNodeMap stylesAtts = getNodeAttributes(dom, format(template, "layerStyles"));
        assertEquals("text/json", attributeValue(stylesAtts, "format"));
        assertEquals(
                "http://localhost:8080/geoserver/cite/gwc/service/wmts/reststyles/layers/RoadSegments/styles/?f=text%2Fjson",
                attributeValue(stylesAtts, "template"));

        // default style
        final NamedNodeMap defaultStylesAtts =
                getNodeAttributes(dom, format(template, "defaultStyle"));
        assertEquals(SLDHandler.MIMETYPE_10, attributeValue(defaultStylesAtts, "format"));
        assertEquals(
                "http://localhost:8080/geoserver/cite/gwc/service/wmts/reststyles/layers/RoadSegments/styles/RoadSegments?f=application%2Fvnd.ogc.sld%2Bxml",
                attributeValue(defaultStylesAtts, "template"));

        // alternate style
        final NamedNodeMap alternateStylesAtts = getNodeAttributes(dom, format(template, "style"));
        assertEquals(SLDHandler.MIMETYPE_10, attributeValue(alternateStylesAtts, "format"));
        assertEquals(
                "http://localhost:8080/geoserver/cite/gwc/service/wmts/reststyles/layers/RoadSegments/styles/line?f=application%2Fvnd.ogc.sld%2Bxml",
                attributeValue(alternateStylesAtts, "template"));
    }

    private NamedNodeMap getNodeAttributes(Document dom, String path) throws XpathException {
        final NodeList matchingNodes = xpath.getMatchingNodes(path, dom);
        assertEquals(1, matchingNodes.getLength());
        final Node layerStylesNode = matchingNodes.item(0);
        return layerStylesNode.getAttributes();
    }

    @Test
    public void testLayerStyles() throws Exception {
        final MockHttpServletResponse response =
                getAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/RoadSegments?f"
                                + "=application%2Fvnd.ogc.sld%2Bxml");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        final Document dom = dom(response, true);
        assertEquals("dirt_road", xpath.evaluate("//sld:Rule/sld:Name", dom));
    }

    @Test
    public void testDefaultFormat() throws Exception {
        final MockHttpServletResponse response =
                getAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/RoadSegments");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        final Document dom = dom(response, true);
        assertEquals("dirt_road", xpath.evaluate("//sld:Rule/sld:Name", dom));
    }

    @Test
    public void testLayerNonAssociatedStyle() throws Exception {
        final MockHttpServletResponse response =
                getAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/polygon");
        assertEquals(NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void testPutStyle() throws Exception {
        String styleBody = loadStyle("simplePoint.sld");
        // use a name not found in the style body
        final MockHttpServletResponse response =
                putAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/testPoint",
                        styleBody,
                        SLDHandler.MIMETYPE_10);
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("testPoint");
        checkSimplePoint(styleInfo, Color.RED);

        // check layer association
        final LayerInfo layer = getCatalog().getLayerByName(getLayerId(ROAD_SEGMENTS));
        assertThat(layer.getStyles(), hasItem(hasProperty("name", equalTo("testPoint"))));
    }

    @Test
    public void testPutSLDStyleModify() throws Exception {
        testPutStyle();

        // use a different style body
        String styleBody = loadStyle("simplePoint2.sld");
        final MockHttpServletResponse response =
                putAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/testPoint",
                        styleBody,
                        SLDHandler.MIMETYPE_10);
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("testPoint");
        checkSimplePoint(styleInfo, Color.BLACK);
    }

    private String loadStyle(String fileName) throws IOException {
        try (InputStream is = StylesExtensionTest.class.getResourceAsStream(fileName)) {
            return IOUtils.toString(is, "UTF-8");
        }
    }

    public void checkSimplePoint(StyleInfo styleInfo, Color expectedColor) throws IOException {
        assertNotNull(styleInfo);
        final Style style = styleInfo.getStyle();
        PointSymbolizer ps =
                (PointSymbolizer)
                        style.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        final Mark mark = (Mark) ps.getGraphic().graphicalSymbols().get(0);
        assertEquals("circle", mark.getWellKnownName().evaluate(null, String.class));
        assertEquals(expectedColor, mark.getFill().getColor().evaluate(null, Color.class));
    }

    @Test
    public void testDeleteNonExistingStyle() throws Exception {
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/testPoint");
        assertEquals(NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void testDeleteNonAssociatedBuiltInStyle() throws Exception {
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/polygon");
        assertEquals(NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void testDeleteNonAssociatedStyle() throws Exception {
        // add testPoint associated to cite:RoadSegments, try to remove it from Streams
        testPutStyle();
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:Streams/styles/testPoint");
        assertEquals(NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void testDeleteAssociatedStyle() throws Exception {
        // add testPoint, but not associated to the road segments layer
        testPutStyle();
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/testPoint");
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check the style is gone and the association too
        assertNull(getCatalog().getStyleByName("simplePoint"));
        final LayerInfo layer = getCatalog().getLayerByName(getLayerId(ROAD_SEGMENTS));
        assertEquals(1, layer.getStyles().size());
        assertThat(layer.getStyles(), hasItems(hasProperty("name", equalTo("line"))));
    }

    @Test
    public void testMBStyle() throws Exception {
        String styleBody = loadStyle("mbcircle.json");
        // use a name not found in the style body
        MockHttpServletResponse response =
                putAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/mbcircle",
                        styleBody,
                        MBStyleHandler.MIME_TYPE);
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("mbcircle");
        assertNotNull(styleInfo);

        // verify links for it
        Document dom = getAsDOM("gwc/service/wmts?request=GetCapabilities", 200);
        String template =
                "//wmts:Layer[ows:Identifier='cite:RoadSegments']/wmts:ResourceURL[contains(@template, 'mbcircle') and @format='%s']";
        // native format link
        final NamedNodeMap mbstyleAtts =
                getNodeAttributes(dom, format(template, "application/vnd.geoserver.mbstyle+json"));
        assertEquals("style", attributeValue(mbstyleAtts, "resourceType"));
        assertEquals(
                "http://localhost:8080/geoserver/gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/mbcircle?f=application%2Fvnd.geoserver.mbstyle%2Bjson",
                attributeValue(mbstyleAtts, "template"));
        // converted format link
        final NamedNodeMap sldAtts =
                getNodeAttributes(dom, format(template, "application/vnd.ogc.sld+xml"));
        assertEquals("style", attributeValue(sldAtts, "resourceType"));
        assertEquals(
                "http://localhost:8080/geoserver/gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/mbcircle?f=application%2Fvnd.ogc.sld%2Bxml",
                attributeValue(sldAtts, "template"));

        // check we can get both styles, first SLD
        response =
                getAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/mbcircle?f=application%2Fvnd.ogc.sld%2Bxml");
        assertEquals(200, response.getStatus());
        assertEquals("inline; filename=mbcircle.sld", response.getHeader("Content-Disposition"));
        dom = dom(response, true);
        // print(dom);
        assertXpathEvaluatesTo("circles", "//sld:StyledLayerDescriptor/sld:Name", dom);
        assertXpathEvaluatesTo("1", "count(//sld:Rule)", dom);
        assertXpathEvaluatesTo("1", "count(//sld:PointSymbolizer)", dom);
        assertXpathEvaluatesTo(
                "circle", "//sld:PointSymbolizer/sld:Graphic/sld:Mark/sld:WellKnownName", dom);

        // .. then MBStyle
        response =
                getAsServletResponse(
                        "/gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/mbcircle?f=application%2Fvnd.geoserver.mbstyle%2Bjson");
        assertEquals(200, response.getStatus());
        assertEquals(
                "inline; filename=mbcircle.mbstyle", response.getHeader("Content-Disposition"));
        DocumentContext mbstyle = getAsJSONPath(response);
        assertEquals("circles", mbstyle.read("$.name"));
    }

    @Test
    public void testCSS() throws Exception {
        String styleBody = loadStyle("line.css");
        // create style
        MockHttpServletResponse response =
                putAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/cssline",
                        styleBody,
                        CssHandler.MIME_TYPE);
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("cssline");
        assertNotNull(styleInfo);

        // verify links for it
        Document dom = getAsDOM("gwc/service/wmts?request=GetCapabilities", 200);
        String template =
                "//wmts:Layer[ows:Identifier='cite:RoadSegments']/wmts:ResourceURL[contains(@template, 'cssline') and @format='%s']";
        // native format link
        final NamedNodeMap mbstyleAtts =
                getNodeAttributes(dom, format(template, CssHandler.MIME_TYPE));
        assertEquals("style", attributeValue(mbstyleAtts, "resourceType"));
        assertEquals(
                "http://localhost:8080/geoserver/gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/cssline?f=application%2Fvnd.geoserver.geocss%2Bcss",
                attributeValue(mbstyleAtts, "template"));
        // converted format link
        final NamedNodeMap sldAtts =
                getNodeAttributes(dom, format(template, "application/vnd.ogc.sld+xml"));
        assertEquals("style", attributeValue(sldAtts, "resourceType"));
        assertEquals(
                "http://localhost:8080/geoserver/gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/cssline?f=application%2Fvnd.ogc.sld%2Bxml",
                attributeValue(sldAtts, "template"));

        // check we can get both styles, first SLD
        response =
                getAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/cssline?f=application%2Fvnd.ogc.sld%2Bxml");
        assertEquals(200, response.getStatus());
        assertEquals("inline; filename=cssline.sld", response.getHeader("Content-Disposition"));
        dom = dom(response, true);
        // print(dom);
        assertXpathEvaluatesTo("cssline", "//sld:StyledLayerDescriptor/sld:Name", dom);
        assertXpathEvaluatesTo("1", "count(//sld:Rule)", dom);
        assertXpathEvaluatesTo("1", "count(//sld:LineSymbolizer)", dom);
        assertXpathEvaluatesTo(
                "3", "//sld:LineSymbolizer/sld:Stroke/sld:CssParameter[@name='stroke-width']", dom);

        // .. then CSS
        response =
                getAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/cite:RoadSegments/styles/cssline?f=application%2Fvnd.geoserver.geocss%2Bcss");
        assertEquals(200, response.getStatus());
        assertEquals("inline; filename=cssline.css", response.getHeader("Content-Disposition"));
        assertEqualsIgnoreNewLineStyle(
                "* {\n" + "   stroke: black;\n" + "   stroke-width: 3;\n" + "}",
                response.getContentAsString());
    }

    protected DocumentContext getAsJSONPath(MockHttpServletResponse response)
            throws UnsupportedEncodingException {
        assertThat(response.getContentType(), containsString("json"));
        JsonContext json = (JsonContext) JsonPath.parse(response.getContentAsString());
        if (!isQuietTests()) {
            print(json(response));
        }
        return json;
    }

    @Test
    public void testWorkspaceSpecific() throws Exception {
        String styleBody = loadStyle("dashedline.sld");
        // use a name not found in the style body
        MockHttpServletResponse response =
                putAsServletResponse(
                        "cite/gwc/service/wmts/reststyles/layers/DividedRoutes/styles/dashed",
                        styleBody,
                        SLDHandler.MIMETYPE_10);
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("dashed");
        assertNotNull(styleInfo);
        assertThat(styleInfo.getWorkspace(), equalTo(getCatalog().getWorkspaceByName("cite")));

        // check layer association
        LayerInfo layer = getCatalog().getLayerByName(getLayerId(DIVIDED_ROUTES));
        assertThat(layer.getStyles(), hasItem(hasProperty("name", equalTo("dashed"))));

        // check capabilities with a layer specific service
        Document dom = getAsDOM("cite/DividedRoutes/gwc/service/wmts?request=GetCapabilities", 200);
        // print(dom);
        String template =
                "//wmts:Layer[ows:Identifier='DividedRoutes']/wmts:ResourceURL[contains(@template, 'dashed') and @format='%s']";
        final NamedNodeMap styleAtts =
                getNodeAttributes(dom, format(template, SLDHandler.MIMETYPE_10));
        assertEquals("style", attributeValue(styleAtts, "resourceType"));

        // remove the style using a workspace specific delete
        response =
                deleteAsServletResponse(
                        "cite/gwc/service/wmts/reststyles/layers/DividedRoutes/styles/dashed");
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check the style is no more and it's no longer associated
        assertNull(getCatalog().getStyleByName("dashed"));
        layer = getCatalog().getLayerByName(getLayerId(DIVIDED_ROUTES));
        assertThat(layer.getStyles(), not(hasItem(hasProperty("name", equalTo("dashed")))));
    }

    @Test
    public void testStyleGroup() throws Exception {
        // check capabilities
        final Document dom = getAsDOM("gwc/service/wmts?request=GetCapabilities", 200);
        final NamedNodeMap attributes =
                getNodeAttributes(
                        dom,
                        "//wmts:Layer[ows:Identifier='wmts-group']/wmts:ResourceURL[@resourceType = "
                                + "'defaultStyle']");
        assertEquals(SLDHandler.MIMETYPE_10, attributeValue(attributes, "format"));
        assertEquals(
                "http://localhost:8080/geoserver/gwc/service/wmts/reststyles/layers/wmts-group/styles/stylegroup?f=application%2Fvnd.ogc.sld%2Bxml",
                attributeValue(attributes, "template"));

        // make sure the style can be retrieved
        final MockHttpServletResponse response =
                getAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/wmts-group/styles/stylegroup?f=application"
                                + "%2Fvnd.ogc.sld%2Bxml");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        final Document styleDom = dom(response, true);
        assertXpathEvaluatesTo("DividedRoutes", "//sld:NamedLayer[1]/sld:Name", styleDom);
        assertXpathEvaluatesTo("Lakes", "//sld:NamedLayer[2]/sld:Name", styleDom);

        // check it can be modified
        final String style = loadStyle("stylegroup2.sld");
        final MockHttpServletResponse putResponse =
                putAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/wmts-group/styles/stylegroup",
                        style,
                        SLDHandler.MIMETYPE_10);
        assertEquals(204, putResponse.getStatus());

        // get it again and check
        final MockHttpServletResponse styleResponse2 =
                getAsServletResponse(
                        "gwc/service/wmts/reststyles/layers/wmts-group/styles/stylegroup?f=application"
                                + "%2Fvnd.ogc.sld%2Bxml");
        assertEquals(200, styleResponse2.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, styleResponse2.getContentType());
        final Document styleDom2 = dom(styleResponse2, true);
        assertXpathEvaluatesTo("#FF00FF", "//sld:NamedLayer[1]//sld:CssParameter", styleDom2);
        assertXpathEvaluatesTo("#000000", "//sld:NamedLayer[2]//sld:CssParameter", styleDom2);
    }

    public String attributeValue(NamedNodeMap attributes, String template) {
        return attributes.getNamedItem(template).getTextContent();
    }
}
