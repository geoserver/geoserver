package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.FeatureChainingMockData;
import org.junit.Test;
import org.w3c.dom.Document;

public class ManagedComplexFeaturesResponseTest extends TemplateComplexTestSupport {

    private static final String MF_GML_MANAGED = "GMLManagedMappedFeature";
    private static final String MF_GML_PARAM = "&" + MF_GML_MANAGED + "=true";

    private static final String MF_GEOJSON_MANAGED = "GeoJSONManagedMappedFeature";
    private static final String MF_GEOJSON_PARAM = "&" + MF_GEOJSON_MANAGED + "=true";

    private static final String MF_JSONLD_MANAGED = "JSONLDManagedMappedFeature";
    private static final String MF_JSONLD_PARAM = "&" + MF_JSONLD_MANAGED + "=true";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        String templateMappedFeatureGML = "ManagedMappedFeatureGML32.xml";
        setUpTemplate(
                "requestParam('" + MF_GML_MANAGED + "')='true'",
                SupportedFormat.GML,
                templateMappedFeatureGML,
                MF_GML_MANAGED,
                ".xml",
                "gsml",
                mappedFeature);

        String templateMappedFeatureGeoJSON = "ManagedMappedFeatureGeoJSON.json";
        setUpTemplate(
                "requestParam('" + MF_GEOJSON_MANAGED + "')='true'",
                SupportedFormat.GEOJSON,
                templateMappedFeatureGeoJSON,
                MF_GEOJSON_MANAGED,
                ".json",
                "gsml",
                mappedFeature);

        String templateMappedFeatureJSONLD = "ManagedMappedFeatureJSONLD.json";

        setUpTemplate(
                "requestParam('" + MF_JSONLD_MANAGED + "')='true'",
                SupportedFormat.JSONLD,
                templateMappedFeatureJSONLD,
                MF_JSONLD_PARAM,
                ".json",
                "gsml",
                mappedFeature);
    }

    @Test
    public void testGMLResponse() throws Exception {
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application%2Fgml%2Bxml%3B%20version%3D3.2");
        sb.append(MF_GML_PARAM);
        Document doc = getAsDOM(sb.toString());
        assertXpathCount(5, "//gsml:MappedFeature", doc);
        assertXpathCount(5, "//gsml:samplingFrame//@xlink:href", doc);
        assertXpathCount(5, "//gsml:MappedFeature/gsml:geometry/gml:Surface", doc);
        assertXpathCount(4, "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit", doc);

        assertXpathCount(
                4,
                "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gml:description/@xlink:href",
                doc);
        assertXpathCount(
                4,
                "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gsml:staticContent",
                doc);
        assertXpathCount(
                4,
                "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gsml:staticContent/@xlink:title",
                doc);

        // filter on array element lithology
        assertXpathCount(2, "//gsml:lithology", doc);
    }

    @Test
    public void testJsonLdResponse() throws Exception {
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        sb.append(MF_JSONLD_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            checkMappedFeatureJSON(feature);
        }
    }

    @Test
    public void testGeoJSONResponse() throws Exception {
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application/json");
        sb.append(MF_GEOJSON_PARAM);
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            checkMappedFeatureJSON(feature);
        }
    }

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new FeatureChainingMockData() {
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
    }
}
