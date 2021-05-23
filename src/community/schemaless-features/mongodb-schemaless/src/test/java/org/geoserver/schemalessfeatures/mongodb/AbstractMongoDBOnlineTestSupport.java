package org.geoserver.schemalessfeatures.mongodb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.schemalessfeatures.mongodb.data.MongoSchemalessDataStoreFactory;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.test.FixtureUtilities;
import org.junit.Before;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public abstract class AbstractMongoDBOnlineTestSupport extends GeoServerSystemTestSupport {

    protected Properties fixture;

    private String fixtureId = "schemaless-mongo";

    protected static final String DB_PREFIX = "mock";

    protected String databaseName;

    public AbstractMongoDBOnlineTestSupport() {
        this.databaseName = DB_PREFIX + getClass().getSimpleName();
    }

    @Before
    public void setUp() throws Exception {
        fixture = getFixture();
        MongoTestSetup testSetup = createTestSetups();
        testSetup.setFixture(fixture);
        testSetup.setUp();
    }

    protected Properties getFixture() {
        File fixtureFile = FixtureUtilities.getFixtureFile(getFixtureDirectory(), fixtureId);
        if (fixtureFile.exists()) {
            return FixtureUtilities.loadProperties(fixtureFile);
        } else {
            Properties exampleFixture = createExampleFixture();
            if (exampleFixture != null) {
                File exFixtureFile = new File(fixtureFile.getAbsolutePath() + ".example");
                if (!exFixtureFile.exists()) {
                    createExampleFixture(exFixtureFile, exampleFixture);
                }
            }
            FixtureUtilities.printSkipNotice(fixtureId, fixtureFile);
            return null;
        }
    }

    private void createExampleFixture(File exFixtureFile, Properties exampleFixture) {
        FileOutputStream fout = null;
        try {
            exFixtureFile.getParentFile().mkdirs();
            exFixtureFile.createNewFile();

            fout = new FileOutputStream(exFixtureFile);

            exampleFixture.store(
                    fout,
                    "This is an example fixture. Update the "
                            + "values and remove the .example suffix to enable the test");
            fout.flush();
            fout.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                }
            }
        }
    }

    File getFixtureDirectory() {
        return new File(System.getProperty("user.home") + File.separator + ".geoserver");
    }

    Properties createExampleFixture() {
        Properties fixture = new Properties();

        fixture.put(
                "mongo.connectionString",
                "examples= mongodb://localhost:27017, mongodb://username:password@localhost:27017");

        return fixture;
    }

    protected abstract MongoTestSetup createTestSetups();

    protected DataStoreInfo addMongoSchemalessStore(WorkspaceInfo ws, String storeName) {
        Catalog catalog = getCatalog();
        DataStoreInfo store = catalog.getDataStoreByName(storeName);
        if (store == null) {

            store = catalog.getFactory().createDataStore();
            store.setName(storeName);
            store.setWorkspace(ws);
            store.setEnabled(true);
            String connectionString =
                    fixture.getProperty("mongo.connectionString") + "/" + databaseName;
            NamespaceInfo namespace = catalog.getNamespaceByPrefix(ws.getName());
            store.getConnectionParameters()
                    .put(MongoSchemalessDataStoreFactory.CONNECTION_STRING.key, connectionString);
            store.getConnectionParameters()
                    .put(MongoSchemalessDataStoreFactory.NAMESPACE.key, namespace.getURI());
            store.getConnectionParameters().put("dbtype", "MongoDB Schemaless");
            store.setType(new MongoSchemalessDataStoreFactory().getDisplayName());
            catalog.add(store);
        }
        return catalog.getDataStoreByName(ws.getName(), storeName);
    }

    protected void addMongoSchemalessLayer(WorkspaceInfo ws, DataStoreInfo store, String typeName)
            throws IOException {
        Catalog catalog = getCatalog();
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace(ws);
        builder.setStore(store);
        DataAccess dataAccess = store.getDataStore(null);
        Map<String, FeatureTypeInfo> featureTypesByNativeName = new HashMap<>();
        @SuppressWarnings("unchecked")
        FeatureSource fs =
                ((DataAccess<FeatureType, Feature>) dataAccess)
                        .getFeatureSource(new NameImpl(ws.getName(), typeName));
        FeatureTypeInfo ftinfo = featureTypesByNativeName.get(typeName);
        if (ftinfo == null) {
            ftinfo = builder.buildFeatureType(fs);
            builder.lookupSRS(ftinfo, true);
            builder.setupBounds(ftinfo, fs);
        }

        ReferencedEnvelope bounds = fs.getBounds();
        ftinfo.setNativeBoundingBox(bounds);

        if (ftinfo.getId() == null) {
            catalog.validate(ftinfo, true).throwIfInvalid();
            catalog.add(ftinfo);
        }

        LayerInfo layer = builder.buildLayer(ftinfo);

        boolean valid = true;
        try {
            if (!catalog.validate(layer, true).isValid()) {
                valid = false;
            }
        } catch (Exception e) {
            valid = false;
        }

        layer.setEnabled(valid);
        catalog.add(layer);
    }
}
