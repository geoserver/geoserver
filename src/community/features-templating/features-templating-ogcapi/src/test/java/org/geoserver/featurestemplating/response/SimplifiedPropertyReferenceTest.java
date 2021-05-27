package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.FeatureChainingMockData;
import org.junit.Test;
import org.w3c.dom.Document;

public class SimplifiedPropertyReferenceTest extends TemplateComplexTestSupport {

    @Test
    public void testGML() throws IOException {
        setUpComplex("SimplifiedPropertyNames.xml", mappedFeature);
        Document doc =
                getAsDOM(
                        "ogc/features/collections/gsml:MappedFeature"
                                + "/items?f=application%2Fgml%2Bxml%3Bversion%3D3.2");
        assertXpathCount(5, "//gsml:MappedFeature", doc);
        assertXpathCount(5, "//gsml:samplingFrame//@xlink:href", doc);
        assertXpathCount(5, "//gsml:MappedFeature/gsml:geometry/gml:Surface", doc);
        assertXpathCount(4, "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit", doc);
        assertXpathCount(
                4,
                "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gml:description",
                doc);
        // filter on array element lithology
        assertXpathCount(2, "//gsml:lithology", doc);
    }

    @Test
    public void testJsonLd() throws Exception {
        setUpComplex(
                "SimplifiedPropertyNames.json",
                "gsml",
                TemplateIdentifier.JSONLD.getFilename(),
                mappedFeature);
        String path =
                "ogc/features/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fld%2Bjson";
        JSONObject result = (JSONObject) getJsonLd(path);
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());
        JSONObject feature = features.getJSONObject(0);
        assertEquals("mf1", feature.getString("@id"));
        assertEquals("GUNTHORPE FORMATION", feature.getString("name"));
        String geometry = feature.getJSONObject("geometry").getString("wkt");
        assertNotNull(geometry);
        JSONObject geologicUnit = feature.getJSONObject("gsml:GeologicUnit");
        assertEquals(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                geologicUnit.getString("description"));
        JSONObject composition = geologicUnit.getJSONArray("gsml:composition").getJSONObject(0);

        JSONObject role = composition.getJSONObject("gsml:role");

        assertEquals("staticValue", role.getString("staticValue"));
        JSONObject lithology = composition.getJSONArray("lithology").getJSONObject(0);
        String value = lithology.getJSONObject("name").getString("value");
        assertEquals("name_cc_5", value);
    }

    @Test
    public void testFlatGeoJSON() throws Exception {
        setUpComplex(
                "FlatSimplifiedPropertyNames.json",
                "gsml",
                TemplateIdentifier.GEOJSON.getFilename(),
                mappedFeature);
        String path =
                "ogc/features/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fgeo%2Bjson";
        JSONObject result = (JSONObject) getJson(path);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());
        JSONObject feature = features.getJSONObject(0);
        assertNotNull(feature.getJSONObject("geometry"));
        assertEquals("mf1", feature.getString("@id"));
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("FeatureName: GUNTHORPE FORMATION", properties.getString("name"));
        assertEquals(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                properties.getString("gsml:GeologicUnit_description"));
        assertEquals(
                "fictitious component",
                properties.getString(
                        "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_gsml:role_value"));
        assertEquals(
                "name_cc_5",
                properties.getString(
                        "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_name"));
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.GML32.getFilename();
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
