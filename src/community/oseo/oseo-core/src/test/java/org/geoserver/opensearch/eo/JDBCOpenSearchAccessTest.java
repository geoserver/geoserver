/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.geoserver.opensearch.eo.ProductClass.GENERIC;
import static org.geoserver.opensearch.eo.ProductClass.OPTICAL;
import static org.geoserver.opensearch.eo.ProductClass.RADAR;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.EO_NAMESPACE;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeImpl;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;

public class JDBCOpenSearchAccessTest {

    public static final String TEST_NAMESPACE = "http://www.test.com/os/eo";
    private static final Name LAYERS_NAME = OpenSearchAccess.LAYERS_PROPERTY_NAME;

    private static JDBCDataStore store;

    private static OpenSearchAccess osAccess;

    private static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    public static final ProductClass GS_PRODUCT =
            new ProductClass("geoServer", "gs", "http://www.geoserver.org/eo/test");

    protected static Properties getFixture() {
        Properties properties = GSFixtureUtilitiesDelegate.loadFixture("oseo-postgis");
        properties.put("Expose primary keys", "true");
        properties.put("dbtype", "postgis");
        return properties;
    }

    @BeforeClass
    public static void setupStore() throws IOException, SQLException {
        Assume.assumeNotNull(getFixture());

        Map params = new HashMap<>();
        params.putAll(getFixture());
        store = (JDBCDataStore) DataStoreFinder.getDataStore(params);
        JDBCOpenSearchAccessTest.populateTestDatabase(store, true);

        Name name = new NameImpl("test", "jdbcStore");
        SerializableDefaultRepository repository = new SerializableDefaultRepository();
        repository.register(name, store);

        // prepare the custom product class
        GeoServer geoServer = EasyMock.createNiceMock(GeoServer.class);
        GeoServerExtensionsHelper.singleton("geoServer", geoServer, GeoServer.class);
        OSEOInfoImpl impl = new OSEOInfoImpl();
        impl.getProductClasses().add(GS_PRODUCT);
        EasyMock.expect(geoServer.getService(OSEOInfo.class)).andReturn(impl).anyTimes();
        EasyMock.replay(geoServer);

        // create the OpenSeach wrapper store
        params = new HashMap<>();
        params.put("dbtype", "opensearch-eo-jdbc");
        params.put("store", "test:jdbcStore");
        params.put("namespace", TEST_NAMESPACE);
        params.put("repository", repository);
        osAccess = (OpenSearchAccess) DataAccessFinder.getDataStore(params);
    }

    @After
    public void resetCollectionLayer() throws IOException, SQLException {
        String s1 = "DELETE from \"collection_layer\"";
        String s2 =
                "INSERT into \"collection_layer\"\n"
                        + "(\"cid\", \"workspace\", \"layer\", \"separateBands\", \"bands\", \"browseBands\", \"heterogeneousCRS\", \"mosaicCRS\", \"defaultLayer\")\n"
                        + "VALUES(17, 'gs', 'sentinel2', 'true', 'B01,B02,B03,B04,B05,B06,B07,B08,B09,B10,B11,B12', 'B04,B03,B02', 'true', 'EPSG:4326', 'true')";
        String s3 =
                "INSERT into collection_layer\n"
                        + "(\"cid\", \"workspace\", \"layer\", \"separateBands\", \"bands\", \"browseBands\", \"heterogeneousCRS\", \"mosaicCRS\", \"defaultLayer\")\n"
                        + "VALUES(31, 'gs', 'landsat8-SINGLE', 'false', null, null, 'true', 'EPSG:4326', 'true');\n";
        String s4 =
                "INSERT into collection_layer\n"
                        + "(\"cid\", \"workspace\", \"layer\", \"separateBands\", \"bands\", \"browseBands\", \"heterogeneousCRS\", \"mosaicCRS\", \"defaultLayer\")\n"
                        + "VALUES(31, 'gs', 'landsat8-SEPARATE', 'true', 'B01,B02,B03,B04,B05,B06,B07,B08,B09', 'B04,B03,B02', 'true', 'EPSG:4326', 'false');";
        try (Connection conn = store.getConnection(Transaction.AUTO_COMMIT);
                Statement st = conn.createStatement()) {
            st.execute(s1);
            st.execute(s2);
            st.execute(s3);
            st.execute(s4);
        }
    }

