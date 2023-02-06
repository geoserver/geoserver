/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class JSONLDGetComplexFeaturesResponseAPITest extends TemplateComplexTestSupport {

    protected static final String MF_JSON_LD = "MappedFeatureJSONLD";

    protected static final String MF_JSON_LD_FILTERS = "MappedFeatureJSONLDFilters";

    private static final String MF_JSON_LD_PARAM = "&" + MF_JSON_LD + "=true";

    private static final String MF_JSON_LD_FILTER_PARAM = "&" + MF_JSON_LD_FILTERS + "=true";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        String templateMappedFeature = "MappedFeature.json";
        setUpTemplate(
                "requestParam('" + MF_JSON_LD + "')='true'",
                SupportedFormat.JSONLD,
                templateMappedFeature,
                MF_JSON_LD,
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
    }

    @Test
    public void testJsonLdResponseOGCAPI() throws Exception {
        String path =
                "ogc/features/v1/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fld%2Bjson"
                        + MF_JSON_LD_PARAM;
        JSONObject result = (JSONObject) getJsonLd(path);
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
    public void testJsonLdQueryOGCAPI() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&filter-lang=cql-text")
                        .append(
                                "&filter= features.gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology.name.value")
                        .append(" = 'name_2' ")
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
    public void testJsonLdQueryEnvSubstitutionOnAttributeName() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&env=id:envId")
                        .append(MF_JSON_LD_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertTrue(feature.has("envId"));
        }
    }

    @Test
    public void testJsonLdQueryEnvSubstitutionOnSource() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&env=source:notComposition")
                        .append(MF_JSON_LD_PARAM);
        MockHttpServletResponse response = getAsServletResponse(sb.toString());
        // source changed validation should fail
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "Failed to validate template for feature type MappedFeature. "
                                        + "Failing attribute is Source: gsml:notComposition"));
    }

    @Test
    public void testJsonLdQueryEnvSubstitutionOnAttribute() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&env=positionalAccuracyType:CGI_NotNumericValue")
                        .append(MF_JSON_LD_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            String typeValue = feature.getJSONObject("gsml:positionalAccuracy").getString("type");
            assertEquals("CGI_NotNumericValue", typeValue);
        }
    }

    @Test
    public void testJsonLdQueryEnvSubstitutionOnXpath() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&env=previous:@id")
                        .append(MF_JSON_LD_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            // mf5 setup is not complete, skip it
            if ("mf5".equals(feature.get("@id"))) continue;
            JSONObject geologicUnit = feature.getJSONObject("gsml:GeologicUnit");
            String geologicUnitId = geologicUnit.getString("@id");
            assertNotNull(geologicUnitId);
            JSONArray composition = geologicUnit.getJSONArray("gsml:composition");
            assertTrue(composition.size() > 0);
            for (int j = 0; j < composition.size(); j++) {
                JSONObject compositionObj = composition.getJSONObject(j);
                assertEquals(geologicUnitId, compositionObj.getString("previousContextValue"));
            }
        }
    }

    @Test
    public void testJsonLdQueryPointingToArray() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&filter= features.gsml:positionalAccuracy.valueArray1 > 120")
                        .append(MF_JSON_LD_PARAM);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONArray features = result.getJSONArray("features");
        assertEquals(3, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject f = features.getJSONObject(i);
            JSONArray values =
                    f.getJSONObject("gsml:positionalAccuracy").getJSONArray("valueArray");
            assertTrue(values.getInt(0) > 120);
        }
    }

    @Test
    public void testJsonLdContextValidationFails() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&validation=true")
                        .append(MF_JSON_LD_PARAM);
        MockHttpServletResponse result = getAsServletResponse(sb.toString());
        String strResult = result.getContentAsString();
        assertTrue(
                strResult.contains(
                        "Validation failed. Unable to resolve the following fields against the @context: proportion,previousContextValue,lithology,valueArray,type,value,@codeSpace."));
    }

    @Test
    public void testJsonLdResponseOGCAPISingleFeature() throws Exception {
        String path =
                "ogc/features/v1/collections/"
                        + "gsml:MappedFeature"
                        + "/items/mf4?f=application%2Fld%2Bjson"
                        + MF_JSON_LD_PARAM;
        JSONObject result = (JSONObject) getJsonLd(path);
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        assertFalse(result.has("features"));
        assertEquals("mf4", result.getString("@id"));
        assertEquals("MURRADUC BASALT", result.getString("name"));
        assertTrue(result.has("@type"));
        assertTrue(result.has("gsml:positionalAccuracy"));
        assertTrue(result.has("gsml:GeologicUnit"));
        assertTrue(result.has("geometry"));
    }

    @Test
    public void testContentNegotiationByProfile() throws Exception {
        String profile = "header('Accept-Profile')='http://my-test-profile/ld+json'";
        FeatureTypeInfo mappedFeature = getCatalog().getFeatureTypeByName("gsml", "MappedFeature");
        setUpTemplate(
                null,
                profile,
                SupportedFormat.JSONLD,
                "MappedFeature.json",
                "ProfileJsonLDTemplate",
                ".json",
                "gsml",
                mappedFeature);
        String path =
                "ogc/features/v1/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fld%2Bjson";
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("GET");
        request.setContent(new byte[] {});
        request.addHeader("Accept-Profile", "http://my-test-profile/ld+json");
        MockHttpServletResponse response = dispatch(request, null);
        JSONObject result = (JSONObject) json(response);
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
    public void testJsonLdOgcFilterConcatenation() throws Exception {
        // test templates filter concatenation with backward mapping
        // "$filter": "xpath('gml:description') = 'Olivine basalt'",

        String cqlFilter =
                "features.gsml:GeologicUnit.gsml:composition.gsml:compositionPart.gsml:role.value = ";

        // if querying for fictitious component expecting no result because the And condition
        // with the template filter
        JSONArray features =
                getResultFilterConcatenated(
                        cqlFilter, "'fictitious component'", MF_JSON_LD_FILTER_PARAM);

        assertEquals(0, features.size());

        // if querying for interbedded component expecting 1 feature as result;
        // this time the result of template filter has this gms:role value.
        JSONArray features2 =
                getResultFilterConcatenated(
                        cqlFilter, "'interbedded component'", MF_JSON_LD_FILTER_PARAM);
        assertEquals(1, features2.size());
    }

    private JSONArray getResultFilterConcatenated(
            String cql_filter, String equalsTo, String reqParam) throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&filter-lang=cql-text")
                        .append("&filter= ")
                        .append(cql_filter)
                        .append(equalsTo);
        if (reqParam != null) sb.append(reqParam);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        return result.getJSONArray("features");
    }
}
