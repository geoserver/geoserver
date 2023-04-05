/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.FeatureChainingMockData;
import org.junit.Test;
import org.w3c.dom.Document;

public class ComplexGetFeatureInfoTest extends TemplateComplexTestSupport {

    private static final String MF_GML_FEATUREINFO = "GMLFeatureInfoMappedFeature";
    private static final String MF_GML_PARAM = "&" + MF_GML_FEATUREINFO + "=true";

    private static final String MF_GEOJSON_FEATUREINFO = "GeoJSONFeatureInfoMappedFeature";
    private static final String MF_GEOJSON_PARAM = "&" + MF_GEOJSON_FEATUREINFO + "=true";

    private static final String MF_JSONLD_FEATUREINFO = "JSONLDFeatureInfoMappedFeature";
    private static final String MF_JSONLD_PARAM = "&" + MF_JSONLD_FEATUREINFO + "=true";

    private static final String MF_HTML_FEATUREINFO = "HTMLFeatureInfoMappedFeature";
    private static final String MF_HTML_PARAM = "&" + MF_HTML_FEATUREINFO + "=true";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        String templateMappedFeatureGML = "ManagedMappedFeatureGML32.xml";
        setUpTemplate(
                "requestParam('" + MF_GML_FEATUREINFO + "')='true'",
                SupportedFormat.GML,
                templateMappedFeatureGML,
                MF_GML_FEATUREINFO,
                ".xml",
                "gsml",
                mappedFeature);

        String templateMappedFeatureGeoJSON = "ManagedMappedFeatureGeoJSON.json";
        setUpTemplate(
                "requestParam('" + MF_GEOJSON_FEATUREINFO + "')='true'",
                SupportedFormat.GEOJSON,
                templateMappedFeatureGeoJSON,
                MF_GEOJSON_FEATUREINFO,
                ".json",
                "gsml",
                mappedFeature);

        String templateMappedFeatureJSONLD = "ManagedMappedFeatureJSONLD.json";

        setUpTemplate(
                "requestParam('" + MF_JSONLD_FEATUREINFO + "')='true'",
                SupportedFormat.JSONLD,
                templateMappedFeatureJSONLD,
                MF_JSONLD_FEATUREINFO,
                ".json",
                "gsml",
                mappedFeature);

        String templateMappedFeatureHTML = "HTMLMappedFeature.xhtml";

        setUpTemplate(
                "requestParam('" + MF_HTML_FEATUREINFO + "')='true'",
                SupportedFormat.HTML,
                templateMappedFeatureHTML,
                MF_HTML_FEATUREINFO,
                ".xhtml",
                "gsml",
                mappedFeature);
    }

    @Test
    public void testHTML() throws Exception {
        String request =
                "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=text/html"
                        + MF_HTML_PARAM;
        org.jsoup.nodes.Document doc = getAsJSoup(request);
        assertEquals(1, doc.select("script").size());
        assertEquals(1, doc.select("style").size());
        assertEquals(1, doc.select("span.caret:contains(MappedFeature)").size());
        assertEquals(1, doc.select("li ul li:contains(mf2)").size());

        assertEquals(1, doc.select("ul li ul li ul li:contains(MERCIA MUDSTONE GROUP)").size());

        assertEquals(1, doc.select("span:contains(Shape)").size());

        assertEquals(1, doc.select("ul li ul li span:contains(Specifications)").size());
        assertEquals(1, doc.select("ul li ul li span:contains(Geologic Unit)").size());
        assertEquals(1, doc.select("ul li ul li span:contains(Purpose)").size());
        assertEquals(1, doc.select("ul li ul li ul li ul li ul li:contains(instance)").size());

        assertEquals(
                1,
                doc.select("ul li ul li ul li ul li ul li:contains(Yaugher Volcanic Group 1)")
                        .size());
        assertEquals(
                1,
                doc.select("ul li ul li ul li ul li ul li:contains(Yaugher Volcanic Group 2)")
                        .size());

        assertEquals(1, doc.select("ul li ul li ul li ul li ul li:contains(-Py)").size());
        assertEquals(
                2, doc.select("ul li ul li ul li ul li span:contains(Composition Parts)").size());
        assertEquals(2, doc.select("ul li ul li ul li ul li ul li span:contains(Part)").size());
        assertEquals(2, doc.select("ul li ul li ul li ul li ul li span:contains(Role)").size());
        assertEquals(
                2,
                doc.select(
                                "ul li ul li ul li ul li ul li ul li ul li:contains(interbedded component)")
                        .size());
    }

    @Test
    public void testJSONLD() throws Exception {
        String request =
                "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=application%2Fld%2Bjson"
                        + MF_JSONLD_PARAM;
        JSONObject result = (JSONObject) getJsonLd(request);
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(1, features.size());
        JSONObject feature = (JSONObject) features.get(0);
        checkMappedFeatureJSON(feature);
        assertEquals(feature.get("nullObject").toString(), "null");
        assertEquals(feature.get("nullText").toString(), "null");
        assertEquals(JSONNull.class, feature.get("nullObject").getClass());
        assertEquals(JSONNull.class, feature.get("nullText").getClass());
    }

    @Test
    public void testGeoJSON() throws Exception {
        String request =
                "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=application/json"
                        + MF_GEOJSON_PARAM;
        JSONObject result = (JSONObject) getJson(request);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(1, features.size());
        JSONObject feature = (JSONObject) features.get(0);
        checkMappedFeatureJSON(feature);
        assertEquals(feature.get("nullObject").toString(), "null");
        assertEquals(feature.get("nullText").toString(), "null");
    }

    @Test
    public void testGML() {
        String request =
                "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=text/xml"
                        + MF_GML_PARAM;
        Document doc = getAsDOM(request);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathCount(1, "//gsml:samplingFrame//@xlink:href", doc);
        assertXpathCount(1, "//gsml:MappedFeature/gsml:geometry/gml:Surface", doc);
        assertXpathCount(1, "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit", doc);

        assertXpathCount(
                1,
                "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gml:description/@xlink:href",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gsml:staticContent",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gsml:staticContent/@xlink:title",
                doc);

        // filter on array element lithology
        assertXpathCount(1, "//gsml:lithology", doc);
        assertXpathCount(1, "//gml:emptyText", doc);
        assertXpathEvaluatesTo("", "//gml:emptyText", doc);
    }

    protected AbstractAppSchemaMockData createTestData() {
        AbstractAppSchemaMockData mockData =
                new FeatureChainingMockData() {
                    @Override
                    public Map<String, String> getNamespaces() {
                        Map<String, String> namespaces = new HashMap<>();
                        namespaces.put("gml", "http://www.opengis.net/gml/3.2");
                        namespaces.put("wfs", "http://www.opengis.net/wfs/2.0");
                        namespaces.put("gsml", "urn:cgi:xmlns:CGI:GeoSciML:2.0");
                        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
                        return namespaces;
                    }
                };
        mockData.addStyle("Default", "styles/Default.sld");
        return mockData;
    }
}