    public static void populateTestDatabase(JDBCDataStore h2, boolean addGranuleTable)
            throws SQLException, IOException {
        try (Connection conn = h2.getConnection(Transaction.AUTO_COMMIT);
                Statement st = conn.createStatement()) {
            // setup for fast import

            // SET CACHE_SIZE (a large cache is faster)
            //            st.execute("SET LOG 0");
            //            st.execute("SET LOCK_MODE 0 ");
            //            st.execute("SET UNDO_LOG 0");
            //            st.execute("SET CACHE_SIZE 512000");
            createTables(conn);
            populateCollections(conn);
            populateProducts(conn);
            if (addGranuleTable) {
                populateGranules(conn);
            }
            addCustomProductClass(conn);

            //            // add spatial indexes
            //            st.execute(
            //                    "CALL AddGeometryColumn(SCHEMA(), 'COLLECTION', 'footprint', 4326,
            // 'POLYGON', 2)");
            //            st.execute("CALL CreateSpatialIndex(SCHEMA(), 'COLLECTION', 'footprint',
            // 4326)");
            //            st.execute(
            //                    "CALL AddGeometryColumn(SCHEMA(), 'PRODUCT', 'footprint', 4326,
            // 'POLYGON', 2)");
            //            st.execute("CALL CreateSpatialIndex(SCHEMA(), 'PRODUCT', 'footprint',
            // 4326)");
            //            st.execute(
            //                    "CALL AddGeometryColumn(SCHEMA(), 'GRANULE', 'the_geom', 4326,
            // 'POLYGON', 2)");
            //            st.execute("CALL CreateSpatialIndex(SCHEMA(), 'GRANULE', 'the_geom',
            // 4326)");
        }
    }

    @AfterClass
    public static void tearDownStore() {
        osAccess.dispose();
        store.dispose();
    }

    private static List<String> loadScriptCommands(String scriptLocation) throws IOException {
        // grab all non comment, non empty lines
        try (InputStream is = JDBCOpenSearchAccess.class.getResourceAsStream(scriptLocation)) {
            List<String> lines =
                    IOUtils.readLines(is)
                            .stream()
                            .map(l -> l.trim())
                            .filter(l -> !l.startsWith("--") && !l.isEmpty())
                            .collect(Collectors.toList());
            // regroup them into statements
            List<String> statements = new ArrayList<String>();
            String buffer = null;
            for (String line : lines) {
                if (buffer == null) {
                    buffer = line;
                } else {
                    buffer = buffer + "\n" + line;
                }
                if (line.trim().endsWith(";")) {
                    statements.add(buffer);
                    buffer = null;
                }
            }
            return statements;
        }
    }

    /** Takes the postgis.sql creation script, adapts it and runs it on H2 */
    static void createTables(Connection conn) throws SQLException, IOException {
        List<String> statements = loadScriptCommands("/postgis.sql");
        try (Statement st = conn.createStatement(); ) {
            for (String statement : statements) {
                //                /* Skip statements H2 does not support */
                //                if (statement.contains("GIST") || statement.contains("create
                // extension")) {
                //                    continue;
                //                }
                //                if (statement.contains("geography(Polygon, 4326)")) {
                //                    statement = statement.replace("geography(Polygon, 4326)",
                // "POLYGON");
                //                } else if (statement.contains("geometry(Polygon, 4326)")) {
                //                    statement = statement.replace("geometry(Polygon, 4326)",
                // "POLYGON");
                //                }
                //                if (statement.contains("float[]")) {
                //                    statement = statement.replace("float[]", "ARRAY");
                //                }
                //                if (statement.contains("varchar[]")) {
                //                    statement = statement.replace("varchar[]", "ARRAY");
                //                }
                st.execute(statement);
            }
        }
    }

    /** Adds the collection data into the H2 database */
    static void populateCollections(Connection conn) throws SQLException, IOException {
        runScript("/collection_test_data.sql", conn);
    }

    /** Adds the product data into the H2 database */
    static void populateProducts(Connection conn) throws SQLException, IOException {
        runScript("/product_test_data.sql", conn);
    }

