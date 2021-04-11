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

    @Test
    public void testGetFeatureNumberMatchedWithComplexPropertyORSimpleProperty() throws Exception {

        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gsml:composition.gsml:CompositionPart.gsml:proportion.gsml:CGI_TermValue.gsml:value = 'significant'"
                                + " OR gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        JSONObject resp = (JSONObject) json;
        int numberMatched = resp.getInt("numberMatched");
        int numberReturned = resp.getInt("numberReturned");
        assertEquals(3, numberMatched);
        assertEquals(3, numberReturned);
    }

    @Test
    public void testGetFeatureNumberMatchedWithSimplePropertyANDComplexProperty() throws Exception {

        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gsml:composition.gsml:CompositionPart.gsml:proportion.gsml:CGI_TermValue.gsml:value = 'significant'"
                                + " AND gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        JSONObject resp = (JSONObject) json;
        int numberMatched = resp.getInt("numberMatched");
        int numberReturned = resp.getInt("numberReturned");
        assertEquals(1, numberMatched);
        assertEquals(1, numberReturned);
    }

    @Test
    public void testGetFeatureNumberMatchedWithComplexPropertyORSimplePropertyWithPagination()
            throws Exception {

        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gsml:composition.gsml:CompositionPart.gsml:proportion.gsml:CGI_TermValue.gsml:value = 'significant'"
                                + " OR gsml:MappedFeature.gml:name = 'MURRADUC BASALT'&startIndex=2");

        JSONObject resp = (JSONObject) json;
        int numberMatched = resp.getInt("numberMatched");
        int numberReturned = resp.getInt("numberReturned");
        assertEquals(3, numberMatched);
        assertEquals(1, numberReturned);
    }

    @Test
    public void testGetFeatureNumberMatchedWithMultipleAND() throws Exception {
        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gsml:composition.gsml:CompositionPart.gsml:proportion.gsml:CGI_TermValue.gsml:value = 'significant'"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%25%27 AND gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        JSONObject resp = (JSONObject) json;
        int numberMatched = resp.getInt("numberMatched");
        int numberReturned = resp.getInt("numberReturned");
        assertEquals(1, numberMatched);
        assertEquals(1, numberReturned);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilter() throws Exception {
        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27");

        JSONObject resp = (JSONObject) json;
        int numberMatched = resp.getInt("numberMatched");
        int numberReturned = resp.getInt("numberReturned");
        assertEquals(3, numberMatched);
        assertEquals(3, numberReturned);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilterWithPagination() throws Exception {
        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27&startIndex=1");

        JSONObject resp = (JSONObject) json;
        int numberMatched = resp.getInt("numberMatched");
        int numberReturned = resp.getInt("numberReturned");
        assertEquals(3, numberMatched);
        assertEquals(2, numberReturned);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilterManyAND() throws Exception {
        JSON json =
                getAsJSON(
                        "ows?service=WFS&outputFormat=application/json&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27 AND gsml:MappedFeature.gml:name = 'GUNTHORPE FORMATION'");
        JSONObject resp = (JSONObject) json;
        int numberMatched = resp.getInt("numberMatched");
        int numberReturned = resp.getInt("numberReturned");
        assertEquals(1, numberMatched);
        assertEquals(1, numberReturned);
    }
}
