/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TestSetup(run=TestSetupFrequency.REPEAT)
@Category(SystemTest.class)
public class SchemaMappingTest extends GeoServerSystemTestSupport {

    public SchemaMappingTest() {
        super();
    }

    @Test
    public void testNoMapping() throws Exception {
        FeatureTypeInfo ft = 
            getCatalog().getFeatureTypeByName( "DividedRoutes");
        assertEquals( 4, ft.attributes().size() );
    }

    @Test
    public void testXsdMapping() throws Exception {
        getDataDirectory().copyToResourceDir(
            getDividedRoutes(), getClass().getResourceAsStream( "schema.xsd"), "schema.xsd");

        reloadCatalogAndConfiguration();
        FeatureTypeInfo ft = 
            getCatalog().getFeatureTypeByName( "DividedRoutes");
        assertEquals( 3, ft.attributes().size() );
    }
    
    @Test
    public void testXmlMapping() throws Exception {
        getDataDirectory().copyToResourceDir(
                getDividedRoutes(), getClass().getResourceAsStream( "schema.xml"), "schema.xml");

        reloadCatalogAndConfiguration();
        FeatureTypeInfo ft = 
            getCatalog().getFeatureTypeByName( "DividedRoutes");
        assertEquals( 2, ft.attributes().size() );
    }

    FeatureTypeInfo getDividedRoutes() {
        return getCatalog().getFeatureTypeByName( "DividedRoutes");
    }
}
