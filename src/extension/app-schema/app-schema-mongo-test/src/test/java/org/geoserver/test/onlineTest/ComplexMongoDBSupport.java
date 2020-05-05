/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geotools.feature.NameImpl;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.hamcrest.MatcherAssert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Support for integration tests between MongoDB and App-schema. This test are integration tests
 * hence they require a MongoDB instance. If no fixture file for MongoDB exists these tests will be
 * skipped.
 */
public abstract class ComplexMongoDBSupport extends GeoServerSystemTestSupport {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(ComplexMongoDBSupport.class);

    protected static final Path ROOT_DIRECTORY = createTempDir();

    private static File APP_SCHEMA_MAPPINGS;

    protected static final String STATIONS_STORE_NAME = UUID.randomUUID().toString();
    private static final String STATIONS_DATA_BASE_NAME = UUID.randomUUID().toString();
    protected static final String STATIONS_COLLECTION_NAME = "stations";

    private static MongoClient MONGO_CLIENT;

    // xpath engines used to check WFS responses
    protected XpathEngine WFS11_XPATH_ENGINE;
    protected XpathEngine WFS20_XPATH_ENGINE;

    @Before
    public void beforeTest() {
        // check that the test should run
        File fixtureFile = getFixtureFile();
        assumeTrue(fixtureFile.exists());
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

    // return the paths to the mappings that should be used
    protected abstract String getPathOfMappingsToUse();

    @AfterClass
    public static void tearDown() throws Exception {
        // remove the temporary directory
        if (ROOT_DIRECTORY != null) {
            IOUtils.delete(ROOT_DIRECTORY.toFile(), true);
        }
        // remove test data base from MongoDB
        try {
            MONGO_CLIENT.getDatabase(STATIONS_DATA_BASE_NAME).drop();
        } catch (Exception exception) {
            // ignore any error just log it
            LOGGER.log(Level.WARNING, "Error removing test database from MongoDB.", exception);
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // create a cache directory for schema resolutions if it doesn't exists
        File cache = new File(ROOT_DIRECTORY.toFile(), "app-schema-cache");
        if (cache.mkdir()) {
            // cache directory created
            LOGGER.log(
                    Level.INFO,
                    String.format(
                            "App-Schema schemas resolutions cache directory '%s' created.",
                            cache.getAbsolutePath()));
        }
        // setup stations data set mappings an auxiliary files
        setupStationsMappings();
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        // create necessary stations workspace
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
        params.put("url", "file:" + APP_SCHEMA_MAPPINGS.getAbsolutePath());
        DataStoreInfoImpl dataStore = new DataStoreInfoImpl(getCatalog());
        dataStore.setName(STATIONS_STORE_NAME);
        dataStore.setType("app-schema");
        dataStore.setConnectionParameters(params);
        dataStore.setWorkspace(workspace);
        dataStore.setEnabled(true);
        catalog.add(dataStore);
        // add the stations style and set it as the default one for stations layer
        testData.addStyle("stations", "stations.sld", ComplexMongoDBSupport.class, catalog);
        testData.addStyle(
                "stations_with_sort_by_desc",
                "stations_with_sort_by_desc.sld",
                ComplexMongoDBSupport.class,
                getCatalog());
        testData.addStyle(
                "stations_with_sort_by_asc",
                "stations_with_sort_by_asc.sld",
                ComplexMongoDBSupport.class,
                getCatalog());
        // build the feature type for the root mapping (StationFeature)
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(dataStore);
        builder.setWorkspace(workspace);
        FeatureTypeInfo featureType =
                builder.buildFeatureType(new NameImpl(nameSpace.getURI(), "StationFeature"));
        catalog.add(featureType);
        LayerInfo layer = builder.buildLayer(featureType);
        layer.setDefaultStyle(catalog.getStyleByName("stations"));
        catalog.add(layer);
    }

    @Test
    public void testGetStationFeatures() throws Exception {
        Document document =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st:StationFeature");
        // assert that the response contains station 1 measurements
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 1",
                "station1@mail.com",
                "wind",
                "km/h",
                "1482146833",
                "155.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 1",
                "station1@mail.com",
                "temp",
                "c",
                "1482146800",
                "20.0");
        // assert that the response contains station 2 measurements
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "pression",
                "pa",
                "1482147051",
                "1015.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "pression",
                "pa",
                "1482147026",
                "1019.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "wind",
                "km/h",
                "1482146964",
                "80.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "temp",
                "c",
                "1482146911",
                "35.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "temp",
                "c",
                "1482146935",
                "25.0");
        checkMeasurementNotExists(WFS11_XPATH_ENGINE, document, "station 3", "station3@mail.com");
    }