    /** Adds the granules table */
    static void populateGranules(Connection conn) throws SQLException, IOException {
        runScript("/granule_test_data.sql", conn);
    }

    static void addCustomProductClass(Connection conn) throws SQLException, IOException {
        runScript("/custom_product_class.sql", conn);
    }

    static void runScript(String script, Connection conn) throws IOException, SQLException {
        List<String> statements = loadScriptCommands(script);
        try (Statement st = conn.createStatement(); ) {
            for (String statement : statements) {
                try {
                    st.execute(statement);
                } catch (SQLException e) {
                    throw new IOException("Failed to run " + statement, e);
                }
            }
        }
    }

    @Test
    public void testCollectionFeatureType() throws Exception {
        // check expected name
        FeatureType schema = osAccess.getCollectionSource().getSchema();
        Name name = schema.getName();
        assertEquals(TEST_NAMESPACE, name.getNamespaceURI());
        assertThat(name.getLocalPart(), equalToIgnoringCase("collection"));

        // test the schema
        assertPropertyNamespace(schema, "wavelength", EO_NAMESPACE);
    }

    @Test
    public void testProductFeatureType() throws Exception {
        // check expected name
        FeatureType schema = osAccess.getProductSource().getSchema();
        Name name = schema.getName();

        assertEquals(TEST_NAMESPACE, name.getNamespaceURI());
        assertThat(name.getLocalPart(), equalToIgnoringCase("product"));

        // get the schema
        assertPropertyNamespace(schema, "cloudCover", OPTICAL.getNamespace());
        assertPropertyNamespace(schema, "track", GENERIC.getNamespace());
        assertPropertyNamespace(schema, "polarisationMode", RADAR.getNamespace());
        assertPropertyNamespace(schema, "test", GS_PRODUCT.getNamespace());
    }

    @Test
    public void testTypeNames() throws Exception {
        List<Name> names = osAccess.getNames();
        // product, collection, SENTINEL1, SENTINEL2, LANDSAT8, ATM1,
        assertThat(names, hasSize(27));
        Set<String> localNames = new HashSet<>();
        for (Name name : names) {
            assertEquals(TEST_NAMESPACE, name.getNamespaceURI());
            localNames.add(name.getLocalPart());
        }
        assertThat(
                localNames,
                containsInAnyOrder(
                        "collection",
                        "product",
                        "SENTINEL1",
                        "LANDSAT8",
                        "GS_TEST",
                        "ATMTEST",
                        "SENTINEL2__B01",
                        "SENTINEL2__B02",
                        "SENTINEL2__B03",
                        "SENTINEL2__B04",
                        "SENTINEL2__B05",
                        "SENTINEL2__B06",
                        "SENTINEL2__B07",
                        "SENTINEL2__B08",
                        "SENTINEL2__B09",
                        "SENTINEL2__B10",
                        "SENTINEL2__B11",
                        "SENTINEL2__B12",
                        "LANDSAT8__B01",
                        "LANDSAT8__B02",
                        "LANDSAT8__B03",
                        "LANDSAT8__B04",
                        "LANDSAT8__B05",
                        "LANDSAT8__B06",
                        "LANDSAT8__B07",
                        "LANDSAT8__B08",
                        "LANDSAT8__B09"));
    }

    @Test
    public void testSentinel1Schema() throws Exception {
        FeatureType schema = osAccess.getSchema(new NameImpl(TEST_NAMESPACE, "SENTINEL1"));
        assertGranulesViewSchema(schema, RADAR);
    }

    @Test
    public void testSentinel2Schema() throws Exception {
        FeatureType schema = osAccess.getSchema(new NameImpl(TEST_NAMESPACE, "SENTINEL2__B01"));
        assertGranulesViewSchema(schema, OPTICAL);
    }

    @Test
    public void testLandsat8Schema() throws Exception {
        FeatureType schema = osAccess.getSchema(new NameImpl(TEST_NAMESPACE, "LANDSAT8"));
        assertGranulesViewSchema(schema, OPTICAL);
    }

    @Test
    public void testCustomClassSchema() throws Exception {
        FeatureType schema = osAccess.getSchema(new NameImpl(TEST_NAMESPACE, "GS_TEST"));
        assertGranulesViewSchema(schema, GS_PRODUCT);
    }

