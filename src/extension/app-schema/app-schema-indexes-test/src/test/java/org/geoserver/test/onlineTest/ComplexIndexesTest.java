/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import static org.geoserver.test.onlineTest.Resources.TEST_DATA_DIR;
import static org.geoserver.test.onlineTest.Resources.resourceToString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geotools.data.complex.IndexIdIterator.IndexUniqueVisitorIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.visitor.UniqueVisitor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class ComplexIndexesTest extends GeoServerSystemTestSupport {

    // xpath engines used to check WFS responses
    private XpathEngine WFS11_XPATH_ENGINE;
    private XpathEngine WFS20_XPATH_ENGINE;

    private static String solrUrl;
    private static String solrCoreName;
    // HTTP Apache Solr client
    private static HttpSolrClient solrClient;

    private static PostgresqlProperties pgProps;

    // test root directory
    private static final File TESTS_ROOT_DIR = createTempDirectory("complex-indexes");
    public static final String STATIONS_NAMESPACE = "http://www.stations.org/1.0";
    public static final String OBSERVATIONS_MAPPING_NAME =
            "ObservationType-e17fbd44-fd26-46e7-bd71-e2568073c6c5";
    public static final String STATIONS_MAPPING_NAME =
            "StationType-f46d72da-5591-4873-b210-5ed30a6ffb0d";

    public static final StationsMappingsSetup stationSetup = new StationsMappingsSetup();

    @Test
    public void testPagination() throws Exception {
        try {
            IndexUniqueVisitorIterator.uniqueVisitorBuildHook = this::checkUniqueVisitorLimits;
            // pagination-test-query.xml
            setupXmlUnitNamespaces();
            String wfsQuery = resourceToString(TEST_DATA_DIR + "/pagination-test-query.xml");
            Document responseDoc = postAsDOM("wfs", wfsQuery);
            // without pagination-limit we'd get 3 features, test we got only 1:
            checkCount(
                    WFS20_XPATH_ENGINE,
                    responseDoc,
                    1,
                    "//wfs:FeatureCollection/wfs:member/st:Station");
        } finally {
            IndexUniqueVisitorIterator.uniqueVisitorBuildHook = null;
        }
    }

    public void checkUniqueVisitorLimits(UniqueVisitor visitor) {
        assertEquals(1, visitor.getStartIndex());
        assertEquals(1, visitor.getMaxFeatures());
    }

    @Test
    public void testQueryComplex() throws Exception {
        setupXmlUnitNamespaces();
        String wfsQuery = resourceToString(TEST_DATA_DIR + "/complex-wildcard-query.xml");
        Document responseDoc = postAsDOM("wfs", wfsQuery);
        checkCount(
                WFS20_XPATH_ENGINE,
                responseDoc,
                1,
                "//wfs:FeatureCollection/wfs:member/st:Station");
        checkCount(
                WFS20_XPATH_ENGINE,
                responseDoc,
                1,
                String.format("//wfs:FeatureCollection/wfs:member/st:Station[@gml:id='%s']", "13"));
        // st:stationName = 1_Alessandria
        XMLAssert.assertXpathEvaluatesTo(
                "1_Alessandria",
                "//wfs:FeatureCollection/wfs:member/st:Station[@gml:id='13']/st:stationName",
                responseDoc);
        // wfs:FeatureCollection/wfs:member/st:Station/st:observation/st:Observation
        checkCount(
                WFS20_XPATH_ENGINE,
                responseDoc,
                2,
                "//wfs:FeatureCollection/wfs:member/st:Station[@gml:id='13']/st:observation/st:Observation");
        XMLAssert.assertXpathEvaluatesTo(
                "wrapper",
                "//wfs:FeatureCollection/wfs:member/st:Station[@gml:id='13']/st:observation"
                        + "/st:Observation[@gml:id='1']/st:description",
                responseDoc);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        // load the fixture file
        solrUrl = loadFixture().getProperty("solr_url");
        solrCoreName = loadFixture().getProperty("solr_core");
        // instantiate the Apache Solr client
        solrClient = new HttpSolrClient.Builder(solrUrl).build();
        // clean all stored documents
        solrClient.deleteByQuery("*:*");
        pgProps = loadPgProperties();
        // generate mapping and related files:
        stationSetup.setupMapping(solrUrl, solrCoreName, pgProps, TESTS_ROOT_DIR);
        // setup solr core
        SolrIndexSetup indexSetup = new SolrIndexSetup(solrUrl);
        indexSetup.init();
        // setup postgresql schema
        PgSchemaSetup pgSetup = new PgSchemaSetup(pgProps);
        pgSetup.init();
    }

    @Before
    public void beforeTest() {
        // instantiate WFS 1.1 xpath engine
        Pair<String, String> stationsNamespace = Pair.of("st", STATIONS_NAMESPACE);
        WFS11_XPATH_ENGINE =
                buildXpathEngine(
                        Pair.of("wfs", "http://www.opengis.net/wfs"),
                        Pair.of("gml", "http://www.opengis.net/gml"),
                        stationsNamespace);
        // instantiate WFS 2.0 xpath engine
        WFS20_XPATH_ENGINE =
                buildXpathEngine(
                        Pair.of("wfs", "http://www.opengis.net/wfs/2.0"),
                        Pair.of("gml", "http://www.opengis.net/gml/3.2"),
                        stationsNamespace);
    }

    @AfterClass
    public static void tearDown() {
        try {
            // remove tests root directory
            IOUtils.delete(TESTS_ROOT_DIR);
            // clean all stored documents
            solrClient.deleteByQuery("*:*");
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Error removing tests root directory.", exception);
        }
    }

    /**
     * Helper method that builds a XPATH engine using the base namespaces (ow, ogc, etc ...), all
     * the namespaces available in the GeoServer catalog and the provided extra namespaces.
     */
    protected XpathEngine buildXpathEngine(Pair<String, String>... extraNamespaces) {
        // build xpath engine
        XpathEngine xpathEngine = XMLUnit.newXpathEngine();
        Map<String, String> namespaces = defaultNamespacesMap(extraNamespaces);
        // add namespaces to the xpath engine
        xpathEngine.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        return xpathEngine;
    }

    protected void setupXmlUnitNamespaces() {
        XMLUnit.setXpathNamespaceContext(
                new SimpleNamespaceContext(
                        defaultNamespacesMap(
                                Pair.of("st", STATIONS_NAMESPACE),
                                Pair.of("wfs", "http://www.opengis.net/wfs/2.0"),
                                Pair.of("gml", "http://www.opengis.net/gml/3.2"))));
    }

    protected Map<String, String> defaultNamespacesMap(Pair<String, String>... extraNamespaces) {
        Map<String, String> namespaces = new HashMap<>();
        // add common namespaces
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        // add catalog namespaces
        for (NamespaceInfo namespace : getCatalog().getNamespaces()) {
            namespaces.put(namespace.getPrefix(), namespace.getURI());
        }
        for (Pair<String, String> ns : extraNamespaces) {
            namespaces.put(ns.getLeft(), ns.getRight());
        }
        return namespaces;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // create necessary workspaces
        Catalog catalog = getCatalog();
        setupWorkspaces(catalog);
        // create the app-schema data store
        StoreInfo store = createAppSchemaDataStore(catalog);
        // build the feature type for the root mapping (StationFeature)
        buildFeatureTypes(catalog, store);
    }

    private void buildFeatureTypes(Catalog catalog, StoreInfo store) throws Exception, IOException {
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(store);
        builder.setWorkspace(catalog.getWorkspaceByName("st"));
        // Stations
        FeatureTypeInfo stationsFeatureType =
                builder.buildFeatureType(new NameImpl(null, STATIONS_MAPPING_NAME));
        catalog.add(stationsFeatureType);
        LayerInfo stLayer = builder.buildLayer(stationsFeatureType);
        stLayer.setDefaultStyle(catalog.getStyleByName("point"));
        catalog.add(stLayer);
        // Observations
        FeatureTypeInfo obserFeatureType =
                builder.buildFeatureType(new NameImpl(null, OBSERVATIONS_MAPPING_NAME));
        catalog.add(obserFeatureType);
        LayerInfo obsLayer = builder.buildLayer(obserFeatureType);
        obsLayer.setDefaultStyle(catalog.getStyleByName("point"));
        catalog.add(obsLayer);
    }

    private StoreInfo createAppSchemaDataStore(Catalog catalog) {
        Map<String, Serializable> params = new HashMap<>();
        params.put("dbtype", "app-schema");
        params.put("url", new File(TESTS_ROOT_DIR, "mappings.xml").toURI().toString());
        DataStoreInfoImpl dataStore = new DataStoreInfoImpl(getCatalog());
        dataStore.setId("stations");
        dataStore.setName("stations");
        dataStore.setType("app-schema");
        dataStore.setConnectionParameters(params);
        dataStore.setWorkspace(catalog.getWorkspaceByName("st"));
        dataStore.setEnabled(true);
        catalog.add(dataStore);
        return dataStore;
    }

    private void setupWorkspaces(Catalog catalog) {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName("st");
        NamespaceInfoImpl nameSpace = new NamespaceInfoImpl();
        nameSpace.setPrefix("st");
        nameSpace.setURI(STATIONS_NAMESPACE);
        catalog.add(workspace);
        catalog.add(nameSpace);
    }

    public static PostgresqlProperties loadPgProperties() {
        Properties props = loadFixture();
        PostgresqlProperties pgp = new PostgresqlProperties();
        pgp.setHost(props.getProperty("pg_host", "localhost"));
        pgp.setPort(props.getProperty("pg_port", "5432"));
        pgp.setDatabase(props.getProperty("pg_database"));
        pgp.setSchema(props.getProperty("pg_schema", "meteo"));
        pgp.setUser(props.getProperty("pg_user"));
        pgp.setPassword(props.getProperty("pg_password"));
        return pgp;
    }

    /**
     * Try to load the fixture file associated with this tests, if the load file the tests are
     * skipped.
     */
    private static Properties loadFixture() {
        // get the fixture file path
        File fixFile = getFixtureFile();
        // check if the file exists
        assumeTrue(fixFile.exists());
        // load the fixture file properties
        return loadFixtureProperties(fixFile);
    }

    /** Gets the fixture file for GeoServer Apache Solr integration tests. */
    private static File getFixtureFile() {
        File directory = new File(System.getProperty("user.home") + "/.geoserver");
        if (!directory.exists()) {
            // make sure parent directory exists
            directory.mkdir();
        }
        return new File(directory, "appschema-indexes.properties");
    }

    /** Helper method that just loads the fixture files properties. */
    private static Properties loadFixtureProperties(File fixtureFile) {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(fixtureFile)) {
            // load properties from fixture file
            properties.load(input);
            return properties;
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error reading fixture file '%s'.", fixtureFile.getAbsolutePath()),
                    exception);
        }
    }

    public String tempDirPath() {
        return TESTS_ROOT_DIR.getAbsolutePath();
    }

    /** Helper method that creates a temporary directory. */
    public static File createTempDirectory(String dirName) {
        try {
            return Files.createTempDirectory(dirName).toFile();
        } catch (Exception exception) {
            throw new RuntimeException("Error creating temporary directory.", exception);
        }
    }

    /**
     * Helper method that checks if the provided XPath expression evaluated against the provided XML
     * document yields the expected number of matches.
     */
    private void checkCount(
            XpathEngine xpathEngine, Document document, int expectedCount, String xpath) {
        try {
            // evaluate the xpath and compare the number of nodes found
            assertEquals(expectedCount, xpathEngine.getMatchingNodes(xpath, document).getLength());
        } catch (Exception exception) {
            throw new RuntimeException("Error evaluating xpath.", exception);
        }
    }
}
