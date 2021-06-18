package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.FeatureChainingMockData;
import org.junit.Test;
import org.w3c.dom.Document;

public class ManagedComplexFeaturesResponseTest extends JSONLDGetComplexFeaturesResponseTest {

    @Test
    public void testGMLResponse() throws Exception {
        setUpComplex(
                "ManagedMappedFeatureGML32.xml",
                "gsml",
                TemplateIdentifier.GML32.getFilename(),
                mappedFeature);
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application%2Fgml%2Bxml%3B%20version%3D3.2");
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
        setUpMappedFeature();
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson");
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