    @Test
    public void testSentinel1Granules() throws Exception {
        FeatureSource<FeatureType, Feature> featureSource =
                osAccess.getFeatureSource(new NameImpl(TEST_NAMESPACE, "SENTINEL1"));
        assertEquals(0, featureSource.getCount(Query.ALL));
        FeatureCollection<FeatureType, Feature> fc = featureSource.getFeatures();
        assertEquals(0, fc.size());
        fc.accepts(
                f -> {},
                null); // just check trying to scroll over the feature does not make it blow
    }

    @Test
    public void testSentinel2Granules() throws Exception {
        FeatureSource<FeatureType, Feature> featureSource =
                osAccess.getFeatureSource(new NameImpl(TEST_NAMESPACE, "SENTINEL2__B01"));
        FeatureCollection<FeatureType, Feature> fc = featureSource.getFeatures();
        assertGranulesViewSchema(fc.getSchema(), OPTICAL);
        assertThat(fc.size(), greaterThan(1));
        fc.accepts(
                f -> {
                    // check the primary key has been mapped
                    assertThat(f, instanceOf(SimpleFeature.class));
                    SimpleFeature sf = (SimpleFeature) f;
                    final String id = sf.getID();
                    assertTrue(id.matches("\\w+\\.\\d+"));
                },
                null);
    }

    @Test
    public void testCustomProductClassGranules() throws Exception {
        // System.out.println(osAccess.getNames());
        FeatureSource<FeatureType, Feature> featureSource =
                osAccess.getFeatureSource(new NameImpl(TEST_NAMESPACE, "GS_TEST"));
        assertEquals(0, featureSource.getCount(Query.ALL));
        FeatureCollection<FeatureType, Feature> fc = featureSource.getFeatures();
        assertGranulesViewSchema(fc.getSchema(), GS_PRODUCT);
        assertEquals(0, fc.size());
        fc.accepts(
                f -> {},
                null); // just check trying to scroll over the feature does not make it blow
    }

    private void assertGranulesViewSchema(FeatureType schema, ProductClass expectedClass)
            throws IOException {
        assertThat(schema, instanceOf(SimpleFeatureType.class));
        SimpleFeatureType ft = (SimpleFeatureType) schema;
        // check there are no foreign attributes
        Map<String, Class> mappings = new HashMap<>();
        for (AttributeDescriptor ad : ft.getAttributeDescriptors()) {
            final String adName = ad.getLocalName();
            for (ProductClass pc : ProductClass.DEFAULT_PRODUCT_CLASSES) {
                if (pc == GENERIC || pc == expectedClass) {
                    continue;
                } else {
                    assertThat(adName, not(startsWith(pc.getPrefix())));
                }
                mappings.put(adName, ad.getType().getBinding());
            }
        }
        // check the granule attributes are alive and well, but the product_id is not visible
        assertThat(mappings.keySet(), not(hasItem("product_id")));
        assertThat(mappings.keySet(), hasItem(equalToIgnoringCase("location")));
        assertThat(mappings.keySet(), hasItem(equalToIgnoringCase("the_geom")));
        // check the class specific attributes are there
        assertThat(mappings.keySet(), hasItem(startsWith(expectedClass.getPrefix())));
        // check the generic EOPs are there too
        assertThat(mappings.keySet(), hasItem(startsWith("eo")));
        // check timestart/timeend
        assertThat(mappings.keySet(), hasItem("timeStart"));
        assertThat(mappings.keySet(), hasItem("timeEnd"));
        // verify the geometry is properly mapped
        assertThat(mappings, hasEntry(equalToIgnoringCase("THE_GEOM"), equalTo(Polygon.class)));
        // check that we have the extra properties for hetero mosaics
        assertThat(mappings, hasEntry(equalTo("crs"), equalTo(String.class)));
    }

    private void assertPropertyNamespace(FeatureType schema, String name, String namespaceURI) {
        PropertyDescriptor wl = schema.getDescriptor(name);
        assertNotNull(wl);
        assertEquals(namespaceURI, wl.getName().getNamespaceURI());
    }

