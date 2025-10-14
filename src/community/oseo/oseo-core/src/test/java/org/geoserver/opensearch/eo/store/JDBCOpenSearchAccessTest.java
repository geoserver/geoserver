/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.geoserver.opensearch.eo.ProductClass.GENERIC;
import static org.geoserver.opensearch.eo.ProductClass.OPTICAL;
import static org.geoserver.opensearch.eo.ProductClass.RADAR;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.EO_NAMESPACE;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.LAYERS;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.easymock.EasyMock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OSEOInfoImpl;
import org.geoserver.opensearch.eo.ProductClass;
import org.geoserver.opensearch.eo.store.Indexable.FieldType;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.decorators.DecoratingSimpleFeatureSource;
import org.geotools.api.data.DataAccessFinder;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.style.Style;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeImpl;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.styling.StyleBuilder;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.DefaultLoggerFactory;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Polygon;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class JDBCOpenSearchAccessTest {

    public static final String TEST_NAMESPACE = "http://www.test.com/os/eo";

    private static JDBCDataStore store;

    private static OpenSearchAccess osAccess;

    private static FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    public static final ProductClass GS_PRODUCT =
            new ProductClass("geoServer", "gs", "http://www.geoserver.org/eo/test");

    // make sure Java logging is used
    @BeforeClass
    public static void setupLogging() throws IOException, SQLException {
        GeoTools.setLoggerFactory(DefaultLoggerFactory.getInstance());
    }

    /**
     * Returns the test fixture to run tests against a PostGIS database
     *
     * @return
     */
    public static Properties getFixture() {
        Properties properties = GSFixtureUtilitiesDelegate.loadFixture("oseo-postgis");
        if (properties != null) {
            properties.put("Expose primary keys", "true");
            properties.put("dbtype", "postgis");
        }
        return properties;
    }

    @BeforeClass
    public static void setupStore() throws IOException, SQLException {
        osAccess = setupAndReturnStore();
    }

    public static OpenSearchAccess setupAndReturnStore() throws IOException, SQLException {
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
        expect(geoServer.getService(OSEOInfo.class)).andReturn(impl).anyTimes();

        // prepare the catalog as well
        Catalog catalog = EasyMock.createNiceMock(Catalog.class);
        expect(geoServer.getCatalog()).andReturn(catalog).anyTimes();
        CoverageInfo ci = EasyMock.createNiceMock(CoverageInfo.class);
        expect(ci.getTitle()).andReturn("The title").anyTimes();
        expect(ci.getDescription()).andReturn("The description").anyTimes();
        expect(ci.getDisabledServices()).andReturn(List.of("WCS")).anyTimes();
        replay(ci);
        StyleBuilder sb = new StyleBuilder();
        Style rasterStyle = sb.createStyle(sb.createRasterSymbolizer());
        rasterStyle.getDescription().setTitle("The raster style");
        StyleInfo siRaster = EasyMock.createNiceMock(StyleInfo.class);
        expect(siRaster.getName()).andReturn("raster").anyTimes();
        expect(siRaster.getStyle()).andReturn(rasterStyle).anyTimes();
        replay(siRaster);
        Style polyStyle = sb.createStyle(sb.createPolygonSymbolizer());
        polyStyle.setName("The polygon style");
        StyleInfo siPoly = EasyMock.createNiceMock(StyleInfo.class);
        expect(siPoly.getName()).andReturn("polygon").anyTimes();
        expect(siPoly.getStyle()).andReturn(polyStyle).anyTimes();
        replay(siPoly);
        LayerInfo li = EasyMock.createNiceMock(LayerInfo.class);
        expect(li.getResource()).andReturn(ci).anyTimes();
        expect(li.getStyles()).andReturn(Set.of(siPoly)).anyTimes();
        expect(li.getDefaultStyle()).andReturn(siRaster).anyTimes();
        replay(li);
        expect(catalog.getLayerByName("gs:sentinel2")).andReturn(li).anyTimes();
        replay(catalog);

        expect(geoServer.getServices()).andReturn(Collections.emptyList()).anyTimes();
        replay(geoServer);

        // create the OpenSeach wrapper store
        params = new HashMap<>();
        params.put("dbtype", "opensearch-eo-jdbc");
        params.put("store", "test:jdbcStore");
        params.put("namespace", TEST_NAMESPACE);
        params.put("repository", repository);
        return (OpenSearchAccess) DataAccessFinder.getDataStore(params);
    }

    @After
    public void resetCollectionLayer() throws IOException, SQLException {
        String s1 = "DELETE from \"collection_layer\"";
        String s2 =
                """
                INSERT into "collection_layer"
                ("cid", "workspace", "layer", "separateBands", "bands", "browseBands", "heterogeneousCRS", "mosaicCRS", "defaultLayer")
                VALUES(17, 'gs', 'sentinel2', 'true', 'B01,B02,B03,B04,B05,B06,B07,B08,B09,B10,B11,B12', 'B04,B03,B02', 'true', 'EPSG:4326', 'true')""";
        String s3 =
                """
                INSERT into collection_layer
                ("cid", "workspace", "layer", "separateBands", "bands", "browseBands", "heterogeneousCRS", "mosaicCRS", "defaultLayer")
                VALUES(31, 'gs', 'landsat8-SINGLE', 'false', null, null, 'true', 'EPSG:4326', 'true');
                """;
        String s4 =
                """
                INSERT into collection_layer
                ("cid", "workspace", "layer", "separateBands", "bands", "browseBands", "heterogeneousCRS", "mosaicCRS", "defaultLayer")
                VALUES(31, 'gs', 'landsat8-SEPARATE', 'true', 'B01,B02,B03,B04,B05,B06,B07,B08,B09', 'B04,B03,B02', 'true', 'EPSG:4326', 'false');\
                """;
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

            createTables(conn);
            populateCollections(conn);
            populateProducts(conn);
            if (addGranuleTable) {
                populateGranules(conn);
            }
            addCustomProductClass(conn);
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
            List<String> lines = IOUtils.readLines(is).stream()
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

        PropertyDescriptor cd = schema.getDescriptor(OpenSearchAccess.COLLECTION_PROPERTY_NAME);
        assertNotNull(cd);
        assertEquals(OpenSearchAccess.COLLECTION_PROPERTY_NAME, cd.getType().getName());
    }

    @Test
    public void testProductReadingNoJoins() throws Exception {
        Query q = new Query();
        String id = "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TVG_N02.01";
        q.setFilter(FF.equals(FF.property(new NameImpl(GENERIC.getNamespace(), "identifier")), FF.literal(id)));
        Feature feature = DataUtilities.first(osAccess.getProductSource().getFeatures(q));
        assertEquals("DATA_DRIVEN", feature.getProperty("processingMode").getValue());
        assertEquals(id, feature.getProperty("identifier").getValue());
        assertEquals("SENTINEL2", feature.getProperty("parentIdentifier").getValue());
    }

    @Test
    public void testProductReadingJoins() throws Exception {
        Query q = new Query();
        q.setProperties(Arrays.asList(
                FF.property(new NameImpl(GENERIC.getNamespace(), "identifier")),
                FF.property(new NameImpl(GENERIC.getNamespace(), "processingMode")),
                FF.property(new NameImpl(GENERIC.getNamespace(), "parentIdentifier")),
                FF.property(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME),
                FF.property(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME),
                FF.property(OpenSearchAccess.COLLECTION_PROPERTY_NAME)));
        String id = "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TVG_N02.01";
        q.setFilter(FF.equals(FF.property(new NameImpl(GENERIC.getNamespace(), "identifier")), FF.literal(id)));
        Feature feature = DataUtilities.first(osAccess.getProductSource().getFeatures(q));
        assertEquals("DATA_DRIVEN", feature.getProperty("processingMode").getValue());
        assertEquals(id, feature.getProperty("identifier").getValue());
        assertEquals("SENTINEL2", feature.getProperty("parentIdentifier").getValue());
        Feature collection = (Feature) feature.getProperty("collection");
        assertNotNull(collection);
        assertEquals("SENTINEL2", collection.getProperty("identifier").getValue());
        assertEquals("S2MSI1C", collection.getProperty("productType").getValue());
    }

    @Test
    public void testTypeNames() throws Exception {
        List<Name> names = osAccess.getNames();
        // product, collection, SENTINEL1, SENTINEL2, LANDSAT8, ATM1,
        assertThat(names, hasSize(29));
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
                        "ATMTEST2",
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
                        "LANDSAT8__B09",
                        "DISABLED_COLLECTION"));
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
        fc.accepts(f -> {}, null); // just check trying to scroll over the feature does not make it blow
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
                    Assert.assertTrue(id.matches("\\w+\\.\\d+"));
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
        fc.accepts(f -> {}, null); // just check trying to scroll over the feature does not make it blow
    }

    private void assertGranulesViewSchema(FeatureType schema, ProductClass expectedClass) throws IOException {
        assertThat(schema, instanceOf(SimpleFeatureType.class));
        SimpleFeatureType ft = (SimpleFeatureType) schema;
        // check there are no foreign attributes
        Map<String, Class<?>> mappings = new HashMap<>();
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
        // check for columns that have an instance in both product and collection
        assertThat(mappings, hasEntry(equalTo("collectionEoIdentifier"), equalTo(String.class)));
        assertThat(mappings, hasEntry(equalTo("collectionEoAcquisitionStation"), equalTo(String.class)));
    }

    private void assertPropertyNamespace(FeatureType schema, String name, String namespaceURI) {
        PropertyDescriptor wl = schema.getDescriptor(name);
        Assert.assertNotNull(wl);
        assertEquals(namespaceURI, wl.getName().getNamespaceURI());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectionLayerInformation() throws Exception {
        // check expected property is there
        FeatureType schema = osAccess.getCollectionSource().getSchema();
        Name name = schema.getName();
        Name layersName = osAccess.getName(LAYERS);
        assertEquals(TEST_NAMESPACE, name.getNamespaceURI());
        final PropertyDescriptor layerDescriptor = schema.getDescriptor(layersName);
        Assert.assertNotNull(layerDescriptor);

        // read it
        FeatureSource<FeatureType, Feature> source = osAccess.getCollectionSource();
        Query q = new Query();
        q.setProperties(Arrays.asList(FF.property(layersName)));
        q.setFilter(FF.equal(
                FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                FF.literal("SENTINEL2"),
                false));
        FeatureCollection<FeatureType, Feature> features = source.getFeatures(q);

        // get the collection and check it
        Feature collection = DataUtilities.first(features);
        Assert.assertNotNull(collection);
        Property layerProperty = collection.getProperty(layersName);
        final Feature layerValue = (Feature) layerProperty;
        assertThat(layerValue, notNullValue());

        assertEquals("gs", getAttribute(layerValue, "workspace"));
        assertEquals("sentinel2", getAttribute(layerValue, "layer"));
        assertEquals(Boolean.TRUE, getAttribute(layerValue, "separateBands"));
        assertThat(getAttribute(layerValue, "bands"), equalTo(new String[] {
            "B01", "B02", "B03", "B04", "B05", "B06", "B07", "B08", "B09", "B10", "B11", "B12"
        }));
        assertThat(getAttribute(layerValue, "browseBands"), equalTo(new String[] {"B04", "B03", "B02"}));
        assertEquals(Boolean.TRUE, getAttribute(layerValue, "heterogeneousCRS"));
        assertEquals("EPSG:4326", getAttribute(layerValue, "mosaicCRS"));
        assertEquals("The title", getAttribute(layerValue, "title"));
        assertEquals("The description", getAttribute(layerValue, "description"));
        List<Attribute> styles = (List<Attribute>) getAttribute(layerValue, "styles");
        SimpleFeature raster = (SimpleFeature) styles.get(0);
        assertEquals("raster", getAttribute(raster, "name"));
        assertEquals("The raster style", getAttribute(raster, "title"));
        SimpleFeature polygon = (SimpleFeature) styles.get(1);
        assertEquals("polygon", getAttribute(polygon, "name"));
        assertEquals("The polygon style", getAttribute(polygon, "title"));
    }

    @Test
    public void testTwoCollectionLayers() throws Exception {
        // read it
        FeatureStore<FeatureType, Feature> store = (FeatureStore<FeatureType, Feature>) osAccess.getCollectionSource();
        Query q = new Query();
        Name layersName = osAccess.getName(LAYERS);
        q.setProperties(Arrays.asList(FF.property(layersName)));
        final PropertyIsEqualTo filter = FF.equal(
                FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")), FF.literal("LANDSAT8"), false);
        q.setFilter(filter);
        FeatureCollection<FeatureType, Feature> features = store.getFeatures(q);

        Map<String, Feature> layerFeatures = getLayerPropertiesFromCollection(features);
        assertThat(layerFeatures.keySet(), Matchers.hasItems("landsat8-SINGLE", "landsat8-SEPARATE"));

        // first layer
        Feature single = layerFeatures.get("landsat8-SINGLE");
        assertEquals("gs", getAttribute(single, "workspace"));
        assertEquals("landsat8-SINGLE", getAttribute(single, "layer"));
        assertEquals(Boolean.FALSE, getAttribute(single, "separateBands"));
        Assert.assertNull(getAttribute(single, "bands"));
        Assert.assertNull(getAttribute(single, "browseBands"));
        assertEquals(Boolean.TRUE, getAttribute(single, "heterogeneousCRS"));
        assertEquals("EPSG:4326", getAttribute(single, "mosaicCRS"));

        // second layer
        Feature separate = layerFeatures.get("landsat8-SEPARATE");
        assertEquals("gs", getAttribute(separate, "workspace"));
        assertEquals("landsat8-SEPARATE", getAttribute(separate, "layer"));
        assertEquals(Boolean.TRUE, getAttribute(separate, "separateBands"));
        assertThat(
                getAttribute(separate, "bands"),
                equalTo(new String[] {"B01", "B02", "B03", "B04", "B05", "B06", "B07", "B08", "B09"}));
        assertThat(getAttribute(separate, "browseBands"), equalTo(new String[] {"B04", "B03", "B02"}));
        assertEquals(Boolean.TRUE, getAttribute(separate, "heterogeneousCRS"));
        assertEquals("EPSG:4326", getAttribute(separate, "mosaicCRS"));
    }

    @Test
    public void testCollectionLayerUpdate() throws Exception {
        // read it
        FeatureStore<FeatureType, Feature> store = (FeatureStore<FeatureType, Feature>) osAccess.getCollectionSource();
        Query q = new Query();
        final PropertyIsEqualTo filter = FF.equal(
                FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")), FF.literal("SENTINEL2"), false);
        q.setFilter(filter);
        FeatureCollection<FeatureType, Feature> features = store.getFeatures(q);

        final Feature layerValue = getLayerPropertyFromCollection(features);

        // modify it
        setAttribute(layerValue, "workspace", "gs2");
        setAttribute(layerValue, "layer", "sentinel12345");
        setAttribute(layerValue, "separateBands", false);
        setAttribute(layerValue, "bands", new String[] {"B01", "B04", "B06"});
        setAttribute(layerValue, "browseBands", null);
        setAttribute(layerValue, "heterogeneousCRS", false);
        setAttribute(layerValue, "mosaicCRS", "EPSG:3857");
        ListFeatureCollection layers = new ListFeatureCollection(osAccess.getCollectionLayerSchema());
        layers.add(toLayerSimpleFeature(layerValue));

        // update the feature
        Name layersName = osAccess.getName(LAYERS);
        store.modifyFeatures(new Name[] {layersName}, new Object[] {layers}, filter);

        // read it back and check
        final Feature layerValue2 = getLayerPropertyFromCollection(store.getFeatures(q));
        assertEquals("gs2", getAttribute(layerValue2, "workspace"));
        assertEquals("sentinel12345", getAttribute(layerValue2, "layer"));
        assertEquals(Boolean.FALSE, getAttribute(layerValue2, "separateBands"));
        Assert.assertArrayEquals(new String[] {"B01", "B04", "B06"}, (String[]) getAttribute(layerValue2, "bands"));
        assertThat(getAttribute(layerValue2, "browseBands"), nullValue());
        assertEquals(Boolean.FALSE, getAttribute(layerValue2, "heterogeneousCRS"));
        assertEquals("EPSG:3857", getAttribute(layerValue2, "mosaicCRS"));
    }

    @Test
    public void testCollectionLayerUpdateMulti() throws Exception {
        // read it
        FeatureStore<FeatureType, Feature> store = (FeatureStore<FeatureType, Feature>) osAccess.getCollectionSource();
        Query q = new Query();
        final PropertyIsEqualTo filter = FF.equal(
                FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")), FF.literal("LANDSAT8"), false);
        q.setFilter(filter);
        FeatureCollection<FeatureType, Feature> features = store.getFeatures(q);

        Map<String, Feature> layerFeatures = getLayerPropertiesFromCollection(features);
        assertThat(layerFeatures.keySet(), Matchers.hasItems("landsat8-SINGLE", "landsat8-SEPARATE"));
        Feature layerSingle = layerFeatures.get("landsat8-SINGLE");

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
        ListFeatureCollection layers = new ListFeatureCollection(osAccess.getCollectionLayerSchema());
        layers.add(toLayerSimpleFeature(layerSingle));
        layers.add(newLayer);

        // update the feature
        Name layersName = osAccess.getName(LAYERS);
        store.modifyFeatures(new Name[] {layersName}, new Object[] {layers}, filter);

        // read it back and check
        layerFeatures = getLayerPropertiesFromCollection(features);
        assertThat(layerFeatures.keySet(), Matchers.hasItems("landsat-foobar", "landsat-third"));
        final Feature layerFooBar = layerFeatures.get("landsat-foobar");
        assertEquals("gs2", getAttribute(layerFooBar, "workspace"));
        assertEquals("landsat-foobar", getAttribute(layerFooBar, "layer"));
        assertEquals(Boolean.FALSE, getAttribute(layerFooBar, "separateBands"));
        Assert.assertArrayEquals(new String[] {"B01", "B04", "B06"}, (String[]) getAttribute(layerFooBar, "bands"));
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

    private SimpleFeature toLayerSimpleFeature(Feature layerSingle) throws IOException {
        SimpleFeatureType schema = osAccess.getCollectionLayerSchema();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
            final String adName = ad.getLocalName();
            final Object value = getAttribute(layerSingle, adName);
            fb.set(adName, value);
        }
        return fb.buildFeature(layerSingle.getIdentifier().getID());
    }

    static Object getAttribute(Feature f, String name) {
        Collection<Property> properties = f.getProperties(name);
        if (properties != null && !properties.isEmpty()) {
            PropertyDescriptor descriptor = f.getType().getDescriptor(name);
            if (descriptor.getMaxOccurs() > 1) {
                return properties; // already a collection
            } else {
                Property p = properties.iterator().next();
                return p.getValue();
            }
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

    private Feature getLayerPropertyFromCollection(FeatureCollection<FeatureType, Feature> features) {
        Map<String, Feature> layerProperty = getLayerPropertiesFromCollection(features);
        return layerProperty.values().iterator().next();
    }

    private Map<String, Feature> getLayerPropertiesFromCollection(FeatureCollection<FeatureType, Feature> features) {
        // get the simple feature representing the layer publishing info
        Feature collection = DataUtilities.first(features);
        Assert.assertNotNull(collection);
        Collection<Property> layerProperty = collection.getProperties(osAccess.getName(LAYERS));
        assertThat(layerProperty, notNullValue());
        assertThat(layerProperty, not(empty()));

        return layerProperty.stream()
                .map(p -> (Feature) p)
                .collect(Collectors.toMap(
                        sf -> (String) sf.getProperty("layer").getValue(), sf -> sf, (u, v) -> u, LinkedHashMap::new));
    }

    @Test
    public void testCollectionLayerRemoval() throws Exception {
        // read it
        FeatureStore<FeatureType, Feature> store = (FeatureStore<FeatureType, Feature>) osAccess.getCollectionSource();
        Query q = new Query();
        final PropertyIsEqualTo filter = FF.equal(
                FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")), FF.literal("SENTINEL2"), false);
        q.setFilter(filter);

        // update the feature to remove the layer information
        Name layersName = osAccess.getName(LAYERS);
        store.modifyFeatures(new Name[] {layersName}, new Object[] {null}, filter);

        // read it back and check it's not set
        Feature collection = DataUtilities.first(store.getFeatures(q));
        Assert.assertNotNull(collection);
        Property layerProperty = collection.getProperty(layersName);
        Assert.assertNull(layerProperty);
    }

    @Test
    public void testIndexCreationRemoval() throws Exception {
        Indexable simple = new Indexable("eo:cloud_cover", CQL.toExpression("opt:cloudCover"), FieldType.Other);
        Indexable geom = new Indexable("geometry", CQL.toExpression("footprint"), FieldType.Geometry);
        Indexable array = new Indexable("keywords", CQL.toExpression("keywords"), FieldType.Array);
        Indexable json = new Indexable(
                "jsontest2",
                CQL.toExpression("jsonPointer(extraProperties, '/sar:looks_range')"),
                FieldType.JsonInteger);
        osAccess.updateIndexes("SENTINEL2", Arrays.asList(simple, geom, array, json));

        // check the index names
        List<String> indexNames = osAccess.getIndexNames("product");
        assertThat(
                indexNames,
                Matchers.hasItems(
                        "sentinel2_jsontest2_idx",
                        "sentinel2_keywords_idx",
                        "sentinel2_geometry_idx",
                        "sentinel2_eo_cloud_cover_idx"));

        // check the type of indexes that got created
        Map<String, String> expectations = new HashMap<>();
        expectations.put(
                "sentinel2_jsontest2_idx", "USING btree (((\"extraProperties\" ->> 'sar:looks_range'::text)))");
        expectations.put("sentinel2_eo_cloud_cover_idx", "USING btree (\"optCloudCover\")");
        expectations.put("sentinel2_geometry_idx", "USING gist (footprint)");
        expectations.put("sentinel2_keywords_idx", "USING gin (keywords)");
        String sql =
                """
                SELECT indexname,indexdef
                FROM pg_indexes
                WHERE schemaname = 'public' and tablename = 'product' and indexname like 'sentinel2_%'""";
        try (Connection cx = store.getConnection(Transaction.AUTO_COMMIT);
                Statement st = cx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString(1);
                String definition = rs.getString(2);
                assertThat(expectations, hasKey(name));
                assertThat(definition, containsString(expectations.get(name)));
            }
        }

        // now drop all the indexes
        osAccess.updateIndexes("SENTINEL2", Collections.emptyList());
        assertThat(osAccess.getIndexNames("product"), not(hasItem(startsWith("sentinel2_"))));
        try (Connection cx = store.getConnection(Transaction.AUTO_COMMIT);
                Statement st = cx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            assertFalse(rs.next());
        }
    }

    @Test
    public void testJDBCProducteFeatureStoreSortJSONB() throws Exception {
        JDBCProductFeatureStore jdbcProductFeatureStore = (JDBCProductFeatureStore) osAccess.getProductSource();
        jdbcProductFeatureStore.jsonBProperties = new HashSet<>(Arrays.asList(new NameImpl("string")));
        SimpleFeatureType featureType = DataUtilities.createType("testType", "string:String,int:Integer,double:Double");
        ComplexFeatureBuilder complexFeatureBuilder = new ComplexFeatureBuilder(featureType);
        SimpleFeature f = SimpleFeatureBuilder.build(
                featureType,
                new Object[] {
                    "{\"g\":1,\"m\":2,\"f\":3,\"h\":4,\"c\":5,\"a\":{\"hello\":6,\"archive\":7,\"meh\":{\"working\":8,\"aver\":9}},\"opt:cloudCover\":34}",
                    Integer.valueOf(3),
                    Double.valueOf(3.3)
                },
                "fid.3");
        jdbcProductFeatureStore.mapPropertiesToComplex(complexFeatureBuilder, f, new HashMap<>());
        assertEquals(
                "{\"a\":{\"archive\":7,\"hello\":6,\"meh\":{\"aver\":9,\"working\":8}},\"c\":5,\"f\":3,\"g\":1,\"h\":4,\"m\":2,\"opt:cloudCover\":34}",
                f.getAttribute("string").toString());
    }

    @Test
    public void testJDBCVisitQuery() throws Exception {
        JDBCProductFeatureStore testSource =
                new JDBCProductFeatureStore(
                        (JDBCOpenSearchAccess) osAccess,
                        osAccess.getProductSource().getSchema()) {
                    @Override
                    public SimpleFeatureSource getDelegateSource() throws IOException {
                        SimpleFeatureSource delegate = super.getDelegateSource();
                        return new DecoratingSimpleFeatureSource(delegate) {
                            @Override
                            public SimpleFeatureCollection getFeatures(Query query) throws IOException {
                                // query filter did not turn into a list of id matches
                                assertEquals(Filter.INCLUDE, query.getFilter());
                                return super.getFeatures(query);
                            }
                        };
                    }
                };

        // max visitor, no paging, will use joining, should avoid loading all ids in advance
        MaxVisitor visitor = new MaxVisitor("timeStart");
        testSource.getFeatures().accepts(visitor, null);
        Date maxDate = (Date) visitor.getMax();
        FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ssZZ", TimeZone.getTimeZone("UTC"));
        assertEquals("2018-02-27 09:20:21Z", format.format(maxDate));
    }

    @Test
    public void testCollectionCaching() throws Exception {
        // setup environment to cache stuff in
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);
        // simulate the presence of a OGC request
        Dispatcher.REQUEST.set(new Request());

        try {

            // grab the collections, check two well known entries
            FeatureSource<FeatureType, Feature> collections = osAccess.getCollectionSource();
            Set<String> collectionIdentifiers = getCollectionIdentifiers(collections);
            assertThat(collectionIdentifiers, hasItems("SENTINEL2", "LANDSAT8"));

            // check it has been cached too
            Set<String> cachedCollections = (Set<String>)
                    attrs.getAttribute(WorkspaceFeatureSource.WS_COLLECTION_CACHE_KEY, RequestAttributes.SCOPE_REQUEST);
            assertThat(cachedCollections, hasItems("SENTINEL2", "LANDSAT8"));

            // now corrupt the cache, remove one of the two entries
            cachedCollections.remove("SENTINEL2");

            // the query should now miss SENTINEL2 too, since it's using the cached results
            collectionIdentifiers = getCollectionIdentifiers(collections);
            assertThat(collectionIdentifiers, allOf(hasItems("LANDSAT8"), not(hasItems("SENTINEL2"))));
        } finally {
            // clean up
            Dispatcher.REQUEST.remove();
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private static Set<String> getCollectionIdentifiers(FeatureSource<FeatureType, Feature> collections)
            throws IOException {
        Set<String> collectionIdentifiers = new LinkedHashSet<>();
        collections
                .getFeatures(Query.ALL)
                .accepts(
                        f -> {
                            String id = (String) f.getProperty("identifier").getValue();
                            collectionIdentifiers.add(id);
                        },
                        null);
        return collectionIdentifiers;
    }
}
