/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.catalog;

import static org.geoserver.catalog.Predicates.acceptAll;
import static org.geoserver.catalog.Predicates.asc;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.junit.After;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

import com.google.common.collect.Lists;

public class CatalogImplWithJDBCFacadeTest extends org.geoserver.catalog.impl.CatalogImplTest {

    private JDBCCatalogFacade facade;

    private JDBCConfigTestSupport testSupport;

    @Override
    public void setUp() throws Exception {
        super.GET_LAYER_BY_ID_WITH_CONCURRENT_ADD_TEST_COUNT = 10;
        testSupport = new JDBCConfigTestSupport();
        testSupport.setUp();

        ConfigDatabase configDb = testSupport.getDatabase();
        facade = new JDBCCatalogFacade(configDb);

        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        facade.dispose();
        testSupport.tearDown();
    }

    @Override
    protected Catalog createCatalog() {
        CatalogImpl catalogImpl = new CatalogImpl();
        catalogImpl.setFacade(facade);
        return catalogImpl;
    }

    @Test
    public void testOrderByMultiple() {
        addDataStore();
        addNamespace();

        FeatureTypeInfo ft1 = newFeatureType("ft1", ds);
        ft1.setSRS("EPSG:1");
        FeatureTypeInfo ft2 = newFeatureType("ft2", ds);
        ft2.setSRS("EPSG:2");
        FeatureTypeInfo ft3 = newFeatureType("ft3", ds);
        ft3.setSRS("EPSG:1");
        FeatureTypeInfo ft4 = newFeatureType("ft4", ds);
        ft4.setSRS("EPSG:2");
        FeatureTypeInfo ft5 = newFeatureType("ft5", ds);
        ft5.setSRS("EPSG:1");
        FeatureTypeInfo ft6 = newFeatureType("ft6", ds);
        ft6.setSRS("EPSG:2");
        FeatureTypeInfo ft7 = newFeatureType("ft7", ds);
        ft7.setSRS("EPSG:1");
        FeatureTypeInfo ft8 = newFeatureType("ft8", ds);
        ft8.setSRS("EPSG:2");

        catalog.add(ft1);
        catalog.add(ft2);
        catalog.add(ft3);
        catalog.add(ft4);
        catalog.add(ft5);
        catalog.add(ft6);
        catalog.add(ft7);
        catalog.add(ft8);

        StyleInfo s1, s2, s3;
        catalog.add(s1 = newStyle("s1", "s1Filename"));
        catalog.add(s2 = newStyle("s2", "s2Filename"));
        catalog.add(s3 = newStyle("s3", "s3Filename"));

        LayerInfo l1 = newLayer(ft8, s1);
        LayerInfo l2 = newLayer(ft7, s2);
        LayerInfo l3 = newLayer(ft6, s3);
        LayerInfo l4 = newLayer(ft5, s1);
        LayerInfo l5 = newLayer(ft4, s2);
        LayerInfo l6 = newLayer(ft3, s3);
        LayerInfo l7 = newLayer(ft2, s1);
        LayerInfo l8 = newLayer(ft1, s2);
        
        catalog.add(l1);
        catalog.add(l2);
        catalog.add(l3);
        catalog.add(l4);
        catalog.add(l5);
        catalog.add(l6);
        catalog.add(l7);
        catalog.add(l8);

        Filter filter;
        SortBy sortOrder;
        List<LayerInfo> expected;

        /*
        Layer   Style   Feature Type    SRS
        l4      s1      ft5     EPSG:1
        l8      s2      ft1     EPSG:1
        l2      s2      ft7     EPSG:1
        l6      s3      ft3     EPSG:1
        l7      s1      ft2     EPSG:2
        l1      s1      ft8     EPSG:2
        l5      s2      ft4     EPSG:2
        l3      s3      ft6     EPSG:2
        */

        filter = acceptAll();
        sortOrder = asc("resource.name");
        expected = Lists.newArrayList(l1, l2, l3);

        //testOrderBy(LayerInfo.class, filter, null, null, sortOrder, expected);
        CloseableIterator<LayerInfo> it = facade.list(LayerInfo.class, filter, null, null, asc("resource.SRS"), asc("defaultStyle.name"), asc("resource.name"));
        try {
            assertThat(it.next(), is(l4));
            assertThat(it.next(), is(l8));
            assertThat(it.next(), is(l2));
            assertThat(it.next(), is(l6));
            assertThat(it.next(), is(l7));
            assertThat(it.next(), is(l1));
            assertThat(it.next(), is(l5));
            assertThat(it.next(), is(l3));
        } finally {
            it.close();
        }
    }

    
//    @Override
//    public void testGetLayerGroupByNameWithWorkspace() {
//        try {
//            super.testGetLayerGroupByNameWithWorkspace();
//        } catch (AssertionFailedError e) {
//            // ignoring failure, we need to fix this as we did for styles by workspace. Check the
//            // comment in the original test case:
//            // "//will randomly return one... we should probably return null with multiple matches"
//            e.printStackTrace();
//        }
//    }
}