    @Test
    public void testCollectionLayerInformation() throws Exception {
        // check expected property is there
        FeatureType schema = osAccess.getCollectionSource().getSchema();
        Name name = schema.getName();
        assertEquals(TEST_NAMESPACE, name.getNamespaceURI());
        final PropertyDescriptor layerDescriptor = schema.getDescriptor(LAYERS_NAME);
        assertNotNull(layerDescriptor);

        // read it
        FeatureSource<FeatureType, Feature> source = osAccess.getCollectionSource();
        Query q = new Query();
        q.setProperties(Arrays.asList(FF.property(LAYERS_NAME)));
        q.setFilter(
                FF.equal(
                        FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                        FF.literal("SENTINEL2"),
                        false));
        FeatureCollection<FeatureType, Feature> features = source.getFeatures(q);

        // get the collection and check it
        Feature collection = DataUtilities.first(features);
        assertNotNull(collection);
        Property layerProperty = collection.getProperty(LAYERS_NAME);
        final Feature layerValue = (Feature) layerProperty;
        assertThat(layerValue, notNullValue());

        assertEquals("gs", getAttribute(layerValue, "workspace"));
        assertEquals("sentinel2", getAttribute(layerValue, "layer"));
        assertEquals(Boolean.TRUE, getAttribute(layerValue, "separateBands"));
        assertThat(
                getAttribute(layerValue, "bands"),
                equalTo(
                        new String[] {
                            "B01", "B02", "B03", "B04", "B05", "B06", "B07", "B08", "B09", "B10",
                            "B11", "B12"
                        }));
        assertThat(
                getAttribute(layerValue, "browseBands"),
                equalTo(new String[] {"B04", "B03", "B02"}));
        assertEquals(Boolean.TRUE, getAttribute(layerValue, "heterogeneousCRS"));
        assertEquals("EPSG:4326", getAttribute(layerValue, "mosaicCRS"));
    }

    @Test
    public void testTwoCollectionLayers() throws Exception {
        // read it
        FeatureStore<FeatureType, Feature> store =
                (FeatureStore<FeatureType, Feature>) osAccess.getCollectionSource();
        Query q = new Query();
        q.setProperties(Arrays.asList(FF.property(LAYERS_NAME)));
        final PropertyIsEqualTo filter =
                FF.equal(
                        FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                        FF.literal("LANDSAT8"),
                        false);
        q.setFilter(filter);
        FeatureCollection<FeatureType, Feature> features = store.getFeatures(q);

        Map<String, SimpleFeature> layerFeatures = getLayerPropertiesFromCollection(features);
        assertThat(
                layerFeatures.keySet(), Matchers.hasItems("landsat8-SINGLE", "landsat8-SEPARATE"));

        // first layer
        SimpleFeature single = layerFeatures.get("landsat8-SINGLE");
        assertEquals("gs", getAttribute(single, "workspace"));
        assertEquals("landsat8-SINGLE", getAttribute(single, "layer"));
        assertEquals(Boolean.FALSE, getAttribute(single, "separateBands"));
        assertNull(getAttribute(single, "bands"));
        assertNull(getAttribute(single, "browseBands"));
        assertEquals(Boolean.TRUE, getAttribute(single, "heterogeneousCRS"));
        assertEquals("EPSG:4326", getAttribute(single, "mosaicCRS"));

        // second layer
        SimpleFeature separate = layerFeatures.get("landsat8-SEPARATE");
        assertEquals("gs", getAttribute(separate, "workspace"));
        assertEquals("landsat8-SEPARATE", getAttribute(separate, "layer"));
        assertEquals(Boolean.TRUE, getAttribute(separate, "separateBands"));
        assertThat(
                getAttribute(separate, "bands"),
                equalTo(
                        new String[] {
                            "B01", "B02", "B03", "B04", "B05", "B06", "B07", "B08", "B09"
                        }));
        assertThat(
                getAttribute(separate, "browseBands"), equalTo(new String[] {"B04", "B03", "B02"}));
        assertEquals(Boolean.TRUE, getAttribute(separate, "heterogeneousCRS"));
        assertEquals("EPSG:4326", getAttribute(separate, "mosaicCRS"));
    }

