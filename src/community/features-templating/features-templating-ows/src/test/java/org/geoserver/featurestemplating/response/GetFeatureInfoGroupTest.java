/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class GetFeatureInfoGroupTest extends WMSTestSupport {

    private static final String FORESTS_GML_FEATUREINFO = "GMLFeatureInfoFORESTS";
    private static final String FORESTS_GML_PARAM = "&" + FORESTS_GML_FEATUREINFO + "=true";

    private static final String FORESTS_GEOJSON_FEATUREINFO = "GeoJSONFeatureInfoFORESTS";
    private static final String FORESTS_GEOJSON_PARAM = "&" + FORESTS_GEOJSON_FEATUREINFO + "=true";

    private static final String FORESTS_JSONLD_FEATUREINFO = "JSONLDFeatureInfoFORESTS";
    private static final String FORESTS_JSONLD_PARAM = "&" + FORESTS_JSONLD_FEATUREINFO + "=true";

    private static final String F_TEST_CONTEXT_JSONLD_FEATUREINFO = "JSONLDFORESTSSameContext";
    private static final String F_TEST_CONTEXT_JSONLD_PARAM =
            "&" + F_TEST_CONTEXT_JSONLD_FEATUREINFO + "=true";

    private static final String FORESTS_HTML_FEATUREINFO = "HTMLFeatureInfoFORESTS";
    private static final String FORESTS_HTML_PARAM = "&" + FORESTS_HTML_FEATUREINFO + "=true";

    private static final String FORESTS_HTML_JSONLD_FEATUREINFO = "HTMLFeatureInfoJSONLDFORESTS";
    private static final String FORESTS_HTML_JSONLD_PARAM =
            "&" + FORESTS_HTML_JSONLD_FEATUREINFO + "=true";

    private static final String LAKES_GML_FEATUREINFO = "GMLFeatureInfoLAKES";
    private static final String LAKES_GML_PARAM = "&" + LAKES_GML_FEATUREINFO + "=true";

    private static final String LAKES_GEOJSON_FEATUREINFO = "GeoJSONFeatureInfoLAKES";
    private static final String LAKES_GEOJSON_PARAM = "&" + LAKES_GEOJSON_FEATUREINFO + "=true";

    private static final String LAKES_JSONLD_FEATUREINFO = "JSONLDFeatureInfoLAKES";
    private static final String LAKES_JSONLD_PARAM = "&" + LAKES_JSONLD_FEATUREINFO + "=true";

    private static final String L_TEST_CONTEXT_JSONLD_FEATUREINFO = "JSONLDLAKESSameContext";
    private static final String L_TEST_CONTEXT_JSONLD_PARAM =
            "&" + L_TEST_CONTEXT_JSONLD_FEATUREINFO + "=true";

    private static final String LAKES_HTML_FEATUREINFO = "HTMLFeatureInfoLAKES";
    private static final String LAKES_HTML_PARAM = "&" + LAKES_HTML_FEATUREINFO + "=true";

    private static final String LAKES_HTML_JSONLD_FEATUREINFO = "HTMLJSONLDFeatureInfoLAKES";
    private static final String LAKES_HTML_JSONLD_PARAM =
            "&" + LAKES_HTML_JSONLD_FEATUREINFO + "=true";

    private static final String GEOJSON_LAKE_TEMPLATE =
            "{\n"
                    + "   \"type\":\"Feature\",\n"
                    + "   \"id\":\"${@id}\",\n"
                    + "   \"geometry\":\"${the_geom}\",\n"
                    + "   \"properties\":{\n"
                    + "     \"name\":\"${NAME}\",\n"
                    + "     \"staticAttr\":\"I'm a lake\"\n"
                    + "   }\n"
                    + "}";

    private static final String GEOJSON_FOREST_TEMPLATE =
            "{\n"
                    + "   \"type\":\"Feature\",\n"
                    + "   \"id\":\"${@id}\",\n"
                    + "   \"geometry\":\"${the_geom}\",\n"
                    + "   \"properties\":{\n"
                    + "     \"name\":\"${NAME}\",\n"
                    + "     \"staticAttr\":\"I'm a forest\"\n"
                    + "   }\n"
                    + "}\n";

    private static final String JSONLD_LAKE_TEMPLATE =
            "{\n"
                    + "   \"$options\":{\n"
                    + "      \"@context\":[\n"
                    + "         \"https://geoserver/lake.org\",\n"
                    + "         {\n"
                    + "            \"sf\":\"http://www.opengis.net/ont/sf#\",\n"
                    + "            \"schema\":\"https://schema.org/\",\n"
                    + "            \"wkt\":\"gsp:asWKT\",\n"
                    + "            \"Feature\":\"gsp:Feature\",\n"
                    + "            \"geometry\":\"gsp:hasGeometry\",\n"
                    + "            \"polygon\":\"sf:polygon\",\n"
                    + "            \"features\":{\n"
                    + "               \"@container\":\"@set\",\n"
                    + "               \"@id\":\"schema:hasPart\"\n"
                    + "            }\n"
                    + "         }\n"
                    + "      ]\n"
                    + "   },\n"
                    + "   \"type\":\"Feature\",\n"
                    + "   \"id\":\"${@id}\",\n"
                    + "   \"geometry\":\"$${toWKT(the_geom)}\",\n"
                    + "   \"properties\":{\n"
                    + "      \"name\":\"${NAME}\",\n"
                    + "      \"staticAttr\":\"I'm a lake\"\n"
                    + "   }\n"
                    + "}";

    private static final String JSONLD_FOREST_TEMPLATE =
            "{  \n"
                    + "   \"$options\": {\n"
                    + "    \"@context\": \"https://geoserver/forest.org\"\n"
                    + "   },\n"
                    + "   \"type\":\"Feature\",\n"
                    + "   \"id\":\"${@id}\",\n"
                    + "   \"geometry\":\"$${toWKT(the_geom)}\",\n"
                    + "   \"properties\":{\n"
                    + "     \"name\":\"${NAME}\",\n"
                    + "     \"staticAttr\":\"I'm a forest\"\n"
                    + "   }\n"
                    + "}";

    private static final String JSONLD_L_TEST_CONTEXT_TEMPLATE =
            "{\n"
                    + "   \"$options\":{\n"
                    + "      \"@context\":{\n"
                    + "            \"sf\":\"http://www.opengis.net/ont/sf#\",\n"
                    + "            \"schema\":\"https://schema.org/\",\n"
                    + "            \"wkt\":\"gsp:asWKT\",\n"
                    + "            \"Feature\":\"gsp:Feature\",\n"
                    + "            \"geometry\":\"gsp:hasGeometry\",\n"
                    + "            \"polygon\":\"sf:polygon\",\n"
                    + "            \"features\":{\n"
                    + "               \"@container\":\"@set\",\n"
                    + "               \"@id\":\"schema:hasPart\"\n"
                    + "            }\n"
                    + "         }\n"
                    + "   },\n"
                    + "   \"type\":\"Feature\",\n"
                    + "   \"id\":\"${@id}\",\n"
                    + "   \"geometry\":\"$${toWKT(the_geom)}\",\n"
                    + "   \"properties\":{\n"
                    + "      \"name\":\"${NAME}\",\n"
                    + "      \"staticAttr\":\"I'm a lake\"\n"
                    + "   }\n"
                    + "}";

    private static final String JSONLD_F_TEST_CONTEXT_TEMPLATE =
            "{\n"
                    + "   \"$options\":{\n"
                    + "      \"@context\": {\n"
                    + "            \"sf\":\"http://www.opengis.net/ont/sf#\",\n"
                    + "            \"schema\":\"https://schema.org/\",\n"
                    + "            \"wkt\":\"gsp:asWKT\",\n"
                    + "            \"Feature\":\"gsp:Feature\",\n"
                    + "            \"geometry\":\"gsp:hasGeometry\",\n"
                    + "            \"polygon\":\"sf:polygon\"\n"
                    + "         }\n"
                    + "   },\n"
                    + "   \"type\":\"Feature\",\n"
                    + "   \"id\":\"${@id}\",\n"
                    + "   \"geometry\":\"$${toWKT(the_geom)}\",\n"
                    + "   \"properties\":{\n"
                    + "     \"name\":\"${NAME}\",\n"
                    + "     \"staticAttr\":\"I'm a forest\"\n"
                    + "   }\n"
                    + "}";

    private static final String GML_LAKES_TEMPLATE =
            "<gft:Template>\n"
                    + "        <gft:Options>\n"
                    + "         <gft:Namespaces xmlns:cite=\"http://www.opengis.net/cite\"/>\n"
                    + "         <gft:SchemaLocation xsi:schemaLocation=\"http://www.opengis.net/wfs http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-basic.xsd http://www.opengis.net/cite http://localhost:8080/geoserver/wfs?service=WFS&amp;version=1.0.0&amp;request=DescribeFeatureType&amp;typeName=cite%3ALakes,cite%3AForests\"/>\n"
                    + "        </gft:Options>\n"
                    + "        <cite:Lakes fid=\"${@id}\">\n"
                    + "          <cite:the_geom>${the_geom}</cite:the_geom>\n"
                    + "          <cite:name>${NAME}</cite:name>\n"
                    + "          <cite:staticAttr>I'm a lake</cite:staticAttr>\n"
                    + "        </cite:Lakes>\n"
                    + "      </gft:Template>";

    private static final String GML_FOREST_TEMPLATE =
            "<gft:Template>\n"
                    + "        <gft:Options>\n"
                    + "         <gft:Namespaces xmlns:cite=\"http://www.opengis.net/cite\"/>\n"
                    + "         <gft:SchemaLocation xsi:schemaLocation=\"http://www.opengis.net/wfs http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-basic.xsd http://www.opengis.net/cite http://localhost:8080/geoserver/wfs?service=WFS&amp;version=1.0.0&amp;request=DescribeFeatureType&amp;typeName=cite%3ALakes,cite%3AForests\"/>\n"
                    + "        </gft:Options>\n"
                    + "        <cite:Forests fid=\"${@id}\">\n"
                    + "          <cite:the_geom>${the_geom}</cite:the_geom>\n"
                    + "          <cite:name>${NAME}</cite:name>\n"
                    + "          <cite:staticAttr>I'm a forest</cite:staticAttr>\n"
                    + "        </cite:Forests>\n"
                    + "      </gft:Template>";

    private static final String HTML_LAKE_TEMPLATE =
            "<gft:Template>\n"
                    + "        <table class=\"featureInfo\">\n"
                    + "         <tr>\n"
                    + "            <th>fid</th>\n"
                    + "            <th>name</th>\n"
                    + "            <th>static attribute</th>\n"
                    + "         </tr>\n"
                    + "         <tr>\n"
                    + "            <td>${@id}</td>\n"
                    + "            <td>${NAME}</td>\n"
                    + "            <td>I'm a lake</td>\n"
                    + "         </tr>\n"
                    + "      </table>\n"
                    + "      </gft:Template>";

    private static final String HTML_FOREST_TEMPLATE =
            "<gft:Template>\n"
                    + "        <table class=\"featureInfo\">\n"
                    + "         <tr>\n"
                    + "            <th>fid</th>\n"
                    + "            <th>name</th>\n"
                    + "            <th>static attribute</th>\n"
                    + "         </tr>\n"
                    + "         <tr>\n"
                    + "            <td>${@id}</td>\n"
                    + "            <td>${NAME}</td>\n"
                    + "            <td>I'm a forest</td>\n"
                    + "         </tr>\n"
                    + "      </table>\n"
                    + "      </gft:Template>";

    private static final String HTML_LAKE_TEMPLATE_JSON_LD =
            "<gft:Template>\n"
                    + "        <gft:Options>"
                    + "          <script type=\"application/ld+json\"/>"
                    + "        </gft:Options>"
                    + "        <table class=\"featureInfo\">\n"
                    + "         <tr>\n"
                    + "            <th>fid</th>\n"
                    + "            <th>name</th>\n"
                    + "            <th>static attribute</th>\n"
                    + "         </tr>\n"
                    + "         <tr>\n"
                    + "            <td>${@id}</td>\n"
                    + "            <td>${NAME}</td>\n"
                    + "            <td>I'm a lake</td>\n"
                    + "         </tr>\n"
                    + "      </table>\n"
                    + "      </gft:Template>";

    private static final String HTML_FOREST_TEMPLATE_JSON_LD =
            "<gft:Template>\n"
                    + "        <gft:Options>"
                    + "          <script type=\"application/ld+json\"/>"
                    + "        </gft:Options>"
                    + "        <table class=\"featureInfo\">\n"
                    + "         <tr>\n"
                    + "            <th>fid</th>\n"
                    + "            <th>name</th>\n"
                    + "            <th>static attribute</th>\n"
                    + "         </tr>\n"
                    + "         <tr>\n"
                    + "            <td>${@id}</td>\n"
                    + "            <td>${NAME}</td>\n"
                    + "            <td>I'm a forest</td>\n"
                    + "         </tr>\n"
                    + "      </table>\n"
                    + "      </gft:Template>";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        TemplateTestHelper helper = new TemplateTestHelper();
        helper.setUpTemplate(
                "requestParam('" + FORESTS_GEOJSON_FEATUREINFO + "')='true'",
                SupportedFormat.GEOJSON,
                IOUtils.toInputStream(GEOJSON_FOREST_TEMPLATE, Charsets.UTF_8),
                FORESTS_GEOJSON_FEATUREINFO,
                ".json",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.FORESTS.getLocalPart()));
        helper.setUpTemplate(
                "requestParam('" + LAKES_GEOJSON_FEATUREINFO + "')='true'",
                SupportedFormat.GEOJSON,
                IOUtils.toInputStream(GEOJSON_LAKE_TEMPLATE, Charsets.UTF_8),
                LAKES_GEOJSON_FEATUREINFO,
                ".json",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.LAKES.getLocalPart()));

        helper.setUpTemplate(
                "requestParam('" + FORESTS_JSONLD_FEATUREINFO + "')='true'",
                SupportedFormat.JSONLD,
                IOUtils.toInputStream(JSONLD_FOREST_TEMPLATE, Charsets.UTF_8),
                FORESTS_JSONLD_FEATUREINFO,
                ".json",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.FORESTS.getLocalPart()));
        helper.setUpTemplate(
                "requestParam('" + LAKES_JSONLD_FEATUREINFO + "')='true'",
                SupportedFormat.JSONLD,
                IOUtils.toInputStream(JSONLD_LAKE_TEMPLATE, Charsets.UTF_8),
                LAKES_JSONLD_FEATUREINFO,
                ".json",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.LAKES.getLocalPart()));

        helper.setUpTemplate(
                "requestParam('" + F_TEST_CONTEXT_JSONLD_FEATUREINFO + "')='true'",
                SupportedFormat.JSONLD,
                IOUtils.toInputStream(JSONLD_F_TEST_CONTEXT_TEMPLATE, Charsets.UTF_8),
                F_TEST_CONTEXT_JSONLD_FEATUREINFO,
                ".json",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.FORESTS.getLocalPart()));
        helper.setUpTemplate(
                "requestParam('" + L_TEST_CONTEXT_JSONLD_FEATUREINFO + "')='true'",
                SupportedFormat.JSONLD,
                IOUtils.toInputStream(JSONLD_L_TEST_CONTEXT_TEMPLATE, Charsets.UTF_8),
                L_TEST_CONTEXT_JSONLD_FEATUREINFO,
                ".json",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.LAKES.getLocalPart()));

        helper.setUpTemplate(
                "requestParam('" + FORESTS_GML_FEATUREINFO + "')='true'",
                SupportedFormat.GML,
                IOUtils.toInputStream(GML_FOREST_TEMPLATE, Charsets.UTF_8),
                FORESTS_GML_FEATUREINFO,
                ".xml",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.FORESTS.getLocalPart()));
        helper.setUpTemplate(
                "requestParam('" + LAKES_GML_FEATUREINFO + "')='true'",
                SupportedFormat.GML,
                IOUtils.toInputStream(GML_LAKES_TEMPLATE, Charsets.UTF_8),
                LAKES_GML_FEATUREINFO,
                ".xml",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.LAKES.getLocalPart()));

        helper.setUpTemplate(
                "requestParam('" + FORESTS_HTML_FEATUREINFO + "')='true'",
                SupportedFormat.HTML,
                IOUtils.toInputStream(HTML_FOREST_TEMPLATE, Charsets.UTF_8),
                FORESTS_HTML_FEATUREINFO,
                ".xhtml",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.FORESTS.getLocalPart()));
        helper.setUpTemplate(
                "requestParam('" + LAKES_HTML_FEATUREINFO + "')='true'",
                SupportedFormat.HTML,
                IOUtils.toInputStream(HTML_LAKE_TEMPLATE, Charsets.UTF_8),
                LAKES_HTML_FEATUREINFO,
                ".xhtml",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.LAKES.getLocalPart()));

        helper.setUpTemplate(
                "requestParam('" + FORESTS_HTML_JSONLD_FEATUREINFO + "')='true'",
                SupportedFormat.HTML,
                IOUtils.toInputStream(HTML_FOREST_TEMPLATE_JSON_LD, Charsets.UTF_8),
                FORESTS_HTML_JSONLD_FEATUREINFO,
                ".xhtml",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.FORESTS.getLocalPart()));
        helper.setUpTemplate(
                "requestParam('" + LAKES_HTML_JSONLD_FEATUREINFO + "')='true'",
                SupportedFormat.HTML,
                IOUtils.toInputStream(HTML_LAKE_TEMPLATE_JSON_LD, Charsets.UTF_8),
                LAKES_HTML_JSONLD_FEATUREINFO,
                ".xhtml",
                "cite",
                getCatalog().getFeatureTypeByName(MockData.LAKES.getLocalPart()));
    }

    @Test
    public void testGeoJSON() throws Exception {
        String url =
                "wms?service=wms&version=1.1.1"
                        + "&layers=nature&width=100&height=100&format=image/png"
                        + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002&info_format=application/json"
                        + "&request=GetFeatureInfo&query_layers=nature&x=50&y=50&feature_count=2"
                        + FORESTS_GEOJSON_PARAM
                        + LAKES_GEOJSON_PARAM;
        JSONObject result = (JSONObject) getAsJSON(url);
        JSONArray array = result.getJSONArray("features");
        assertEquals(2, array.size());
        JSONObject lake = array.getJSONObject(0);
        JSONObject forest = array.getJSONObject(1);
        JSONObject lakeProps = lake.getJSONObject("properties");
        JSONObject forestsProps = forest.getJSONObject("properties");
        String lakeAttr = lakeProps.getString("staticAttr");
        String forestAttr = forestsProps.getString("staticAttr");
        assertEquals("I'm a lake", lakeAttr);
        assertEquals("I'm a forest", forestAttr);
    }

    @Test
    public void testJSONLD() throws Exception {
        String url =
                "wms?service=wms&version=1.1.1"
                        + "&layers=nature&width=100&height=100&format=image/png"
                        + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002&info_format=application%2Fld%2Bjson"
                        + "&request=GetFeatureInfo&query_layers=nature&x=50&y=50&feature_count=2"
                        + FORESTS_JSONLD_PARAM
                        + LAKES_JSONLD_PARAM;
        JSONObject result = (JSONObject) getAsJSON(url);
        JSONArray context = result.getJSONArray("@context");
        assertEquals(3, context.size());
        assertEquals("https://geoserver/lake.org", context.getString(0));
        assertNotNull(context.getJSONObject(1));
        assertEquals("https://geoserver/forest.org", context.getString(2));
        JSONArray array = result.getJSONArray("features");
        assertEquals(2, array.size());
        JSONObject lake = array.getJSONObject(0);
        JSONObject forest = array.getJSONObject(1);
        JSONObject lakeProps = lake.getJSONObject("properties");
        JSONObject forestsProps = forest.getJSONObject("properties");
        String lakeAttr = lakeProps.getString("staticAttr");
        String forestAttr = forestsProps.getString("staticAttr");
        assertEquals("I'm a lake", lakeAttr);
        assertEquals("I'm a forest", forestAttr);
    }

    @Test
    public void testGML() throws Exception {
        String url =
                "wms?service=wms&version=1.1.1"
                        + "&layers=nature&width=100&height=100&format=image/png"
                        + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002&info_format=text/xml; subtype=gml/3.1.1"
                        + "&request=GetFeatureInfo&query_layers=nature&x=50&y=50&feature_count=2"
                        + FORESTS_GML_PARAM
                        + LAKES_GML_PARAM;

        Document dom = getAsDOM(url);
        // checks templates have been used
        NodeList nodes = dom.getElementsByTagName("cite:staticAttr");
        assertEquals("I'm a lake", nodes.item(0).getTextContent().trim());
        assertEquals("I'm a forest", nodes.item(1).getTextContent().trim());
    }

    @Test
    public void testHTML() throws Exception {
        String url =
                "wms?service=wms&version=1.1.1"
                        + "&layers=nature&width=100&height=100&format=image/png"
                        + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002&info_format=text/html"
                        + "&request=GetFeatureInfo&query_layers=nature&x=50&y=50&feature_count=2"
                        + LAKES_HTML_PARAM
                        + FORESTS_HTML_PARAM;

        String result = getAsString(url);
        // checks templates have been used
        assertTrue(result.trim().contains("<td>I'm a lake</td>"));
        assertTrue(result.trim().contains("<td>I'm a forest</td>"));
    }

    @Test
    public void testJSONLDObjectContextUnion() throws Exception {
        String url =
                "wms?service=wms&version=1.1.1"
                        + "&layers=nature&width=100&height=100&format=image/png"
                        + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002&info_format=application%2Fld%2Bjson"
                        + "&request=GetFeatureInfo&query_layers=nature&x=50&y=50&feature_count=2"
                        + F_TEST_CONTEXT_JSONLD_PARAM
                        + L_TEST_CONTEXT_JSONLD_PARAM;
        JSONObject result = (JSONObject) getAsJSON(url);
        JSONObject context = result.getJSONObject("@context");
        assertEquals(7, context.size());
        assertTrue(context.has("features"));
        JSONArray array = result.getJSONArray("features");
        assertEquals(2, array.size());
        JSONObject lake = array.getJSONObject(0);
        JSONObject forest = array.getJSONObject(1);
        JSONObject lakeProps = lake.getJSONObject("properties");
        JSONObject forestsProps = forest.getJSONObject("properties");
        String lakeAttr = lakeProps.getString("staticAttr");
        String forestAttr = forestsProps.getString("staticAttr");
        assertEquals("I'm a lake", lakeAttr);
        assertEquals("I'm a forest", forestAttr);
    }

    @Test
    public void testGetFeatureInfoCoverageNotFails() throws Exception {
        String url =
                "wms?service=wms&version=1.1.1"
                        + "&layers=wcs:World&width=100&height=100&format=image/png"
                        + "&srs=epsg:4326&bbox=-180,-90,180,90&info_format=application/json"
                        + "&request=GetFeatureInfo&query_layers=wcs:World&x=50&y=50";
        JSONObject result = (JSONObject) getAsJSON(url);
        assertNotNull(result);
        assertEquals(6, result.size());
    }

    @Test
    public void testHTMLWithJSONLD() throws Exception {
        String url =
                "wms?service=wms&version=1.1.1"
                        + "&layers=nature&width=100&height=100&format=image/png"
                        + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002&info_format=text/html"
                        + "&request=GetFeatureInfo&query_layers=nature&x=50&y=50&feature_count=2"
                        + LAKES_HTML_JSONLD_PARAM
                        + FORESTS_HTML_JSONLD_PARAM
                        + LAKES_JSONLD_PARAM
                        + FORESTS_JSONLD_PARAM;

        String result = getAsString(url);
        String trimmed = result.trim();
        // check jsonld is present
        assertTrue(trimmed.contains("@context"));
        assertTrue(trimmed.contains("\"staticAttr\":\"I'm a lake\""));
        assertTrue(trimmed.contains("\"staticAttr\":\"I'm a forest\""));
        // checks html template
        assertTrue(trimmed.contains("<td>I'm a lake</td>"));
        assertTrue(trimmed.contains("<td>I'm a forest</td>"));
    }
}
