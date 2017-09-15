/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.geoserver.opensearch.eo.store.OpenSearchAccess.EO_NAMESPACE;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.ProductClass.EOP_GENERIC;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.ProductClass.OPTICAL;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.ProductClass.RADAR;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.opensearch.eo.store.OpenSearchAccess.ProductClass;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeImpl;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.PropertyImpl;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
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

import com.vividsolutions.jts.geom.Polygon;

public class JDBCOpenSearchAccessTest {

    public static final String TEST_NAMESPACE = "http://www.test.com/os/eo";

    private static JDBCDataStore h2;

    private static OpenSearchAccess osAccess;

    private static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    static final Name LAYER_NAME = new NameImpl(TEST_NAMESPACE, OpenSearchAccess.LAYER);

    @BeforeClass
    public static void setupStore() throws IOException, SQLException {
        Map<String, Serializable> params = new HashMap<>();
        params.put("dbtype", "h2");
        File dbFolder = new File("./target/oseo_db_store_test");
        FileUtils.deleteQuietly(dbFolder);
        dbFolder.mkdir();
        File dbFile = new File(dbFolder, "oseo_db_store_test");
        params.put("database", dbFile.getAbsolutePath());
        params.put(JDBCDataStoreFactory.EXPOSE_PK.key, "true");
        h2 = (JDBCDataStore) DataStoreFinder.getDataStore(params);
        JDBCOpenSearchAccessTest.populateTestDatabase(h2, true);

        Name name = new NameImpl("test", "jdbcStore");
        SerializableDefaultRepository repository = new SerializableDefaultRepository();
        repository.register(name, h2);

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
        String s1 = "DELETE public.collection_layer";
        String s2 = "INSERT into public.collection_layer\n"
                + "(\"cid\", \"workspace\", \"layer\", \"separateBands\", \"bands\", \"browseBands\", \"heterogeneousCRS\", \"mosaicCRS\")\n"
                + "VALUES(17, 'gs', 'sentinel2', true, 'B01,B02,B03,B04,B05,B06,B07,B08,B09,B10,B11,B12', 'B04,B03,B02', true, 'EPSG:4326')";
        try (Connection conn = h2.getConnection(Transaction.AUTO_COMMIT);
                Statement st = conn.createStatement()) {
            st.execute(s1);
            st.execute(s2);
        }
    }

    public static void populateTestDatabase(JDBCDataStore h2, boolean addGranuleTable)
            throws SQLException, IOException {
        try (Connection conn = h2.getConnection(Transaction.AUTO_COMMIT);
                Statement st = conn.createStatement()) {
            // setup for fast import

            // SET CACHE_SIZE (a large cache is faster)
            st.execute("SET LOG 0");
            st.execute("SET LOCK_MODE 0 ");
            st.execute("SET UNDO_LOG 0");
            st.execute("SET CACHE_SIZE 512000");
            createTables(conn);
            populateCollections(conn);
            populateProducts(conn);
            if (addGranuleTable) {
                populateGranules(conn);
            }
        }
    }

    @AfterClass
    public static void tearDownStore() {
        osAccess.dispose();
        h2.dispose();
    }

