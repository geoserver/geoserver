package org.geoserver.test;

import static org.junit.Assert.assertEquals;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.junit.Test;

public class GetFeatureNumberMatchedGeoJSONTest extends AbstractAppSchemaTestSupport {
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

        assertNumberMatchedAndNumberReturned(json, 3, 3);
    }

    @Test
    public void testGetFeatureNumberMatchedWithAndNestedFilterOnSameTypes() throws Exception {

        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'"
                                + "AND gsml:specification.gsml:GeologicUnit.gml:name = 'New Group'");

        assertNumberMatchedAndNumberReturned(json, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithComplexPropertyORSimpleProperty() throws Exception {

        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gsml:composition.gsml:CompositionPart.gsml:proportion.gsml:CGI_TermValue.gsml:value = 'significant'"
                                + " OR gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        assertNumberMatchedAndNumberReturned(json, 3, 3);
    }

    @Test
    public void testGetFeatureNumberMatchedWithSimplePropertyANDComplexProperty() throws Exception {

        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gsml:composition.gsml:CompositionPart.gsml:proportion.gsml:CGI_TermValue.gsml:value = 'significant'"
                                + " AND gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        assertNumberMatchedAndNumberReturned(json, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithComplexPropertyORSimplePropertyWithPagination()
            throws Exception {

        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gsml:composition.gsml:CompositionPart.gsml:proportion.gsml:CGI_TermValue.gsml:value = 'significant'"
                                + " OR gsml:MappedFeature.gml:name = 'MURRADUC BASALT'&startIndex=2");

        assertNumberMatchedAndNumberReturned(json, 3, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithMultipleAND() throws Exception {
        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gsml:composition.gsml:CompositionPart.gsml:proportion.gsml:CGI_TermValue.gsml:value = 'significant'"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%25%27 AND gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        assertNumberMatchedAndNumberReturned(json, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilter() throws Exception {
        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27");

        assertNumberMatchedAndNumberReturned(json, 3, 3);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilterWithPagination() throws Exception {
        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27&startIndex=1");

        assertNumberMatchedAndNumberReturned(json, 3, 2);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilterManyAND() throws Exception {
        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27 AND gsml:MappedFeature.gml:name = 'GUNTHORPE FORMATION'");
        assertNumberMatchedAndNumberReturned(json, 1, 1);
    }

    private void assertNumberMatchedAndNumberReturned(
            JSON resp, int numberMatched, int numberReturned) {
        JSONObject jsonObject = (JSONObject) resp;
        int numberMatchedValue = jsonObject.getInt("numberMatched");
        int numberReturnedValue = jsonObject.getInt("numberReturned");
        assertEquals(numberMatched, numberMatchedValue);
        assertEquals(numberReturned, numberReturnedValue);
    }
}
