package com.boundlessgeo.gsr;

import com.vividsolutions.jts.geom.Point;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
//import org.locationtech.jts.geom.Coordinate;
//import org.locationtech.jts.geom.GeometryFactory;
//import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DynamicTableTest extends GeoServerSystemTestSupport {
    Catalog cat;

    @Before
    public void getCat(){
        cat = getCatalog();
    }

    private static SimpleFeatureType createFeatureType() {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Location");
        builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

        // add attributes in order
        builder.add("the_geom", Point.class);
        builder.length(15).add("Name", String.class); // <- 15 chars width for name field
        builder.add("number", Integer.class);

        // build the type
        final SimpleFeatureType LOCATION = builder.buildFeatureType();

        return LOCATION;
    }

    private static SimpleFeatureType newFeatureType() {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Location");
        builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

        // add attributes in order
        builder.add("the_geom", Point.class);
        builder.length(15).add("Name", String.class); // <- 15 chars width for name field
        builder.add("number", Integer.class);

        // build the type
        final SimpleFeatureType LOCATION = builder.buildFeatureType();

        return LOCATION;
    }


    public DataStore createH2DataStore(String wsName, String dsName) throws IOException {
        /*
         * We use the DataUtilities class to create a FeatureType that will describe the data in our
         * shapefile.
         *
         * See also the createFeatureType method below for another, more flexible approach.
         */
        SimpleFeatureType type = createFeatureType();
        System.out.println("TYPE:" + type);

        /*
         * A list to collect features as we create them.
         */
        List<SimpleFeature> features = new ArrayList<>();

        /*
         * GeometryFactory will be used to create the geometry attribute of each feature,
         * using a Point object for the location.
         */
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

        /*
         * test data
         * LAT        LON        NAME    NUMBER
         * 46.066667, 11.116667, Trento, 140
         */
        org.locationtech.jts.geom.Point point1 = geometryFactory.createPoint((new Coordinate(46, 11)));

        // Build feature
        featureBuilder.add(point1);
        featureBuilder.add("Trento");
        featureBuilder.add(140);
        SimpleFeature feature = featureBuilder.buildFeature(null);
        features.add(feature);

        WorkspaceInfo ws =
                wsName != null ? cat.getWorkspaceByName(wsName) : cat.getDefaultWorkspace();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setWorkspace(ws);
        ds.setName(dsName);
        ds.setType("H2");

        Map params = new HashMap();
        params.put("database", getTestData().getDataDirectoryRoot().getPath() + "/" + dsName);
        params.put("dbtype", "h2");
        ds.getConnectionParameters().putAll(params);
        ds.setEnabled(true);
        cat.add(ds);

        DataStore da = (DataStore) ds.getDataStore(null);

        da.createSchema(type);

        /*
         * Write the features to the datastore
         */
        Transaction transaction = new DefaultTransaction("create");

        String typeName = da.getTypeNames()[0];
        SimpleFeatureSource featureSource = da.getFeatureSource(typeName);
        SimpleFeatureType GEOM_TYPE = featureSource.getSchema();

        System.out.println("SHAPE:" + GEOM_TYPE);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            /*
             * SimpleFeatureStore has a method to add features from a
             * SimpleFeatureCollection object, so we use the ListFeatureCollection
             * class to wrap our list of features.
             */
            SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
            featureStore.setTransaction(transaction);

            try {
                featureStore.addFeatures(collection);
                transaction.commit();

            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
        } else {
            System.out.println(typeName + " does not support read/write access");
        }
        return da;
    }

    @Test
    public void testUpdateCacheClear() throws IOException {
        System.out.println();
        //create data store info
        DataStore ds = createH2DataStore("gs", "test");

        assertNotNull(cat.getDataStores().get(4).getDataStore(null));

        FeatureSource ft = cat.getFeatureType("Location");

        System.out.println("FTI = " + ft);

        ds.getFeatureSource("Location");

        FeatureSource updatedFs = updateFeature();

        assertEquals(cat.getFeatureSource("Location"), fs);

        cat.getResourcePool().getFeatureTypeAttributeCache().get("Location").clear();

        assertEquals((cat.getFeatureSource("Location"), updatedFs));

        System.out.println(ds);
    }
}
