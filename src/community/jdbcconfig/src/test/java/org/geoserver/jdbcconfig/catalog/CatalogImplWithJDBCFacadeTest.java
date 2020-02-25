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

import com.google.common.collect.Lists;
import java.util.List;
import java.util.logging.Level;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;

@RunWith(Parameterized.class)
public class CatalogImplWithJDBCFacadeTest extends org.geoserver.catalog.impl.CatalogImplTest {

    private JDBCCatalogFacade facade;

    private JDBCConfigTestSupport testSupport;

    public CatalogImplWithJDBCFacadeTest(JDBCConfigTestSupport.DBConfig dbConfig) {
        testSupport = new JDBCConfigTestSupport(dbConfig);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return JDBCConfigTestSupport.parameterizedDBConfigs();
    }

    @Override
    public void setUp() throws Exception {
        super.GET_LAYER_BY_ID_WITH_CONCURRENT_ADD_TEST_COUNT = 10;

        testSupport.setUpWithoutAppContext();

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
    public void testAdvertised() {
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        addDataStore();
        addNamespace();

        FeatureTypeInfo ft1 = newFeatureType("ft1", ds);
        ft1.setAdvertised(false);
        catalog.add(ft1);
        StyleInfo s1;
        catalog.add(s1 = newStyle("s1", "s1Filename"));
        LayerInfo l1 = newLayer(ft1, s1);
        catalog.add(l1);
        CloseableIterator<LayerInfo> it =
                catalog.list(
                        LayerInfo.class, ff.equals(ff.property("advertised"), ff.literal(true)));
        assertFalse(it.hasNext());

        ft1 = catalog.getFeatureTypeByName("ft1");
        ft1.setAdvertised(true);
        catalog.save(ft1);

        it = catalog.list(LayerInfo.class, ff.equals(ff.property("advertised"), ff.literal(true)));
        assertTrue(it.hasNext());
        assertEquals(l1.getName(), it.next().getName());
    }

    @Test
    public void testCharacterEncoding() {
        addDataStore();
        addNamespace();
        String name = "testFT";
        FeatureTypeInfo ft = newFeatureType(name, ds);

        String degC = "Air Temperature in \u00b0C";
        ft.setAbstract(degC);
        catalog.add(ft);
        // clear cache to force retrieval from database.
        facade.getConfigDatabase().dispose();
        FeatureTypeInfo added = catalog.getFeatureTypeByName(name);
        assertEquals(degC, added.getAbstract());

        String degF = "Air Temperature in \u00b0F";
        added.setAbstract(degF);
        catalog.save(added);
        facade.getConfigDatabase().dispose();
        FeatureTypeInfo saved = catalog.getFeatureTypeByName(name);
        assertEquals(degF, saved.getAbstract());
    }

    @Test
    public void testEnabled() {
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        addDataStore();
        addNamespace();

        FeatureTypeInfo ft1 = newFeatureType("ft1", ds);
        ft1.setEnabled(false);
        catalog.add(ft1);
        StyleInfo s1;
        catalog.add(s1 = newStyle("s1", "s1Filename"));
        LayerInfo l1 = newLayer(ft1, s1);
        catalog.add(l1);
        CloseableIterator<LayerInfo> it =
                catalog.list(LayerInfo.class, ff.equals(ff.property("enabled"), ff.literal(true)));
        assertFalse(it.hasNext());

        ft1 = catalog.getFeatureTypeByName("ft1");
        ft1.setEnabled(true);
        catalog.save(ft1);

        it = catalog.list(LayerInfo.class, ff.equals(ff.property("enabled"), ff.literal(true)));
        assertTrue(it.hasNext());
        assertEquals(l1.getName(), it.next().getName());
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

        // testOrderBy(LayerInfo.class, filter, null, null, sortOrder, expected);
        CloseableIterator<LayerInfo> it =
                facade.list(
                        LayerInfo.class,
                        filter,
                        null,
                        null,
                        asc("resource.SRS"),
                        asc("defaultStyle.name"),
                        asc("resource.name"));
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

    @Test
    public void testUpgradeLock() {
        GeoServerConfigurationLock configurationLock =
                GeoServerExtensions.bean(GeoServerConfigurationLock.class);
        configurationLock.lock(LockType.READ);
        catalog.getNamespaces();
        assertEquals(LockType.READ, configurationLock.getCurrentLock());
        addNamespace();
        assertEquals(LockType.WRITE, configurationLock.getCurrentLock());
    }

    @Test
    public void testUpdateLinkedStyle() {
        addLayer();
        LayerInfo layer = catalog.getLayerByName("ftName");
        testSupport.getDatabase().clearCache(s);

        StyleInfo style = catalog.getStyle(s.getId());
        style.setName("newStyleName");
        catalog.save(style);

        layer = catalog.getLayerByName("ftName");
        assertEquals(style.getName(), layer.getDefaultStyle().getName());
    }

    /**
     * Allow execution of a single test method with a hard-coded DBConfig. Due to the way
     * junit/maven work with parameterized tests, running a single test was not possible at the time
     * of the change.
     *
     * <p>To do so, use the public constructor of DBConfig, create your test, call setUp and then
     * the test(s) of interest as in the example below.
     */
    public static void main(String[] args) throws Exception {
        Logging.getLogger("").getHandlers()[0].setLevel(Level.ALL);
        Logging.getLogger("org.geoserver.jdbcconfig.internal").setLevel(Level.ALL);

        JDBCConfigTestSupport.DBConfig config =
                new JDBCConfigTestSupport.DBConfig(
                        "oracle",
                        "oracle.jdbc.OracleDriver",
                        "jdbc:oracle:thin:system/oracle@//localhost:49161/xe",
                        "system",
                        "oracle");
        CatalogImplWithJDBCFacadeTest test = new CatalogImplWithJDBCFacadeTest(config);
        test.setUp();
        test.testOrderBy();
    }

    // not supported
    @Test
    public void testAddIsolatedWorkspace() {}

    // not supported
    @Test
    public void testAddIsolatedNamespace() {}
}
