/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class JSONLDGetComplexFeaturesResponseWFSTest extends TemplateComplexTestSupport {

    private static final String MF_JSON_LD_TEMPLATE = "MappedFeatureJSONLD";

    private static final String MF_JSON_LD_PARAM = "&" + MF_JSON_LD_TEMPLATE + "=true";

    private static final String MF_JSON_LD_FILTERS = "MappedFeatureJSONLDFilters";

    private static final String MF_JSON_LD_FILTERS_PARAM = "&" + MF_JSON_LD_FILTERS + "=true";

    private static final String GU_JSON_LD_FILTERS = "GeologicFilterJSONLD";

    private static final String GU_JSON_LD_FILTERS_PARAM = "&" + GU_JSON_LD_FILTERS + "=true";

    private static final String INVALID = "InvalidJSONLD";
    private static final String INVALID_PARAM = "&" + INVALID + "=true";

    private static final String INVALID_GU = "InvalidJSONLDGu";
    private static final String INVALID_GU_PARAM = "&" + INVALID_GU + "=true";

    private static final String MF_JSON_LD_DEF_ENC = "MfJsonLdDefEncoding";

    private static final String MF_JSON_LD_DEF_ENC_PARAM = "&" + MF_JSON_LD_DEF_ENC + "=true";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        String templateMappedFeature = "MappedFeature.json";
        setUpTemplate(
                "requestParam('" + MF_JSON_LD_TEMPLATE + "')='true'",
                SupportedFormat.JSONLD,
                templateMappedFeature,
                MF_JSON_LD_TEMPLATE,
                ".json",
                "gsml",
                mappedFeature);

        String mappedFeatureFilter = "MappedFeatureIteratingAndCompositeFilter.json";
        setUpTemplate(
                "requestParam('" + MF_JSON_LD_FILTERS + "')='true'",
                SupportedFormat.JSONLD,
                mappedFeatureFilter,
                MF_JSON_LD_FILTERS,
                ".json",
                "gsml",
                mappedFeature);

        String geologicUnitFilter = "GeologicUnitDynamicStaticFilter.json";
        FeatureTypeInfo geologic = getCatalog().getFeatureTypeByName("gsml", "GeologicUnit");

        setUpTemplate(
                "requestParam('" + GU_JSON_LD_FILTERS + "')='true'",
                SupportedFormat.JSONLD,
                geologicUnitFilter,
                GU_JSON_LD_FILTERS,
                ".json",
                "gsml",
                geologic);

        String invalid = "FirstParentFeature_invalid.json";
        FeatureTypeInfo parentF = getCatalog().getFeatureTypeByName("ex", "FirstParentFeature");
        setUpTemplate(
                "requestParam('" + INVALID + "')='true'",
                SupportedFormat.JSONLD,
                invalid,
                INVALID,
                ".json",
                "ex",
                parentF);

        String invalidGeologic = "GeologicUnit_invalid.json";

        setUpTemplate(
                "requestParam('" + INVALID_GU + "')='true'",
                SupportedFormat.JSONLD,
                invalidGeologic,
                INVALID_GU,
                ".json",
                "gsml",
                geologic);

        String mappedFeatureDefEncoding = "MfJsonLdDefEncoding.json";
        setUpTemplate(
                "requestParam('" + MF_JSON_LD_DEF_ENC + "')='true'",
                SupportedFormat.JSONLD,
                mappedFeatureDefEncoding,
                MF_JSON_LD_DEF_ENC,
                ".json",
                "gsml",
                mappedFeature);
    }

    @Test
    public void testJsonLdResponse() throws Exception {
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson").append(MF_JSON_LD_PARAM);
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
    public void testJsonLdResponseWithoutTemplate() throws Exception {
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=ex:SecondParentFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        MockHttpServletResponse response = getAsServletResponse(sb.toString());
        assertTrue(response.getContentAsString().contains("No template found for feature type"));
    }

    @Test
    public void testJsonLdQueryWithGET() throws Exception {
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append(
                                "&cql_filter=features.gsml:GeologicUnit.description = 'Olivine basalt'")
                        .append(MF_JSON_LD_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
    }

    @Test
    public void testJsonLdQueryPointingToExpr() throws Exception {
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append("&cql_filter= features.geometry.wkt IS NULL")
                        .append(MF_JSON_LD_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 0);
    }

    @Test
    public void testJsonLdQueryWithPOST() throws Exception {
        StringBuilder xml =
                new StringBuilder("<wfs:GetFeature ")
                        .append(" service=\"WFS\" ")
                        .append(" outputFormat=\"application/ld+json\" ")
                        .append(" version=\"1.0.0\" ")
                        .append(" xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML:2.0\" ")
                        .append(" xmlns:wfs=\"http://www.opengis.net/wfs\" ")
                        .append(" xmlns:ogc=\"http://www.opengis.net/ogc\" ")
                        .append(">")
                        .append(" <wfs:Query typeName=\"gsml:MappedFeature\">")
                        .append(" <ogc:Filter><ogc:PropertyIsEqualTo> ")
                        .append(
                                "<ogc:PropertyName>features.gsml:GeologicUnit.description</ogc:PropertyName>")
                        .append("<ogc:Literal>Olivine basalt</ogc:Literal>")
                        .append("</ogc:PropertyIsEqualTo></ogc:Filter></wfs:Query>")
                        .append("</wfs:GetFeature>");
        JSONObject result =
                (JSONObject)
                        postJsonLd("wfs?" + "&" + MF_JSON_LD_TEMPLATE + "=true", xml.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
    }

    @Test
    public void testInvalidTemplateResponse() throws Exception {
        StringBuilder sb = new StringBuilder("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=ex:FirstParentFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson").append(INVALID_PARAM);
        MockHttpServletResponse response = getAsServletResponse(sb.toString());
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "Failed to validate template for feature type FirstParentFeature. "
                                        + "Failing attribute is Key: @id Value: &amp;quot;invalid/id&amp;quot;"));
    }

    @Test
    public void testInvalidTemplateResponse2() throws Exception {
        // check that validation fails for an invalid attribute down in the template
        // the failing attribute also point to a previous context attribute (../)
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:GeologicUnit&outputFormat=");
        sb.append("application%2Fld%2Bjson").append(INVALID_GU_PARAM);
        MockHttpServletResponse resp = getAsServletResponse(sb.toString());
        assertTrue(
                resp.getContentAsString()
                        .contains(
                                "Failed to validate template for feature type GeologicUnit. "
                                        + "Failing attribute is Key: invalidAttr Value: &amp;quot;gsml:notExisting&amp;quot;"));
    }

    @Test
    public void testJsonLdWithFilterOnIteratingAndComposite() throws Exception {
        // test filter capabilities in json-ld template
        // filter on composite "$filter": "xpath('gml:description') = 'Olivine basalt'"
        // filter on iterating "$filter": "xpath('gsml:ControlledConcept/@id') = 'cc.2'"
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append(MF_JSON_LD_FILTERS_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        int size = features.size();
        for (int i = 0; i < size; i++) {
            JSONObject feature = features.getJSONObject(i);
            if (i + 1 != 4) {
                assertEquals(5, feature.size());
                assertNull(feature.get("gsml:GeologicUnit"));
            } else {
                // second to last feature is the one having the condition matched
                // thus GeologicUnit got encoded.
                assertEquals(6, feature.size());
                Object geologicUnit = feature.get("gsml:GeologicUnit");
                assertNotNull(feature.get("gsml:GeologicUnit"));

                JSONArray lithologyAr =
                        (JSONArray)
                                ((JSONObject) geologicUnit)
                                        .getJSONArray("gsml:composition")
                                        .getJSONObject(0)
                                        .getJSONArray("gsml:compositionPart")
                                        .getJSONObject(0)
                                        .get("lithology");
                assertEquals(1, lithologyAr.size());
                JSONObject lithology = lithologyAr.getJSONObject(0);
                // lithology element with cc2.2 is the one matching the filter
                assertEquals("cc.2", lithology.getString("@id"));
                assertEquals("name_2", lithology.getJSONObject("name").getString("value"));
            }
        }
    }

    @Test
    public void testJsonLdWithFilterOnStaticAndDynamic() throws Exception {
        // filter on dynamic:
        // "$filter{xpath('gml:description')='Olivine basalt'},${gml:description}"
        // filter on static
        //  "@codeSpace": "$filter{xpath('../../gml:description')='Olivine
        // basalt'},urn:cgi:classifierScheme:Example:CompositionPartRole"
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:GeologicUnit&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append(GU_JSON_LD_FILTERS_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        int size = features.size();
        assertEquals(3, size);
        for (int i = 0; i < size; i++) {
            JSONObject geologicUnit = features.getJSONObject(i);
            Object description = geologicUnit.get("description");
            // filter on dynamic
            if (description != null) {
                assertEquals("Olivine basalt", description);
            }
            JSONArray composition = geologicUnit.getJSONArray("gsml:composition");
            for (int j = 0; j < composition.size(); j++) {
                JSONArray compositionPart =
                        (JSONArray) ((JSONObject) composition.get(j)).get("gsml:compositionPart");
                assertTrue(compositionPart.size() > 0);
                JSONObject role = compositionPart.getJSONObject(0).getJSONObject("gsml:role");
                Object codeSpace = role.get("@codeSpace");
                // check filter on the static value
                if (description == null) {
                    assertNull(codeSpace);
                } else {
                    assertNotNull(codeSpace);
                }
            }
        }
    }

    @Test
    public void testJsonLdNotEncodingArrayFilteredOut() throws Exception {
        // filter on dynamic:
        // "$filter{xpath('gml:description')='Olivine basalt'},${gml:description}"
        // filter on static
        //  "@codeSpace": "$filter{xpath('../../gml:description')='Olivine
        // basalt'},urn:cgi:classifierScheme:Example:CompositionPartRole"
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:GeologicUnit&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append(GU_JSON_LD_FILTERS_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        int size = features.size();
        assertEquals(3, size);
        for (int i = 0; i < size; i++) {
            JSONArray composition = features.getJSONObject(i).getJSONArray("gsml:composition");
            for (int j = 0; j < composition.size(); j++) {
                JSONArray compositionPart =
                        (JSONArray) ((JSONObject) composition.get(j)).get("gsml:compositionPart");
                for (int z = 0; z < compositionPart.size(); z++) {
                    assertNull(compositionPart.getJSONObject(z).get("lithology"));
                }
            }
        }
    }

    @Test
    public void testJsonLdResponseNonStringValues() throws Exception {
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson").append(MF_JSON_LD_DEF_ENC_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());

        // gets the first feature to check if some non string attributes have been encoded with
        // proper type.
        JSONObject feature = features.getJSONObject(0);
        assertTrue(feature.getJSONObject("gsml:positionalAccuracy").get("value") instanceof Double);
        assertTrue(
                feature.getJSONObject("gsml:positionalAccuracy").getJSONArray("valueArray").get(0)
                        instanceof Double);
        Object geom = feature.get("geometry");
        // no WKT should be a JSONObject
        assertTrue(geom instanceof JSONObject);
        JSONObject geomJSON = (JSONObject) geom;
        Object coors = geomJSON.get("coordinates");
        assertTrue(coors instanceof JSONArray);
    }
}
