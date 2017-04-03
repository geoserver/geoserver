/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geotools.feature.NameImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeTrue;

/**
 * Tests the integration between MongoDB and App-schema. This test are integration tests hence they
 * require a MongoDB instance. If no fixture file for MongoDB exists these tests will be skipped.
 */
public final class ComplexMongoDBTest extends GeoServerSystemTestSupport {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(ComplexMongoDBTest.class);

    private static final Path ROOT_DIRECTORY = createTempDir();

    private static File APP_SCHEMA_MAPPINGS;

    private static final String STATIONS_DATA_BASE_NAME = UUID.randomUUID().toString();
    private static final String STATIONS_COLLECTION_NAME = "stations";

    private static MongoClient MONGO_CLIENT;

    @BeforeClass
    public static void setup() throws Exception {
        // check that a fixture file was provided
        File fixtureFile = getFixtureFile();
        if (!fixtureFile.exists()) {
            // create fixture example file
            createFixtureExample(fixtureFile);
            LOGGER.warning(String.format(
                    "No fixture file '%s' for MongoDB exists, example file created. Tests will eb skipped.",
                    fixtureFile.getAbsolutePath()));
        }
        assumeTrue(fixtureFile.exists());
        // load MongoDB connection properties from fixture file
        Properties properties = loadFixtureProperties(fixtureFile);
        // check that we have access to a mongodb and instantiate the client
        String hostAsString = properties.getProperty("mongo.host", "127.0.0.1");
        String portAsString = properties.getProperty("mongo.port", "27017");
        ServerAddress serverAddress = new ServerAddress(hostAsString, Integer.parseInt(portAsString));
        MONGO_CLIENT = new MongoClient(serverAddress,
                new MongoClientOptions.Builder().serverSelectionTimeout(2000).build());
        try {
            MONGO_CLIENT.listDatabaseNames().first();
        } catch (Exception exception) {
            // not able to connect to the MongoDB database
            throw new RuntimeException(String.format(
                    "Could not connect to MongoDB database with host '%s' and port '%s'.",
                    hostAsString, portAsString));
        }
        // moving schemas files to the test directory
        moveResourceToTempDir("/schemas/stations.xsd", "stations.xsd");
        // copy the mappings file and do some substitutions
        APP_SCHEMA_MAPPINGS = moveResourceToTempDir("/mappings/stations.xml", "stations.xml");
        String mappingsContent = new String(Files.readAllBytes(APP_SCHEMA_MAPPINGS.toPath()));
        mappingsContent = mappingsContent.replaceAll("\\{dataBaseName\\}", STATIONS_DATA_BASE_NAME);
        mappingsContent = mappingsContent.replaceAll("\\{collectionName\\}", STATIONS_COLLECTION_NAME);
        mappingsContent = mappingsContent.replaceAll("\\{mongoHost\\}", hostAsString);
        mappingsContent = mappingsContent.replaceAll("\\{mongoPort\\}", portAsString);
        mappingsContent = mappingsContent.replaceAll("\\{schemaStore\\}",
                new File(ROOT_DIRECTORY.toFile(), "schema-store").getAbsolutePath());
        Files.write(APP_SCHEMA_MAPPINGS.toPath(), mappingsContent.getBytes());
        // insert stations data set in MongoDB
        File stationsFile1 = moveResourceToTempDir("/data/stations1.json", "stations1.json");
        File stationsFile2 = moveResourceToTempDir("/data/stations2.json", "stations2.json");
        String stationsContent1 = new String(Files.readAllBytes(stationsFile1.toPath()));
        String stationsContent2 = new String(Files.readAllBytes(stationsFile2.toPath()));
        insertJson(STATIONS_DATA_BASE_NAME, STATIONS_COLLECTION_NAME, stationsContent1);
        insertJson(STATIONS_DATA_BASE_NAME, STATIONS_COLLECTION_NAME, stationsContent2);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // remove the temporary directory
        if (ROOT_DIRECTORY != null) {
            IOUtils.delete(ROOT_DIRECTORY.toFile());
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
        dataStore.setName(UUID.randomUUID().toString());
        dataStore.setType("app-schema");
        dataStore.setConnectionParameters(params);
        dataStore.setWorkspace(workspace);
        dataStore.setEnabled(true);
        catalog.add(dataStore);
        // build the feature type for the root mapping (StationFeature)
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(dataStore);
        builder.setWorkspace(workspace);
        FeatureTypeInfo featureType = builder.buildFeatureType(
                new NameImpl(nameSpace.getURI(), "StationFeature"));
        catalog.add(featureType);
        LayerInfo layer = builder.buildLayer(featureType);
        catalog.add(layer);
    }

    @Test
    public void testGetStationFeatures() throws Exception {
        Document result = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st:StationFeature");
        checkResult(result, "/results/result1.xml");
    }

    @Test
    public void testGetStationFeaturesWithFilter() throws Exception {
        String postContent = readResourceContent("/querys/postQuery1.xml");
        Document result = postAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st:StationFeature", postContent);
        checkResult(result, "/results/result2.xml");
    }

    /**
     * Load MongoDB connection properties.
     */
    private static Properties loadFixtureProperties(File fixtureFile) {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(fixtureFile)) {
            // load properties from fixture file
            properties.load(input);
            return properties;
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error reading fixture file '%s'.",
                    fixtureFile.getAbsolutePath()), exception);
        }
    }

    /**
     * Write fixture example file for MongoDB, if the file already
     * exists nothing will be done.
     */
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
            properties.store(output, "This is an example fixture. Update the values " +
                    "and remove the .example suffix to enable the test");
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error writing example fixture file '%s'.",
                    fixtureFile.getAbsolutePath()), exception);
        }
    }

    /**
     * Gets the fixture file for MongoDB, parent directories are created if needed.
     */
    private static File getFixtureFile() {
        File directory = new File(System.getProperty("user.home") + File.separator + ".geoserver");
        if (!directory.exists()) {
            // make sure parent directory exists
            directory.mkdir();
        }
        return new File(directory, "mongodb.properties");
    }

    /**
     * Helper method that reads the content of a resource to a string.
     */
    private static String readResourceContent(String resourcePath) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = ComplexMongoDBTest.class.getResourceAsStream(resourcePath)) {
            IOUtils.copy(input, output);
            return new String(output.toByteArray());
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error reading resource '%s' content.",
                    resourcePath), exception);
        }
    }

    /**
     * Helper method that will check the returned document against a
     * control document. The control document will be parsed from the
     * provided resource.
     */
    private void checkResult(Document result, String resultResourcePath) throws Exception {
        // parse the expected result document
        Document expected = dom(ComplexMongoDBTest.class.getResourceAsStream(resultResourcePath), true);
        Diff diff = new Diff(expected, result);
        // elements don't need to be in the same order
        diff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());
        // ignore timestamps and schema locations differences
        diff.overrideDifferenceListener(new AppSchemaDifferenceListener());

        Assert.assertThat(diff.similar(), is(true));
    }

    /**
     * Helper method that creates a temporary directory taking
     * care of the IO exception.
     */
    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("app-schema-mongo");
        } catch (Exception exception) {
            throw new RuntimeException("Error creating temporary directory.", exception);
        }
    }

    /**
     * Helper method that moves a resource to the tests temporary directory
     * and return the resource file path.
     */
    private static File moveResourceToTempDir(String resourcePath, String resourceName) {
        // create the output file
        File outputFile = new File(ROOT_DIRECTORY.toFile(), resourceName);
        try (InputStream input = ComplexMongoDBTest.class.getResourceAsStream(resourcePath);
             OutputStream output = new FileOutputStream(outputFile)) {
            // copy the resource content to the output file
            IOUtils.copy(input, output);
        } catch (Exception exception) {
            throw new RuntimeException("Error moving resource to temporary directory.", exception);
        }
        return outputFile;
    }

    /**
     * Helper method that reads a JSON object from a file and inserts it in
     * the provided database and collection.
     */
    private static void insertJson(String databaseName, String collectionName, String json) {
        // insert stations data
        org.bson.Document document = org.bson.Document.parse(json);
        MONGO_CLIENT.getDatabase(databaseName).getCollection(collectionName).insertOne(document);
        // add / update geometry index
        BasicDBObject indexObject = new BasicDBObject();
        indexObject.put("geometry", "2dsphere");
        MONGO_CLIENT.getDatabase(databaseName).getCollection(collectionName).createIndex(indexObject);
    }

    /**
     * Helper listener for ignoring differences related with timestamp
     * values and schema locations.
     */
    private static final class AppSchemaDifferenceListener implements DifferenceListener {

        @Override
        public int differenceFound(Difference difference) {
            String controlNode = difference.getControlNodeDetail().getNode().getLocalName();
            String testNode = difference.getTestNodeDetail().getNode().getLocalName();
            if (controlNode != null && controlNode.equals(testNode) && (
                    controlNode.equalsIgnoreCase("timestamp") ||
                            controlNode.equalsIgnoreCase("schemaLocation"))) {
                // ignore this difference
                return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
            }
            // valid difference, the engine may try to match a node in a different order
            return RETURN_ACCEPT_DIFFERENCE;
        }

        @Override
        public void skippedComparison(Node node, Node node1) {
        }
    }
}
