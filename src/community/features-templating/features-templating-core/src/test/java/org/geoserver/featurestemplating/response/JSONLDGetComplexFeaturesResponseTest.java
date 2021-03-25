/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JSONLDGetComplexFeaturesResponseTest extends TemplateJSONComplexTestSupport {

    protected void checkMappedFeatureJSON(JSONObject feature) {
        assertNotNull(feature);
        String id = feature.getString("@id");
        assertNotNull(id);
        JSONObject geom = (JSONObject) feature.get("geometry");
        assertNotNull(geom);
        assertEquals(String.valueOf(geom.get("@type")), "Polygon");
        assertNotNull(geom.get("wkt"));
        checkSimpleArrayWithDynamicValues(feature);
        if ("mf5".equals(id)) return; // this feature lacks the whole setup
        JSONObject geologicUnit = feature.getJSONObject("gsml:GeologicUnit");
        String geologicUnitDescr = geologicUnit.getString("description");
        assertNotNull(geologicUnitDescr);
        JSONArray composition = geologicUnit.getJSONArray("gsml:composition");
        assertTrue(composition.size() > 0);
        for (int i = 0; i < composition.size(); i++) {
            JSONObject compositionObj = composition.getJSONObject(i);

            String previousContextEl = compositionObj.getString("previousContextValue");
            // check an ${../xpath} expression to be equal to the one
            // acquired previously
            assertEquals(geologicUnitDescr, previousContextEl);
            JSONArray compositionPart =
                    (JSONArray) ((JSONObject) composition.get(i)).get("gsml:compositionPart");
            assertTrue(compositionPart.size() > 0);
            for (int j = 0; j < compositionPart.size(); j++) {
                JSONObject role = compositionPart.getJSONObject(j).getJSONObject("gsml:role");
                assertNotNull(role);
                JSONObject proportion =
                        compositionPart.getJSONObject(j).getJSONObject("proportion");
                assertNotNull(proportion);
                JSONArray lithology = (JSONArray) compositionPart.getJSONObject(j).get("lithology");
                assertTrue(lithology.size() > 0);
            }
        }
    }

    protected void checkSimpleArrayWithDynamicValues(JSONObject feature) {
        JSONArray arrayWithDynamic =
                feature.getJSONObject("gsml:positionalAccuracy").getJSONArray("valueArray");
        String value = feature.getJSONObject("gsml:positionalAccuracy").getString("value");

        assertEquals(value, arrayWithDynamic.getString(0));
        assertEquals("someStaticVal", arrayWithDynamic.getString(1));
        assertEquals("duplicated value: " + value, arrayWithDynamic.getString(2));
    }

    @Override
    @After
    public void cleanup() {
        Resource res =
                dd.getResourceLoader()
                        .get(
                                "workspaces/gsml/"
                                        + mappedFeature.getStore().getName()
                                        + "/"
                                        + mappedFeature.getName()
                                        + "/"
                                        + TemplateIdentifier.JSONLD.getFilename());
        if (res != null) res.delete();

        Resource res2 =
                dd.getResourceLoader()
                        .get(
                                "workspaces/gsml/"
                                        + geologicUnit.getStore().getName()
                                        + "/"
                                        + geologicUnit.getName()
                                        + "/"
                                        + TemplateIdentifier.JSONLD.getFilename());
        if (res2 != null) res2.delete();
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.JSONLD.getFilename();
    }
}