    @Test
    public void testCollectionLayerUpdate() throws Exception {
        // read it
        FeatureStore<FeatureType, Feature> store =
                (FeatureStore<FeatureType, Feature>) osAccess.getCollectionSource();
        Query q = new Query();
        q.setProperties(Arrays.asList(FF.property(LAYERS_NAME)));
        final PropertyIsEqualTo filter =
                FF.equal(
                        FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                        FF.literal("SENTINEL2"),
                        false);
        q.setFilter(filter);
        FeatureCollection<FeatureType, Feature> features = store.getFeatures(q);

        final SimpleFeature layerValue = getLayerPropertyFromCollection(features);

        // modify it
        setAttribute(layerValue, "workspace", "gs2");
        setAttribute(layerValue, "layer", "sentinel12345");
        setAttribute(layerValue, "separateBands", false);
        setAttribute(layerValue, "bands", new String[] {"B01", "B04", "B06"});
        setAttribute(layerValue, "browseBands", null);
        setAttribute(layerValue, "heterogeneousCRS", false);
        setAttribute(layerValue, "mosaicCRS", "EPSG:3857");
        ListFeatureCollection layers =
                new ListFeatureCollection(osAccess.getCollectionLayerSchema());
        layers.add(layerValue);

        // update the feature
        store.modifyFeatures(new Name[] {LAYERS_NAME}, new Object[] {layers}, filter);

        // read it back and check
        final Feature layerValue2 = getLayerPropertyFromCollection(store.getFeatures(q));
        assertEquals("gs2", getAttribute(layerValue2, "workspace"));
        assertEquals("sentinel12345", getAttribute(layerValue2, "layer"));
        assertEquals(Boolean.FALSE, getAttribute(layerValue2, "separateBands"));
        assertArrayEquals(
                new String[] {"B01", "B04", "B06"}, (String[]) getAttribute(layerValue2, "bands"));
        assertThat(getAttribute(layerValue2, "browseBands"), nullValue());
        assertEquals(Boolean.FALSE, getAttribute(layerValue2, "heterogeneousCRS"));
        assertEquals("EPSG:3857", getAttribute(layerValue2, "mosaicCRS"));
    }

    @Test
    public void testCollectionLayerUpdateMulti() throws Exception {
        // read it
        FeatureStore<FeatureType, Feature> store =
                (FeatureStore<FeatureType, Feature>) osAccess.getCollectionSource();
        Query q = new Query();
        q.setProperties(Arrays.asList(FF.property(LAYERS_NAME)));
        final PropertyIsEqualTo filter =
                FF.equal(
                        FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                        FF.literal("LANDSAT8"),
                        false);
        q.setFilter(filter);
        FeatureCollection<FeatureType, Feature> features = store.getFeatures(q);

        Map<String, SimpleFeature> layerFeatures = getLayerPropertiesFromCollection(features);
        assertThat(
                layerFeatures.keySet(), Matchers.hasItems("landsat8-SINGLE", "landsat8-SEPARATE"));
        SimpleFeature layerSingle = layerFeatures.get("landsat8-SINGLE");

        // modify the single layer one
        setAttribute(layerSingle, "workspace", "gs2");
        setAttribute(layerSingle, "layer", "landsat-foobar");
        setAttribute(layerSingle, "separateBands", false);
        setAttribute(layerSingle, "bands", new String[] {"B01", "B04", "B06"});
        setAttribute(layerSingle, "browseBands", null);
        setAttribute(layerSingle, "heterogeneousCRS", false);
        setAttribute(layerSingle, "mosaicCRS", "EPSG:3857");

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(osAccess.getCollectionLayerSchema());
        fb.set("workspace", "gs2");
        fb.set("layer", "landsat-third");
        fb.set("separateBands", false);
        fb.set("bands", null);
        fb.set("browseBands", null);
        fb.set("heterogeneousCRS", true);
        fb.set("mosaicCRS", "EPSG:32632");
        SimpleFeature newLayer = fb.buildFeature(null);

        // new list of layers, some will be gone
        ListFeatureCollection layers =
                new ListFeatureCollection(osAccess.getCollectionLayerSchema());
        layers.add(layerSingle);
        layers.add(newLayer);

        // update the feature
        store.modifyFeatures(new Name[] {LAYERS_NAME}, new Object[] {layers}, filter);

        // read it back and check
        layerFeatures = getLayerPropertiesFromCollection(features);
        assertThat(layerFeatures.keySet(), Matchers.hasItems("landsat-foobar", "landsat-third"));
        final Feature layerFooBar = layerFeatures.get("landsat-foobar");
        assertEquals("gs2", getAttribute(layerFooBar, "workspace"));
        assertEquals("landsat-foobar", getAttribute(layerFooBar, "layer"));
        assertEquals(Boolean.FALSE, getAttribute(layerFooBar, "separateBands"));
        assertArrayEquals(
                new String[] {"B01", "B04", "B06"}, (String[]) getAttribute(layerFooBar, "bands"));
        assertThat(getAttribute(layerFooBar, "browseBands"), nullValue());
        assertEquals(Boolean.FALSE, getAttribute(layerFooBar, "heterogeneousCRS"));
        assertEquals("EPSG:3857", getAttribute(layerFooBar, "mosaicCRS"));

        final Feature layerThird = layerFeatures.get("landsat-third");
        assertEquals("gs2", getAttribute(layerThird, "workspace"));
        assertEquals("landsat-third", getAttribute(layerThird, "layer"));
        assertEquals(Boolean.FALSE, getAttribute(layerThird, "separateBands"));
        assertThat(getAttribute(layerThird, "bands"), nullValue());
        assertThat(getAttribute(layerThird, "browseBands"), nullValue());
        assertEquals(Boolean.TRUE, getAttribute(layerThird, "heterogeneousCRS"));
        assertEquals("EPSG:32632", getAttribute(layerThird, "mosaicCRS"));
    }

