package org.geoserver.test;

import static org.junit.Assert.assertEquals;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.junit.Test;

public class GetFeatureNumberMatchedFeatureChainingTest extends AbstractAppSchemaTestSupport {
    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new FeatureChainingMockData();
    }

    @Test
    public void testGetFeatureNumberMatchedWithAndNestedFilterOnDifferentTypes() throws Exception {

        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter= gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'"
                                + "OR gsml:specification.gsml:GeologicUnit.gsml:composition.gsml:CompositionPart.gsml:proportion.gsml:CGI_TermValue.gsml:value = 'significant'");

        JSONObject resp = (JSONObject) json;
        int numberMatched = resp.getInt("numberMatched");
        int numberReturned = resp.getInt("numberReturned");
        assertEquals(3, numberMatched);
        assertEquals(3, numberReturned);
    }

    @Test
    public void testGetFeatureNumberMatchedWithAndNestedFilterOnSameTypes() throws Exception {

        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'"
                                + "AND gsml:specification.gsml:GeologicUnit.gml:name = 'New Group'");

        JSONObject resp = (JSONObject) json;
        int numberMatched = resp.getInt("numberMatched");
        int numberReturned = resp.getInt("numberReturned");
        assertEquals(1, numberMatched);
        assertEquals(1, numberReturned);
    }
}
