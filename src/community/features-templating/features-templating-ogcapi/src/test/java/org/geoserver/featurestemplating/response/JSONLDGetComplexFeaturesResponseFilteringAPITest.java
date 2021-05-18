/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JSONLDGetComplexFeaturesResponseFilteringAPITest extends TemplateComplexTestSupport {

    @Test
    public void testJsonLdOgcFilterConcatenation() throws Exception {
        // test templates filter concatenation with backward mapping
        // "$filter": "xpath('gml:description') = 'Olivine basalt'",
        setUpComplex("MappedFeatureIteratingAndCompositeFilter.json", mappedFeature);

        String cqlFilter =
                "features.gsml:GeologicUnit.gsml:composition.gsml:compositionPart.gsml:role.value = ";

        // if querying for fictitious component expecting no result because the And condition
        // with the template filter
        JSONArray features = getResultFilterConcatenated(cqlFilter, "'fictitious component'");

        assertEquals(0, features.size());

        // if querying for interbedded component expecting 1 feature as result;
        // this time the result of template filter has this gms:role value.
        JSONArray features2 = getResultFilterConcatenated(cqlFilter, "'interbedded component'");
        assertEquals(1, features2.size());
    }

    private JSONArray getResultFilterConcatenated(String cql_filter, String equalsTo)
            throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&filter-lang=cql-text")
                        .append("&filter= ")
                        .append(cql_filter)
                        .append(equalsTo);
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        return result.getJSONArray("features");
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.JSONLD.getFilename();
    }
}