    private Object getAttribute(Feature sf, String name) {
        Property p = sf.getProperty(name);
        if (p != null) {
            return p.getValue();
        } else {
            return null;
        }
    }

    private void setAttribute(Feature sf, String name, Object value) {
        Property p = sf.getProperty(name);
        if (p != null) {
            p.setValue(value);
        } else {
            List<Property> properties = new ArrayList<>(sf.getValue());
            AttributeDescriptor ad = (AttributeDescriptor) sf.getType().getDescriptor(name);
            properties.add(new AttributeImpl(value, ad, null));
        }
    }

    private SimpleFeature getLayerPropertyFromCollection(
            FeatureCollection<FeatureType, Feature> features) {
        Map<String, SimpleFeature> layerProperty = getLayerPropertiesFromCollection(features);
        return layerProperty.values().iterator().next();
    }

    private Map<String, SimpleFeature> getLayerPropertiesFromCollection(
            FeatureCollection<FeatureType, Feature> features) {
        // get the simple feature representing the layer publishing info
        Feature collection = DataUtilities.first(features);
        assertNotNull(collection);
        Collection<Property> layerProperty = collection.getProperties(LAYERS_NAME);
        assertThat(layerProperty, notNullValue());
        assertThat(layerProperty, not(empty()));

        return layerProperty
                .stream()
                .map(p -> (SimpleFeature) p)
                .collect(
                        Collectors.toMap(
                                sf -> (String) sf.getAttribute("layer"),
                                sf -> sf,
                                (u, v) -> u,
                                LinkedHashMap::new));
    }

    @Test
    public void testCollectionLayerRemoval() throws Exception {
        // read it
        FeatureStore<FeatureType, Feature> store =
                (FeatureStore<FeatureType, Feature>) osAccess.getCollectionSource();
        Query q = new Query();
        q.setProperties(Arrays.asList(FF.property(LAYERS_NAME)));
        final PropertyIsEqualTo filter =
                FF.equal(
                        FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                        FF.literal("SENTINEL2"),
                        false);
        q.setFilter(filter);

        // update the feature to remove the layer information
        store.modifyFeatures(
                new Name[] {OpenSearchAccess.LAYERS_PROPERTY_NAME}, new Object[] {null}, filter);

        // read it back and check it's not set
        Feature collection = DataUtilities.first(store.getFeatures(q));
        assertNotNull(collection);
        Property layerProperty = collection.getProperty(LAYERS_NAME);
        assertNull(layerProperty);
    }
}
