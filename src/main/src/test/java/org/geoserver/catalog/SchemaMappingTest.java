/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import org.geoserver.platform.resource.Resources;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class SchemaMappingTest extends GeoServerSystemTestSupport {

    public SchemaMappingTest() {
        super();
    }

    @Before
    public void removeMappings() throws IOException {
        File resourceDir = Resources.directory(getDataDirectory().get(getDividedRoutes()));
        new File(resourceDir, "schema.xsd").delete();
        new File(resourceDir, "schema.xml").delete();
    }

    @Test
    public void testNoMapping() throws Exception {
        reloadCatalogAndConfiguration();
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName("DividedRoutes");
        assertEquals(4, ft.attributes().size());
    }

    @Test
    public void testXsdMapping() throws Exception {
        Resources.copy(
                getClass().getResourceAsStream("schema.xsd"),
                getDataDirectory().get(getDividedRoutes()),
                "schema.xsd");

        reloadCatalogAndConfiguration();
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName("DividedRoutes");
        assertEquals(3, ft.attributes().size());
    }

    @Test
    public void testXmlMapping() throws Exception {
        Resources.copy(
                getClass().getResourceAsStream("schema.xml"),
                getDataDirectory().get(getDividedRoutes()),
                "schema.xml");

        reloadCatalogAndConfiguration();
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName("DividedRoutes");
        assertEquals(2, ft.attributes().size());
    }

    FeatureTypeInfo getDividedRoutes() {
        return getCatalog().getFeatureTypeByName("DividedRoutes");
    }
}
