/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geotools.data.solr.TestsSolrUtils;
import org.geotools.data.solr.complex.StationsSetup;
import org.geotools.feature.NameImpl;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.geotools.xml.resolver.SchemaCache;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * This class contains the integration tests (online tests) for the integration between App-Schema
 * and Apache Solr. To activate this tests a fixture file needs to be created in the user home, this
 * follows the usual GeoServer conventions for fixture files. Read the README.rst file for more
 * instructions.
 */
public final class ComplexSolrTest extends GeoServerSystemTestSupport {

    // xpath engines used to check WFS responses
    private XpathEngine WFS11_XPATH_ENGINE;
    private XpathEngine WFS20_XPATH_ENGINE;

    // test root directory
    private static final File TESTS_ROOT_DIR = TestsSolrUtils.createTempDirectory("complex-solr");

    static {
        // create and set App-Schema cache directory
        System.setProperty(
                SchemaCache.PROVIDED_CACHE_LOCATION_KEY,
                new File(TESTS_ROOT_DIR, "app-schema-cache").getAbsolutePath());
    }

    private static String solrUrl;
    // HTTP Apache Solr client
    private static HttpSolrClient solrClient;

    @BeforeClass
    public static void beforeClass() {
        // load the fixture file
        solrUrl = loadFixture().getProperty("solr_url");
        // instantiate the Apache Solr client
        solrClient = new HttpSolrClient.Builder(solrUrl).build();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // configure the target Apache Solr core
        StationsSetup.setupSolrIndex(solrClient);
        // prepare the App-Schema configuration files
        StationsSetup.prepareAppSchemaFiles(TESTS_ROOT_DIR, solrUrl);
        Catalog catalog = getCatalog();
        // create necessary workspaces
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName("st");
        NamespaceInfoImpl nameSpace = new NamespaceInfoImpl();
        nameSpace.setPrefix("st");
        nameSpace.setURI("http://www.stations.org/1.0");
        catalog.add(workspace);
        catalog.add(nameSpace);
        // create the app-schema data store
        Map<String, Serializable> params = new HashMap<>();
        params.put("dbtype", "app-schema");
        params.put("url", new File(TESTS_ROOT_DIR, "mappings.xml").toURI().toString());
        DataStoreInfoImpl dataStore = new DataStoreInfoImpl(getCatalog());
        dataStore.setId("stations");
        dataStore.setName("stations");
        dataStore.setType("app-schema");
        dataStore.setConnectionParameters(params);
        dataStore.setWorkspace(workspace);
        dataStore.setEnabled(true);
        catalog.add(dataStore);
        // build the feature type for the root mapping (StationFeature)
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(dataStore);
        builder.setWorkspace(workspace);
        FeatureTypeInfo featureType =
                builder.buildFeatureType(new NameImpl(nameSpace.getURI(), "Station"));
        catalog.add(featureType);
        LayerInfo layer = builder.buildLayer(featureType);
        layer.setDefaultStyle(catalog.getStyleByName("point"));
        catalog.add(layer);
    }

    @Before
    public void beforeTest() {
        // instantiate WFS 1.1 xpath engine
        WFS11_XPATH_ENGINE =
                buildXpathEngine(
                        "wfs", "http://www.opengis.net/wfs",
                        "gml", "http://www.opengis.net/gml");
        // instantiate WFS 2.0 xpath engine
        WFS20_XPATH_ENGINE =
                buildXpathEngine(
                        "wfs", "http://www.opengis.net/wfs/2.0",
                        "gml", "http://www.opengis.net/gml/3.2");
    }

    @AfterClass
    public static void tearDown() {
        try {
            // remove tests root directory
            IOUtils.delete(TESTS_ROOT_DIR);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Error removing tests root directory.", exception);
        }
    }

    @Test
    public void testGetStationFeatures() throws Exception {
        // perform a WFS GetFeature request returning all the available complex features
        Document document =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&srsName=EPSG:4326&typename=st:Station");
        // check that we got the expected stations
        checkStationsNumber(2, WFS11_XPATH_ENGINE, document);
        checkStationData(7, "Bologna", "POINT (11.34 44.5)", WFS11_XPATH_ENGINE, document);
        checkStationData(13, "Alessandria", "POINT (8.63 44.92)", WFS11_XPATH_ENGINE, document);
    }