    private static List<String> loadScriptCommands(String scriptLocation) throws IOException {
        // grab all non comment, non empty lines
        try (InputStream is = JDBCOpenSearchAccess.class.getResourceAsStream(scriptLocation)) {
            List<String> lines = IOUtils.readLines(is).stream().map(l -> l.trim())
                    .filter(l -> !l.startsWith("--") && !l.isEmpty()).collect(Collectors.toList());
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

    /**
     * Takes the postgis.sql creation script, adapts it and runs it on H2
     */
    static void createTables(Connection conn) throws SQLException, IOException {
        List<String> statements = loadScriptCommands("/postgis.sql");
        try (Statement st = conn.createStatement();) {
            for (String statement : statements) {
                /* Skip statements H2 does not support */
                if (statement.contains("GIST") || statement.contains("create extension")) {
                    continue;
                }
                if (statement.contains("geography(Polygon, 4326)")) {
                    statement = statement.replace("geography(Polygon, 4326)", "POLYGON");
                } else if (statement.contains("geometry(Polygon, 4326)")) {
                    statement = statement.replace("geometry(Polygon, 4326)", "POLYGON");
                }
                st.execute(statement);
            }
            // add spatial indexes
            st.execute(
                    "CALL AddGeometryColumn(SCHEMA(), 'COLLECTION', 'footprint', 4326, 'POLYGON', 2)");
            st.execute("CALL CreateSpatialIndex(SCHEMA(), 'COLLECTION', 'footprint', 4326)");
            st.execute(
                    "CALL AddGeometryColumn(SCHEMA(), 'PRODUCT', 'footprint', 4326, 'POLYGON', 2)");
            st.execute("CALL CreateSpatialIndex(SCHEMA(), 'PRODUCT', 'footprint', 4326)");
            st.execute(
                    "CALL AddGeometryColumn(SCHEMA(), 'GRANULE', 'the_geom', 4326, 'POLYGON', 2)");
            st.execute("CALL CreateSpatialIndex(SCHEMA(), 'GRANULE', 'the_geom', 4326)");
        }
    }

    /**
     * Adds the collection data into the H2 database
     */
    static void populateCollections(Connection conn) throws SQLException, IOException {
        runScript("/collection_h2_data.sql", conn);
    }

    /**
     * Adds the product data into the H2 database
     */
    static void populateProducts(Connection conn) throws SQLException, IOException {
        runScript("/product_h2_data.sql", conn);
    }

    /**
     * Adds the granules table
     * 
     * @param conn
     * @throws SQLException
     * @throws IOException
     */
    static void populateGranules(Connection conn) throws SQLException, IOException {
        runScript("/granule_h2_data.sql", conn);
    }

    static void runScript(String script, Connection conn) throws IOException, SQLException {
        List<String> statements = loadScriptCommands(script);
        try (Statement st = conn.createStatement();) {
            for (String statement : statements) {
                st.execute(statement);
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
        assertPropertyNamespace(schema, "track", EOP_GENERIC.getNamespace());
        assertPropertyNamespace(schema, "polarisationMode", RADAR.getNamespace());
    }

    @Test
    public void testTypeNames() throws Exception {
        List<Name> names = osAccess.getNames();
        // product, collection, SENTINEL1, SENTINEL2, LANDSAT8
        assertEquals(16, names.size());
        Set<String> localNames = new HashSet<>();
        for (Name name : names) {
            assertEquals(TEST_NAMESPACE, name.getNamespaceURI());
            localNames.add(name.getLocalPart());
        }
        assertThat(localNames,
                containsInAnyOrder("collection", "product", "SENTINEL1", "LANDSAT8",
                        "SENTINEL2__B01", "SENTINEL2__B02", "SENTINEL2__B03", "SENTINEL2__B04",
                        "SENTINEL2__B05", "SENTINEL2__B06", "SENTINEL2__B07", "SENTINEL2__B08",
                        "SENTINEL2__B09", "SENTINEL2__B10", "SENTINEL2__B11", "SENTINEL2__B12"));
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
    public void testSentinel1Granules() throws Exception {
        FeatureSource<FeatureType, Feature> featureSource = osAccess
                .getFeatureSource(new NameImpl(TEST_NAMESPACE, "SENTINEL1"));
        assertEquals(0, featureSource.getCount(Query.ALL));
        FeatureCollection<FeatureType, Feature> fc = featureSource.getFeatures();
        assertEquals(0, fc.size());
        fc.accepts(f -> {
        }, null); // just check trying to scroll over the feature does not make it blow
    }

    @Test
    public void testSentinel2Granules() throws Exception {
        FeatureSource<FeatureType, Feature> featureSource = osAccess
                .getFeatureSource(new NameImpl(TEST_NAMESPACE, "SENTINEL2__B01"));
        FeatureCollection<FeatureType, Feature> fc = featureSource.getFeatures();
        assertGranulesViewSchema(fc.getSchema(), OPTICAL);
        assertThat(fc.size(), greaterThan(1));
        fc.accepts(f -> {
            // check the primary key has been mapped
            assertThat(f, instanceOf(SimpleFeature.class));
            SimpleFeature sf = (SimpleFeature) f;
            final String id = sf.getID();
            assertTrue(id.matches("\\w+\\.\\d+"));
        }, null);
    }

    private void assertGranulesViewSchema(FeatureType schema, ProductClass expectedClass)
            throws IOException {
        assertThat(schema, instanceOf(SimpleFeatureType.class));
        SimpleFeatureType ft = (SimpleFeatureType) schema;
        // check there are no foreign attributes
        Map<String, Class> mappings = new HashMap<>();
        for (AttributeDescriptor ad : ft.getAttributeDescriptors()) {
            final String adName = ad.getLocalName();
            for (ProductClass pc : ProductClass.values()) {
                if (pc == EOP_GENERIC || pc == expectedClass) {
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
        final PropertyDescriptor layerDescriptor = schema.getDescriptor(LAYER_NAME);
        assertNotNull(layerDescriptor);

        // read it
        FeatureSource<FeatureType, Feature> source = osAccess.getCollectionSource();
        Query q = new Query();
        q.setProperties(Arrays.asList(FF.property(LAYER_NAME)));
        q.setFilter(FF.equal(FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                FF.literal("SENTINEL2"), false));
        FeatureCollection<FeatureType, Feature> features = source.getFeatures(q);

        // get the collection and check it
        Feature collection = DataUtilities.first(features);
        assertNotNull(collection);
        Property layerProperty = collection.getProperty(LAYER_NAME);
        final Feature layerValue = (Feature) layerProperty;
        assertThat(layerValue, notNullValue());

        assertEquals("gs", getAttribute(layerValue, "workspace"));
        assertEquals("sentinel2", getAttribute(layerValue, "layer"));
        assertEquals(Boolean.TRUE, getAttribute(layerValue, "separateBands"));
        assertThat(getAttribute(layerValue, "bands"),
                equalTo(new String[] { "B01", "B02", "B03", "B04", "B05", "B06", "B07", "B08", "B09", "B10", "B11", "B12" }));
        assertThat(getAttribute(layerValue, "browseBands"), equalTo(new String[] { "B04", "B03", "B02" }));
        assertEquals(Boolean.TRUE, getAttribute(layerValue, "heterogeneousCRS"));
        assertEquals("EPSG:4326", getAttribute(layerValue, "mosaicCRS"));
    }

    @Test
    public void testCollectionLayerUpdate() throws Exception {
        // read it
        FeatureStore<FeatureType, Feature> store = (FeatureStore<FeatureType, Feature>) osAccess
                .getCollectionSource();
        Query q = new Query();
        q.setProperties(Arrays.asList(FF.property(LAYER_NAME)));
        final PropertyIsEqualTo filter = FF.equal(
                FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                FF.literal("SENTINEL2"), false);
        q.setFilter(filter);
        FeatureCollection<FeatureType, Feature> features = store.getFeatures(q);

        final Feature layerValue = getLayerPropertyFromCollection(features);

        // modify it
        setAttribute(layerValue, "workspace", "gs2");
        setAttribute(layerValue, "layer", "sentinel12345");
        setAttribute(layerValue, "separateBands", false);
        setAttribute(layerValue, "bands", new String[] { "B01", "B04", "B06" });
        setAttribute(layerValue, "browseBands", null);
        setAttribute(layerValue, "heterogeneousCRS", false);
        setAttribute(layerValue, "mosaicCRS", "EPSG:3857");

        // update the feature
        store.modifyFeatures(new Name[] { LAYER_NAME }, new Object[] { layerValue }, filter);

        // read it back and check
        final Feature layerValue2 = getLayerPropertyFromCollection(store.getFeatures(q));
        assertEquals("gs2", getAttribute(layerValue2, "workspace"));
        assertEquals("sentinel12345", getAttribute(layerValue2, "layer"));
        assertEquals(Boolean.FALSE, getAttribute(layerValue2, "separateBands"));
        assertArrayEquals(new String[] { "B01", "B04", "B06" }, (String[]) getAttribute(layerValue2, "bands"));
        assertThat(getAttribute(layerValue2, "browseBands"), nullValue());
        assertEquals(Boolean.FALSE, getAttribute(layerValue2, "heterogeneousCRS"));
        assertEquals("EPSG:3857", getAttribute(layerValue2, "mosaicCRS"));
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

    private Feature getLayerPropertyFromCollection(
            FeatureCollection<FeatureType, Feature> features) {
        // get the simple feature representing the layer publishing info
        Feature collection = DataUtilities.first(features);
        assertNotNull(collection);
        Property layerProperty = collection.getProperty(LAYER_NAME);
        assertThat(layerProperty, notNullValue());
        final Feature layerValue = (Feature) layerProperty;
        return layerValue;
    }

    @Test
    public void testCollectionLayerRemoval() throws Exception {
        // read it
        FeatureStore<FeatureType, Feature> store = (FeatureStore<FeatureType, Feature>) osAccess
                .getCollectionSource();
        Query q = new Query();
        q.setProperties(Arrays.asList(FF.property(LAYER_NAME)));
        final PropertyIsEqualTo filter = FF.equal(
                FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                FF.literal("SENTINEL2"), false);
        q.setFilter(filter);

        // update the feature to remove the layer information
        store.modifyFeatures(new Name[] { LAYER_NAME }, new Object[] { null }, filter);

        // read it back and check it's not set
        Feature collection = DataUtilities.first(store.getFeatures(q));
        assertNotNull(collection);
        Property layerProperty = collection.getProperty(LAYER_NAME);
        assertNull(layerProperty);
    }

}
