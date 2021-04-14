package org.geoserver.schemalessfeatures.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.Properties;
import org.bson.Document;

@SuppressWarnings({
    "PMD.JUnit4TestShouldUseAfterAnnotation",
    "PMD.JUnit4TestShouldUseBeforeAnnotation"
})
public abstract class MongoTestSetup {

    protected Properties fixture = null;

    protected MongoClient client;
    protected MongoDatabase database;
    protected MongoCollection<Document> collection;

    public MongoTestSetup() {}

    public void setUp() throws Exception {
        setUpData();
    }

    protected abstract void setUpData() throws Exception;

    public abstract void tearDown();

    public void setFixture(Properties fixture) {
        this.fixture = fixture;
    }

    protected void insertJson(String json) {
        // insert stations data
        org.bson.Document document = org.bson.Document.parse(json);
        collection.insertOne(document);
    }

    protected void addGeometryIndex() {
        // add / update geometry index
        BasicDBObject indexObject = new BasicDBObject();
        indexObject.put("geometry", "2dsphere");
        collection.createIndex(indexObject);
    }
}
