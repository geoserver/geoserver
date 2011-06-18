package org.geoserver.catalog;

import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;

public class SchemaMappingTest extends GeoServerTestSupport {

    public void testNoMapping() throws Exception {
        FeatureTypeInfo ft = 
            getCatalog().getFeatureTypeByName( "DividedRoutes");
        assertEquals( 4, ft.attributes().size() );
    }
    
    public void testXsdMapping() throws Exception {
        ((MockData)testData).copyToFeatureTypeDirectory( getClass().getResourceAsStream( "schema.xsd"), 
                MockData.DIVIDED_ROUTES, "schema.xsd");
        reloadCatalogAndConfiguration();
        FeatureTypeInfo ft = 
            getCatalog().getFeatureTypeByName( "DividedRoutes");
        assertEquals( 3, ft.attributes().size() );
    }
    
    public void testXmlMapping() throws Exception {
        ((MockData)testData).copyToFeatureTypeDirectory( getClass().getResourceAsStream( "schema.xml"), 
                MockData.DIVIDED_ROUTES, "schema.xml");
        reloadCatalogAndConfiguration();
        FeatureTypeInfo ft = 
            getCatalog().getFeatureTypeByName( "DividedRoutes");
        assertEquals( 2, ft.attributes().size() );
    }
    
}
