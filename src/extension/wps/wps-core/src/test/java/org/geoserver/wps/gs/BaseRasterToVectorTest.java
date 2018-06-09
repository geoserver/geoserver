/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Utilities;

public abstract class BaseRasterToVectorTest extends WPSTestSupport {

    static final double EPS = 1e-6;
    public static QName RESTRICTED = new QName(MockData.SF_URI, "restricted", MockData.SF_PREFIX);
    public static QName DEM = new QName(MockData.SF_URI, "sfdem", MockData.SF_PREFIX);
    public static QName TASMANIA_BM_ZONES =
            new QName(MockData.SF_URI, "BmZones", MockData.SF_PREFIX);

    public BaseRasterToVectorTest() {
        super();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);
        testData.addRasterLayer(DEM, "sfdem.tiff", TIFF, null, getClass(), getCatalog());

        Map<LayerProperty, Object> props = new HashMap<SystemTestData.LayerProperty, Object>();
        props.put(
                LayerProperty.ENVELOPE,
                new ReferencedEnvelope(
                        181985.7630,
                        818014.2370,
                        1973809.4640,
                        8894102.4298,
                        CRS.decode("EPSG:26713", true)));

        testData.addVectorLayer(
                RESTRICTED, props, "restricted.properties", getClass(), getCatalog());
        testData.addVectorLayer(
                TASMANIA_BM_ZONES, props, "tazdem_zones.properties", getClass(), getCatalog());
    }

    /**
     * This method takes the input {@link SimpleFeatureCollection} and transforms it into a
     * shapefile using the provided file.
     *
     * <p>Make sure the provided files ends with .shp.
     *
     * @param fc the {@link SimpleFeatureCollection} to be encoded as a shapefile.
     * @param destination the {@link File} where we want to write the shapefile.
     * @throws IOException in case an {@link IOException} is thrown by the underlying code.
     */
    protected static void featureCollectionToShapeFile(
            final SimpleFeatureCollection fc, final File destination) throws IOException {

        //
        // checks
        //
        org.geotools.util.Utilities.ensureNonNull("fc", fc);
        Utilities.ensureNonNull("destination", destination);
        // checks on the file
        if (destination.exists()) {

            if (destination.isDirectory())
                throw new IOException(
                        "The provided destination maps to a directory:" + destination);

            if (!destination.canWrite())
                throw new IOException(
                        "The provided destination maps to an existing file that cannot be deleted:"
                                + destination);

            if (!destination.delete())
                throw new IOException(
                        "The provided destination maps to an existing file that cannot be deleted:"
                                + destination);
        }

        // real work
        final DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", destination.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore store = null;
        Transaction transaction = null;
        try {
            store = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            store.createSchema(fc.getSchema());

            final SimpleFeatureStore featureStore =
                    (SimpleFeatureStore) store.getFeatureSource(fc.getSchema().getName());
            transaction = featureStore.getTransaction();

            featureStore.addFeatures(fc);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (transaction != null) {

                transaction.commit();
                transaction.close();
            }

            if (store != null) {
                store.dispose();
            }
        }
    }
}