    @Test
    public void testGetStationFeaturesWithFilter() throws Exception {
        String postContent = readResourceContent("/querys/postQuery1.xml");
        Document document =
                postAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=st:StationFeature",
                        postContent);
        // assert that the response contains station 2 measurements
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "pression",
                "pa",
                "1482147051",
                "1015.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "pression",
                "pa",
                "1482147026",
                "1019.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "wind",
                "km/h",
                "1482146964",
                "80.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "temp",
                "c",
                "1482146911",
                "35.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "temp",
                "c",
                "1482146935",
                "25.0");
    }

    @Test
    public void testStationsWmsGetMap() throws Exception {
        // execute the WMS GetMap request
        MockHttpServletResponse result =
                getAsServletResponse(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES&LAYERS=st:StationFeature"
                                + "&SRS=EPSG:4326&WIDTH=349&HEIGHT=768"
                                + "&BBOX=96.251220703125,-57.81005859375,103.919677734375,-40.93505859375");
        assertThat(result.getStatus(), is(200));
        assertThat(result.getContentType(), is("image/png"));
        // check that we got the expected image back
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(getBinary(result)));
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("/results/result1.png")), image, 240);
    }

    @Test
    public void testStationsWmsGetFeatureInfo() throws Exception {
        // execute the WMS GetFeatureInfo request
        Document document =
                getAsDOM(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetFeatureInfo&FORMAT=image/png&TRANSPARENT=true&QUERY_LAYERS=st:StationFeature"
                                + "&STYLES&LAYERS=st:StationFeature&INFO_FORMAT=text/xml; subtype=gml/3.1.1"
                                + "&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG:4326&WIDTH=101&HEIGHT=101"
                                + "&BBOX=91.23046875,-58.623046874999986,108.984375,-40.869140624999986");
        // assert that the response contains station 2 measurements
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "pression",
                "pa",
                "1482147051",
                "1015.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "pression",
                "pa",
                "1482147026",
                "1019.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "wind",
                "km/h",
                "1482146964",
                "80.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "temp",
                "c",
                "1482146911",
                "35.0");
        checkMeasurementExists(
                WFS11_XPATH_ENGINE,
                document,
                "station 2",
                "station2@mail.com",
                "temp",
                "c",
                "1482146935",
                "25.0");
    }

    /**
     * Helper method that evaluates a xpath and checks if the number of nodes found correspond to
     * the expected number,
     */
    protected void checkCount(
            XpathEngine xpathEngine, Document document, int expectedCount, String xpath) {
        try {
            // evaluate the xpath and compare the number of nodes found
            MatcherAssert.assertThat(
                    xpathEngine.getMatchingNodes(xpath, document).getLength(), is(expectedCount));
        } catch (Exception exception) {
            throw new RuntimeException("Error evaluating xpath.", exception);
        }
    }

    /** Helper method that setup stations data set mappings files and schemas. */
    private void setupStationsMappings() throws Exception {
        // check that a fixture file was provided
        File fixtureFile = getFixtureFile();
        if (!fixtureFile.exists()) {
            // create fixture example file
            createFixtureExample(fixtureFile);
            LOGGER.warning(
                    String.format(
                            "No fixture file '%s' for MongoDB exists, example file created. Tests will eb skipped.",
                            fixtureFile.getAbsolutePath()));
        }
        assumeTrue(fixtureFile.exists());
        // load MongoDB connection properties from fixture file
        Properties properties = loadFixtureProperties(fixtureFile);
        // check that we have access to a mongodb and instantiate the client
        String hostAsString = properties.getProperty("mongo.host", "127.0.0.1");
        String portAsString = properties.getProperty("mongo.port", "27017");
        ServerAddress serverAddress =
                new ServerAddress(hostAsString, Integer.parseInt(portAsString));
        MONGO_CLIENT =
                new MongoClient(
                        serverAddress,
                        new MongoClientOptions.Builder().serverSelectionTimeout(2000).build());
        try {
            MONGO_CLIENT.listDatabaseNames().first();
        } catch (Exception exception) {
            // not able to connect to the MongoDB database
            throw new RuntimeException(
                    String.format(
                            "Could not connect to MongoDB database with host '%s' and port '%s'.",
                            hostAsString, portAsString));
        }
        // moving schemas files to the test directory
        moveResourceToTempDir("/schemas/stations.xsd", "stations.xsd");
        // copy the mappings file and do some substitutions
        APP_SCHEMA_MAPPINGS = moveResourceToTempDir(getPathOfMappingsToUse(), "stations.xml");
        String mappingsContent = new String(Files.readAllBytes(APP_SCHEMA_MAPPINGS.toPath()));
        mappingsContent = mappingsContent.replaceAll("\\{dataBaseName\\}", STATIONS_DATA_BASE_NAME);
        mappingsContent =
                mappingsContent.replaceAll("\\{collectionName\\}", STATIONS_COLLECTION_NAME);
        mappingsContent = mappingsContent.replaceAll("\\{mongoHost\\}", hostAsString);
        mappingsContent = mappingsContent.replaceAll("\\{mongoPort\\}", portAsString);
        mappingsContent =
                mappingsContent.replaceAll(
                        "\\{schemaStore\\}",
                        new File(ROOT_DIRECTORY.toFile(), "schema-store").getAbsolutePath());
        Files.write(APP_SCHEMA_MAPPINGS.toPath(), mappingsContent.getBytes());
        // insert stations data set in MongoDB
        File stationsFile1 = moveResourceToTempDir("/data/stations1.json", "stations1.json");
        File stationsFile2 = moveResourceToTempDir("/data/stations2.json", "stations2.json");
        File stationsFile3 = moveResourceToTempDir("/data/stations3.json", "stations3.json");
        File stationsFile4 = moveResourceToTempDir("/data/stations4.json", "stations4.json");
        String stationsContent1 = new String(Files.readAllBytes(stationsFile1.toPath()));
        String stationsContent2 = new String(Files.readAllBytes(stationsFile2.toPath()));
        String stationsContent3 = new String(Files.readAllBytes(stationsFile3.toPath()));
        String stationsContent4 = new String(Files.readAllBytes(stationsFile4.toPath()));
        insertJson(STATIONS_DATA_BASE_NAME, STATIONS_COLLECTION_NAME, stationsContent1);
        insertJson(STATIONS_DATA_BASE_NAME, STATIONS_COLLECTION_NAME, stationsContent2);
        insertJson(STATIONS_DATA_BASE_NAME, STATIONS_COLLECTION_NAME, stationsContent3);
        insertJson(STATIONS_DATA_BASE_NAME, STATIONS_COLLECTION_NAME, stationsContent4);
    }

    /** Load MongoDB connection properties. */
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

    /** Write fixture example file for MongoDB, if the file already exists nothing will be done. */
    private static void createFixtureExample(File fixtureFile) {
        // example fixture file
        File exampleFixtureFile = new File(fixtureFile.getAbsolutePath() + ".example");
        if (exampleFixtureFile.exists()) {
            // file already exists
            return;
        }
        // default MongoDB connection parameters
        Properties properties = new Properties();
        properties.put("mongo.host", "127.0.0.1");
        properties.put("mongo.port", "27017");
        try (OutputStream output = new FileOutputStream(exampleFixtureFile)) {
            properties.store(
                    output,
                    "This is an example fixture. Update the values "
                            + "and remove the .example suffix to enable the test");
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error writing example fixture file '%s'.",
                            fixtureFile.getAbsolutePath()),
                    exception);
        }
    }

    /** Gets the fixture file for MongoDB, parent directories are created if needed. */
    private static File getFixtureFile() {
        File directory = new File(System.getProperty("user.home") + File.separator + ".geoserver");
        if (!directory.exists()) {
            // make sure parent directory exists
            directory.mkdir();
        }
        return new File(directory, "mongodb.properties");
    }

    /** Helper method that reads the content of a resource to a string. */
    private static String readResourceContent(String resourcePath) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = ComplexMongoDBSupport.class.getResourceAsStream(resourcePath)) {
            IOUtils.copy(input, output);
            return new String(output.toByteArray());
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Error reading resource '%s' content.", resourcePath), exception);
        }
    }

    /** Helper method that creates a temporary directory taking care of the IO exception. */
    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("app-schema-mongo");
        } catch (Exception exception) {
            throw new RuntimeException("Error creating temporary directory.", exception);
        }
    }

    /**
     * Helper method that moves a resource to the tests temporary directory and return the resource
     * file path.
     */
    private static File moveResourceToTempDir(String resourcePath, String resourceName) {
        // create the output file
        File outputFile = new File(ROOT_DIRECTORY.toFile(), resourceName);
        try (InputStream input = ComplexMongoDBSupport.class.getResourceAsStream(resourcePath);
                OutputStream output = new FileOutputStream(outputFile)) {
            // copy the resource content to the output file
            IOUtils.copy(input, output);
        } catch (Exception exception) {
            throw new RuntimeException("Error moving resource to temporary directory.", exception);
        }
        return outputFile;
    }

    /**
     * Helper method that reads a JSON object from a file and inserts it in the provided database
     * and collection.
     */
    private static void insertJson(String databaseName, String collectionName, String json) {
        // insert stations data
        org.bson.Document document = org.bson.Document.parse(json);
        MONGO_CLIENT.getDatabase(databaseName).getCollection(collectionName).insertOne(document);
        // add / update geometry index
        BasicDBObject indexObject = new BasicDBObject();
        indexObject.put("geometry", "2dsphere");
        MONGO_CLIENT
                .getDatabase(databaseName)
                .getCollection(collectionName)
                .createIndex(indexObject);
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
     * Helper method that checks that the provided XML document contains the correct stations and
     * associated measurement.
     */
    private void checkMeasurementExists(
            XpathEngine xpathEngine,
            Document document,
            String stationName,
            String stationMail,
            String measurementName,
            String measurementUnit,
            String measurementTimestamp,
            String measurementValue) {
        // check that the station metadata is correct
        checkCount(
                xpathEngine,
                document,
                1,
                String.format(
                        "/wfs:FeatureCollection/gml:featureMembers"
                                + "/st:StationFeature[st:name='%s']/st:contact[st:mail='%s']",
                        stationName, stationMail));
        // check that the measurement is present in the XML document
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                String.format(
                        "/wfs:FeatureCollection/gml:featureMembers"
                                + "/st:StationFeature[st:name='%s']/st:measurement/st:Measurement[st:name='%s'][st:unit='%s']"
                                + "/st:values/st:Value[st:timestamp='%s'][st:value='%s']",
                        stationName,
                        measurementName,
                        measurementUnit,
                        measurementTimestamp,
                        measurementValue));
    }

    private void checkMeasurementNotExists(
            XpathEngine xpathEngine, Document document, String stationName, String stationMail) {
        // check that the station metadata is correct
        checkCount(
                xpathEngine,
                document,
                1,
                String.format(
                        "/wfs:FeatureCollection/gml:featureMembers"
                                + "/st:StationFeature[st:name='%s']/st:contact[st:mail='%s']",
                        stationName, stationMail));
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                0,
                String.format(
                        "/wfs:FeatureCollection/gml:featureMembers"
                                + "/st:StationFeature[st:name='%s']/st:measurement",
                        stationName));
    }

    @Test
    public void testStationsWmsGetMapWithSortByDesc() throws Exception {

        LayerInfo layer = getCatalog().getLayerByName("st:StationFeature");
        StyleInfo sort = getCatalog().getStyleByName("stations_with_sort_by_desc");
        StyleInfo defaultStyle = getCatalog().getStyleByName("stations");
        layer.setDefaultStyle(sort);
        getCatalog().save(layer);
        // execute the WMS GetMap request
        MockHttpServletResponse result =
                getAsServletResponse(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES&LAYERS=st:StationFeature"
                                + "&SRS=EPSG:4326&WIDTH=349&HEIGHT=768"
                                + "&BBOX=96.251220703125,-57.81005859375,103.919677734375,-40.93505859375");
        assertThat(result.getStatus(), is(200));
        assertThat(result.getContentType(), is("image/png"));
        // check that we got the expected image back
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(getBinary(result)));
        // use same image as test asc to check for equality since in the desc sld
        // the two symbolizers have been shifted with respect to the asc sld
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("/results/sorted_result.png")), image, 240);
        layer.setDefaultStyle(defaultStyle);
        getCatalog().save(layer);
    }

    @Test
    public void testStationsWmsGetMapWithSortByAsc() throws Exception {

        LayerInfo layer = getCatalog().getLayerByName("st:StationFeature");
        StyleInfo sort = getCatalog().getStyleByName("stations_with_sort_by_asc");
        StyleInfo defaultStyle = getCatalog().getStyleByName("stations");
        layer.setDefaultStyle(sort);
        getCatalog().save(layer);
        // execute the WMS GetMap request
        MockHttpServletResponse result =
                getAsServletResponse(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES&LAYERS=st:StationFeature"
                                + "&SRS=EPSG:4326&WIDTH=349&HEIGHT=768"
                                + "&BBOX=96.251220703125,-57.81005859375,103.919677734375,-40.93505859375");
        assertThat(result.getStatus(), is(200));
        assertThat(result.getContentType(), is("image/png"));
        // check that we got the expected image back
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(getBinary(result)));
        // use same image as test desc to check for equality since in the asc sld
        // the two symbolizers have been shifted with respect to the desc sld
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("/results/sorted_result.png")), image, 240);
        layer.setDefaultStyle(defaultStyle);
        getCatalog().save(layer);
    }
}