    @Test
    public void testFilterStationFeatures() throws Exception {
        // perform a WFS GetFeature POST request matching only a single station
        String postContent = readResourceContent("/querys/postQuery1.xml");
        Document document =
                postAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&srsName=EPSG:4326&typename=st:Station",
                        postContent);
        // check that we got the expected stations
        checkStationsNumber(1, WFS11_XPATH_ENGINE, document);
        checkStationData(7, "Bologna", "POINT (11.34 44.5)", WFS11_XPATH_ENGINE, document);
        checkNoStationId(13, WFS11_XPATH_ENGINE, document);
    }

    @Test
    public void testStationsWmsGetMap() throws Exception {
        // execute the WMS GetMap request that should render all stations
        MockHttpServletResponse result =
                getAsServletResponse(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES&LAYERS=st:Station"
                                + "&SRS=EPSG:4326&WIDTH=768&HEIGHT=768"
                                + "&BBOX=5,40,15,50");
        assertThat(result.getStatus(), is(200));
        assertThat(result.getContentType(), is("image/png"));
        // check that we got the expected image back
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(getBinary(result)));
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("/results/wms_result.png")), image, 10);
    }

    @Test
    public void testStationsWmsGetFeatureInfo() throws Exception {
        // execute a WMS GetFeatureInfo request that should hit the Alessandria station
        Document document =
                getAsDOM(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetFeatureInfo&FORMAT=image/png&TRANSPARENT=true&QUERY_LAYERS=st:Station"
                                + "&STYLES&LAYERS=st:Station&INFO_FORMAT=text/xml; subtype=gml/3.1.1"
                                + "&FEATURE_COUNT=50&X=278&Y=390&SRS=EPSG:4326&WIDTH=768&HEIGHT=768"
                                + "&BBOX=5,40,15,50");
        checkStationData(13, "Alessandria", "POINT (8.63 44.92)", WFS11_XPATH_ENGINE, document);
        checkNoStationId(7, WFS11_XPATH_ENGINE, document);
        // execute a WMS GetFeatureInfo request that should hit the Bologna station
        document =
                getAsDOM(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetFeatureInfo&FORMAT=image/png&TRANSPARENT=true&QUERY_LAYERS=st:Station"
                                + "&STYLES&LAYERS=st:Station&INFO_FORMAT=text/xml; subtype=gml/3.1.1"
                                + "&FEATURE_COUNT=50&X=486&Y=422&SRS=EPSG:4326&WIDTH=768&HEIGHT=768"
                                + "&BBOX=5,40,15,50");
        checkStationData(7, "Bologna", "POINT (11.34 44.5)", WFS11_XPATH_ENGINE, document);
        checkNoStationId(13, WFS11_XPATH_ENGINE, document);
    }

    /**
     * Helper method that just checks that a station that matches the provided attributes exists in
     * the XML response.
     */
    private void checkStationData(
            Integer id, String name, String position, XpathEngine engine, Document document) {
        checkCount(
                engine,
                document,
                1,
                String.format(
                        "/wfs:FeatureCollection/gml:featureMembers"
                                + "/st:Station[@gml:id='%s'][st:stationName='%s'][st:position='%s']",
                        id, name, position));
    }

    /**
     * Helper method that just checks that there is no station that matches the provided ID in the
     * XML response.
     */
    private void checkNoStationId(Integer id, XpathEngine engine, Document document) {
        checkCount(
                engine,
                document,
                0,
                String.format(
                        "/wfs:FeatureCollection/gml:featureMembers/st:Station[@gml:id='%s']", id));
    }

    /**
     * Helper method that just checks that the XML response contains the expected number of
     * stations.
     */
    private void checkStationsNumber(int expected, XpathEngine engine, Document document) {
        checkCount(
                engine, document, expected, "/wfs:FeatureCollection/gml:featureMembers/st:Station");
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
        return new File(directory, "solr.properties");
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

    /**
     * Helper method that builds a XPATH engine using the base namespaces (ow, ogc, etc ...), all
     * the namespaces available in the GeoServer catalog and the provided extra namespaces.
     */
    private XpathEngine buildXpathEngine(String... extraNamespaces) {
        // build xpath engine
        XpathEngine xpathEngine = XMLUnit.newXpathEngine();
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
        // add provided namespaces
        if (extraNamespaces.length % 2 != 0) {
            throw new RuntimeException("Invalid number of namespaces provided.");
        }
        for (int i = 0; i < extraNamespaces.length; i += 2) {
            namespaces.put(extraNamespaces[i], extraNamespaces[i + 1]);
        }
        // add namespaces to the xpath engine
        xpathEngine.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        return xpathEngine;
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

    /** Helper method that reads the content of a resource to a string. */
    private static String readResourceContent(String resource) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = ComplexSolrTest.class.getResourceAsStream(resource)) {
            IOUtils.copy(input, output);
            return new String(output.toByteArray());
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Error reading resource '%s' content.", resource), exception);
        }
    }
}
