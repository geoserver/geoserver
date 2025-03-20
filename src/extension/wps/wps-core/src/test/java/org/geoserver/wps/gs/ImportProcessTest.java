/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang3.function.Failable;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.threadlocals.ThreadLocalsTransfer;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureIterator;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.SimpleInternationalString;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class ImportProcessTest extends WPSTestSupport {

    @Before
    public void login() {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @After
    public void removeNewLayers() {
        removeLayer(SystemTestData.CITE_PREFIX, "Buildings2");
        removeLayer(SystemTestData.CITE_PREFIX, "Buildings4");
        removeLayer(SystemTestData.CITE_PREFIX, "Buildings5");
        removeLayer(SystemTestData.CITE_PREFIX, "Buildings6");
        removeLayer(SystemTestData.CITE_PREFIX, "Buildings7");
        removeLayer(SystemTestData.CITE_PREFIX, "Buildings8");
        removeLayer(SystemTestData.CITE_PREFIX, "BuildingsCancelled");
        removeStore(SystemTestData.CITE_PREFIX, SystemTestData.CITE_PREFIX + "data");
        removeStore(SystemTestData.CITE_PREFIX, SystemTestData.CITE_PREFIX + "raster");
        removeStore(SystemTestData.IAU_PREFIX, SystemTestData.IAU_PREFIX + "marsCoverageStore");
        removeStore(SystemTestData.IAU_PREFIX, SystemTestData.IAU_PREFIX + "marsCoverageStore2");
    }

    /** Try to re-import buildings as another layer (different name, different projection) */
    @Test
    public void testImportBuildings() throws Exception {
        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();
        ForceCoordinateSystemFeatureResults forced =
                new ForceCoordinateSystemFeatureResults(rawSource, CRS.decode("EPSG:4326"));

        ImportProcess importer = new ImportProcess(getCatalog());
        String result = importer.execute(
                forced,
                null,
                SystemTestData.CITE_PREFIX,
                SystemTestData.CITE_PREFIX,
                "Buildings2",
                null,
                null,
                null,
                null);

        checkBuildings(result, "Buildings2");
    }

    @Test
    public void testImportBuildingsProgress() throws Exception {
        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();
        ForceCoordinateSystemFeatureResults forced =
                new ForceCoordinateSystemFeatureResults(rawSource, CRS.decode("EPSG:4326"));

        ImportProcess importer = new ImportProcess(getCatalog());
        DefaultProgressListener testProgressListener = new DefaultProgressListener() {
            float previousProgress = 0;

            @Override
            public void progress(float percent) {
                super.progress(percent);
                assertTrue(percent >= previousProgress);
                previousProgress = percent;
            }
        };
        String result = importer.execute(
                forced,
                null,
                SystemTestData.CITE_PREFIX,
                SystemTestData.CITE_PREFIX,
                "Buildings6",
                null,
                null,
                null,
                testProgressListener);
        assertEquals(100, testProgressListener.getProgress(), 0f);

        checkBuildings(result, "Buildings6");
    }

    @Test
    public void testImportBuildingsCancellation() throws Exception {
        Assume.assumeFalse(System.getProperty("macos-github-build") != null);
        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();
        // a feature collection whose scrolling will block until we relase it via the latch
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleFeatureCollection testFeatureCollection = new DecoratingSimpleFeatureCollection(rawSource) {

            @Override
            public SimpleFeatureIterator features() {
                return new DecoratingSimpleFeatureIterator(super.features()) {
                    @Override
                    public SimpleFeature next() throws NoSuchElementException {
                        Failable.run(() -> latch.await());
                        return super.next();
                    }
                };
            }
        };

        final ImportProcess importer = new ImportProcess(getCatalog());
        final DefaultProgressListener listener = new DefaultProgressListener();
        ThreadLocalsTransfer transfer = new ThreadLocalsTransfer();
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            Future<?> future = executor.submit(() -> {
                transfer.apply();
                try {
                    importer.execute(
                            testFeatureCollection,
                            null,
                            SystemTestData.CITE_PREFIX,
                            SystemTestData.CITE_PREFIX,
                            "BuildingsCancelled",
                            null,
                            null,
                            null,
                            listener);
                } finally {
                    transfer.cleanup();
                }
            });
            // cancel the import
            listener.setTask(new SimpleInternationalString("Test message"));
            listener.setCanceled(true);
            // release the importer
            latch.countDown();

            try {
                future.get();
                fail("Should have failed with an exception");
            } catch (ExecutionException e) {
                assertThat(e.getCause(), instanceOf(ProcessException.class));
                assertEquals("Test message", e.getCause().getMessage());
            }
        } finally {
            executor.shutdown();
        }
    }

    /** Try to re-import buildings as another layer (different name, different projection) */
    @Test
    public void testImportBuildingsForceCRS() throws Exception {
        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();

        ImportProcess importer = new ImportProcess(getCatalog());
        String result = importer.execute(
                rawSource,
                null,
                SystemTestData.CITE_PREFIX,
                SystemTestData.CITE_PREFIX,
                "Buildings3",
                CRS.decode("EPSG:4326"),
                null,
                null,
                null);

        checkBuildings(result, "Buildings3");
    }

    private void checkBuildings(String result, String expected) throws IOException {
        assertEquals(SystemTestData.CITE_PREFIX + ":" + expected, result);

        // check the layer
        LayerInfo layer = getCatalog().getLayerByName(result);
        assertNotNull(layer);
        assertEquals("polygon", layer.getDefaultStyle().getName());

        // check the feature type info
        FeatureTypeInfo fti = (FeatureTypeInfo) layer.getResource();
        assertEquals("EPSG:4326", fti.getSRS());
        SimpleFeatureSource fs = (SimpleFeatureSource) fti.getFeatureSource(null, null);
        assertEquals(2, fs.getCount(Query.ALL));

        // _=the_geom:MultiPolygon,FID:String,ADDRESS:String
        // Buildings.1107531701010=MULTIPOLYGON (((0.0008 0.0005, 0.0008 0.0007,
        // 0.0012 0.0007, 0.0012 0.0005, 0.0008 0.0005)))|113|123 Main Street
        // Buildings.1107531701011=MULTIPOLYGON (((0.002 0.0008, 0.002 0.001,
        // 0.0024 0.001, 0.0024 0.0008, 0.002 0.0008)))|114|215 Main Street

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        SimpleFeature f = DataUtilities.first(fs.getFeatures(ff.equals(ff.property("FID"), ff.literal("113"))));
        assertEquals("113", f.getAttribute("FID"));
        assertEquals("123 Main Street", f.getAttribute("ADDRESS"));

        f = DataUtilities.first(fs.getFeatures(ff.equals(ff.property("FID"), ff.literal("114"))));
        assertEquals("114", f.getAttribute("FID"));
        assertEquals("215 Main Street", f.getAttribute("ADDRESS"));
    }

    /** Test creating a coverage store when a store name is specified but does not exist */
    @Test
    public void testCreateCoverageStore() throws Exception {
        String storeName = SystemTestData.CITE_PREFIX + "raster";
        // use Coverage2RenderedImageAdapterTest's method, just need any sample raster
        GridCoverage2D sampleCoverage = Coverage2RenderedImageAdapterTest.createTestCoverage(
                500, 500, 0, 0, 10, 10, DefaultGeographicCRS.WGS84);
        CoverageStoreInfo storeInfo = catalog.getCoverageStoreByName(storeName);
        assertNull("Store already exists " + storeInfo, storeInfo);
        ImportProcess importer = new ImportProcess(getCatalog());
        String result = importer.execute(
                null,
                sampleCoverage,
                SystemTestData.CITE_PREFIX,
                storeName,
                "Buildings4",
                CRS.decode("EPSG:4326"),
                null,
                null,
                null);
        // expect workspace:layername
        assertEquals(result, SystemTestData.CITE_PREFIX + ":" + "Buildings4");
    }

    @Test
    public void testCreateCoverageStoreIAU() throws Exception {
        testCreateCoverageStoreIAU("marsCoverage", CRS.decode("IAU:49900")); // force CRS
        testCreateCoverageStoreIAU("marsCoverage2", null); // let it use the native CRS
    }

    private void testCreateCoverageStoreIAU(String name, CoordinateReferenceSystem crs) throws FactoryException {
        String storeName = SystemTestData.IAU_PREFIX + name + "Store";
        // use Coverage2RenderedImageAdapterTest's method, just need any sample raster
        CoordinateReferenceSystem marsGeographic = CRS.decode("IAU:49900");
        GridCoverage2D sampleCoverage =
                Coverage2RenderedImageAdapterTest.createTestCoverage(500, 500, 0, 0, 10, 10, marsGeographic);
        CoverageStoreInfo storeInfo = catalog.getCoverageStoreByName(storeName);
        assertNull("Store already exists " + storeInfo, storeInfo);
        ImportProcess importer = new ImportProcess(getCatalog());
        String result = importer.execute(
                null,
                sampleCoverage,
                SystemTestData.IAU_PREFIX,
                storeName,
                name,
                null, // force the CRS this time
                null,
                null,
                null);
        // expect workspace:layername
        assertEquals(result, SystemTestData.IAU_PREFIX + ":" + name);

        CoverageInfo ci = getCatalog().getCoverageByName("iau:" + name);
        assertNotNull(ci);
        assertTrue(CRS.equalsIgnoreMetadata(marsGeographic, ci.getCRS()));
        assertTrue(CRS.equalsIgnoreMetadata(marsGeographic, ci.getNativeCRS()));
    }

    @Test
    public void testCreateCoverageStoreAccessDenied() throws Exception {
        logout();
        String storeName = SystemTestData.CITE_PREFIX + "raster";
        // use Coverage2RenderedImageAdapterTest's method, just need any sample raster
        GridCoverage2D sampleCoverage = Coverage2RenderedImageAdapterTest.createTestCoverage(
                500, 500, 0, 0, 10, 10, DefaultGeographicCRS.WGS84);
        CoverageStoreInfo storeInfo = catalog.getCoverageStoreByName(storeName);
        assertNull("Store already exists " + storeInfo, storeInfo);
        ImportProcess importer = new ImportProcess(getCatalog());
        ProcessException exception = assertThrows(
                ProcessException.class,
                () -> importer.execute(
                        null,
                        sampleCoverage,
                        SystemTestData.CITE_PREFIX,
                        storeName,
                        "Buildings7",
                        CRS.decode("EPSG:4326"),
                        null,
                        null,
                        null));
        assertEquals("Operation unallowed with the current privileges", exception.getMessage());
    }

    /** Test creating a vector store when a store name is specified but does not exist */
    @Test
    public void testCreateDataStore() throws Exception {
        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();
        ForceCoordinateSystemFeatureResults sampleData =
                new ForceCoordinateSystemFeatureResults(rawSource, CRS.decode("EPSG:4326"));
        String storeName = SystemTestData.CITE_PREFIX + "data";
        DataStoreInfo storeInfo = catalog.getDataStoreByName(storeName);
        assertNull("Store already exists " + storeInfo, storeInfo);
        ImportProcess importer = new ImportProcess(getCatalog());
        String result = importer.execute(
                sampleData,
                null,
                SystemTestData.CITE_PREFIX,
                storeName,
                "Buildings5",
                CRS.decode("EPSG:4326"),
                null,
                null,
                null);
        // expect workspace:layername
        assertEquals(result, SystemTestData.CITE_PREFIX + ":" + "Buildings5");
    }

    @Test
    public void testCreateDataStoreAccessDenied() throws Exception {
        logout();
        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();
        ForceCoordinateSystemFeatureResults sampleData =
                new ForceCoordinateSystemFeatureResults(rawSource, CRS.decode("EPSG:4326"));
        String storeName = SystemTestData.CITE_PREFIX + "data";
        DataStoreInfo storeInfo = catalog.getDataStoreByName(storeName);
        assertNull("Store already exists " + storeInfo, storeInfo);
        ImportProcess importer = new ImportProcess(getCatalog());
        ProcessException exception = assertThrows(
                ProcessException.class,
                () -> importer.execute(
                        sampleData,
                        null,
                        SystemTestData.CITE_PREFIX,
                        storeName,
                        "Buildings8",
                        CRS.decode("EPSG:4326"),
                        null,
                        null,
                        null));
        assertEquals("Operation unallowed with the current privileges", exception.getMessage());
    }

    @Test
    public void testImportVectorMarsPOI() throws Exception {
        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(SystemTestData.MARS_POI));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();

        ImportProcess importer = new ImportProcess(getCatalog());
        String result = importer.execute(
                rawSource,
                null,
                SystemTestData.IAU_PREFIX,
                SystemTestData.IAU_PREFIX,
                "poi2",
                CRS.decode("IAU:49900"),
                null,
                null,
                null);

        assertEquals("iau:poi2", result);

        // check the layer
        LayerInfo layer = getCatalog().getLayerByName(result);
        assertNotNull(layer);
        assertEquals("point", layer.getDefaultStyle().getName());

        // check the feature type info
        FeatureTypeInfo fti = (FeatureTypeInfo) layer.getResource();
        assertEquals("IAU:49900", fti.getSRS());
        SimpleFeatureSource fs = (SimpleFeatureSource) fti.getFeatureSource(null, null);
        assertEquals(4, fs.getCount(Query.ALL));
    }
}
