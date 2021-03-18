package org.geoserver.schemalessfeatures.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import java.io.File;
import java.nio.file.Files;

@SuppressWarnings({
    "PMD.JUnit4TestShouldUseAfterAnnotation",
    "PMD.JUnit4TestShouldUseBeforeAnnotation"
})
public class StationsTestSetup extends MongoTestSetup {

    public static final String COLLECTION_NAME = "geoJSONStations";
    private static final String TEST_DATA_DIR = "test-data/stations";
    private String dataBaseName;

    public StationsTestSetup(String dataBaseName) {
        super();
        this.dataBaseName = dataBaseName;
    }

    @Override
    protected void setUpData() throws Exception {
        String connectionString = fixture.getProperty("mongo.connectionString");
        ConnectionString connectionStringBO = new ConnectionString(connectionString);
        client = MongoClients.create(connectionStringBO);
        database = client.getDatabase(dataBaseName);
        if (database.getCollection(COLLECTION_NAME).countDocuments() == 0) {
            this.collection = database.getCollection(COLLECTION_NAME);
            addGeometryIndex();
            File resource = new File(getClass().getResource(TEST_DATA_DIR + "/json").toURI());
            File[] stations = resource.listFiles();
            for (File f : stations) {
                String stationsContent = new String(Files.readAllBytes(f.toPath()));
                insertJson(stationsContent);
            }
        }
    }

    @Override
    public void tearDown() {
        database.drop();
        client.close();
    }
}
