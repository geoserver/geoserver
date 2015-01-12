package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.geoserver.data.test.MockData;
import org.geoserver.security.decorators.DecoratingGridCoverage2DReader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.parameter.GeneralParameterValue;

/**
 * This test class tests the {@link GridCoverageReaderCallback} interface and the ability to skip the extension point if not needed.
 * 
 * 
 * @author Nicola Lagomarsini
 */
public class GridCoverageReaderCallbackTest extends GeoServerSystemTestSupport {

    @Before
    public void setup() throws Exception {
        // build the store
        Catalog cat = getCatalog();
        addStore(cat, "dem", "tazdem.tiff");
        addStore(cat, "bm", "tazbm.tiff");
    }

    @After
    public void removeFromCatalog() throws Exception {
        // build the store
        Catalog cat = getCatalog();
        cat.remove(cat.getCoverageByName("tazdem"));
        cat.remove(cat.getCoverageByName("tazbm"));
        cat.remove(cat.getCoverageStoreByName("dem"));
        cat.remove(cat.getCoverageStoreByName("bm"));
    }

    private void addStore(Catalog cat, String storeName, String fileName) throws Exception {
        CatalogBuilder cb = new CatalogBuilder(cat);
        CoverageStoreInfo store = cb.buildCoverageStore(storeName);
        store.setURL(MockData.class.getResource(fileName).toExternalForm());
        store.setType("GeoTIFF");
        cat.add(store);
        // build the coverage
        cb.setStore(store);
        CoverageInfo ci = cb.buildCoverage();
        cat.add(ci);
    }

    @Test
    public void testMockCallback() throws Exception {
        // Getting the catalog
        Catalog catalog = getCatalog();
        // Extracting the coverages
        CoverageInfo info = catalog.getCoverageByName("tazdem");

        // Calling the ResourcePool for getting the GridCoverageReader
        ResourcePool pool = catalog.getResourcePool();
        // Adding the new MockCallback
        pool.gridCoverageReaderCallbacks.add(new MockCallback());

        // Getting the reader for the Coverage
        GridCoverageReader gridCoverageReader = pool.getGridCoverageReader(info, info.getName(),
                null);

        // Ensure the reader is the returned reader is not null and also is an instance of the CoverageDimensionCustomizerReader class
        assertTrue(gridCoverageReader instanceof CoverageDimensionCustomizerReader);

        // Get the Returned Coverage
        GridCoverage2D cov = (GridCoverage2D) gridCoverageReader.read(null);
        // Ensure is the one created by the MockGridCoverage2DReader
        assertTrue(cov.getName().toString().equalsIgnoreCase("tazdem"));
        Envelope2D envelope2d = cov.getEnvelope2D();
        assertEquals(envelope2d.getMinX(), 0, 0);
        assertEquals(envelope2d.getMinY(), 0, 0);
        assertEquals(envelope2d.getMaxX(), 1, 0);
        assertEquals(envelope2d.getMaxY(), 1, 0);

        // Test the same with another CoverageInfo, it should return another GridCoverage
        info = catalog.getCoverageByName("tazbm");
        gridCoverageReader = pool.getGridCoverageReader(info, info.getName(), null);

        // Ensure the reader is the returned reader is not null and also is an instance of the CoverageDimensionCustomizerReader class
        assertTrue(gridCoverageReader instanceof CoverageDimensionCustomizerReader);

        // Get the Returned Coverage
        cov = (GridCoverage2D) gridCoverageReader.read(null);
        // Ensure is the one created by the MockGridCoverage2DReader
        assertTrue(cov.getName().toString().equalsIgnoreCase("tazbm"));
        envelope2d = cov.getEnvelope2D();
        assertNotEquals(envelope2d.getMinX(), 0, 0);
        assertNotEquals(envelope2d.getMinY(), 0, 0);
        assertNotEquals(envelope2d.getMaxX(), 1, 0);
        assertNotEquals(envelope2d.getMaxY(), 1, 0);
    }

    @Test
    public void testNormalBehavior() throws Exception {
        // Getting the catalog
        Catalog catalog = getCatalog();

        // Calling the ResourcePool for getting the GridCoverageReader
        ResourcePool pool = catalog.getResourcePool();
        // Adding the new MockCallback
        pool.gridCoverageReaderCallbacks.add(new MockCallback());

        // It should return another GridCoverage, not the wrapped one
        CoverageInfo info = catalog.getCoverageByName("tazbm");
        // Adding Hints
        Hints hints = new Hints(ResourcePool.SKIP_COVERAGE_EXTENSIONS_LOOKUP, true);

        GridCoverageReader gridCoverageReader = pool.getGridCoverageReader(info, info.getName(),
                hints);

        // Ensure the reader is the returned reader is not null and also is an instance of the CoverageDimensionCustomizerReader class
        assertTrue(gridCoverageReader instanceof CoverageDimensionCustomizerReader);

        // Get the Returned Coverage
        GridCoverage2D cov = (GridCoverage2D) gridCoverageReader.read(null);
        // Ensure is the one created by the MockGridCoverage2DReader
        assertTrue(cov.getName().toString().equalsIgnoreCase("tazbm"));
        Envelope2D envelope2d = cov.getEnvelope2D();
        assertNotEquals(envelope2d.getMinX(), 0, 0);
        assertNotEquals(envelope2d.getMinY(), 0, 0);
        assertNotEquals(envelope2d.getMaxX(), 1, 0);
        assertNotEquals(envelope2d.getMaxY(), 1, 0);
    }

    static class MockCallback implements GridCoverageReaderCallback {

        @Override
        public boolean canHandle(CoverageInfo info) {
            return info.getStore().getName().equalsIgnoreCase("dem");
        }

        @Override
        public GridCoverage2DReader wrapGridCoverageReader(ResourcePool pool,
                CoverageInfo coverageInfo, String coverageName, Hints hints) throws IOException {
            return new MockGridCoverage2DReader(
                    (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, hints));
        }
    }

    static class MockGridCoverage2DReader extends DecoratingGridCoverage2DReader {

        private GridCoverageFactory gridCoverageFactory;

        public MockGridCoverage2DReader(GridCoverage2DReader delegate) {
            super(delegate);
            gridCoverageFactory = new GridCoverageFactory(GeoTools.getDefaultHints());
        }

        @Override
        public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
                throws IllegalArgumentException, IOException {
            return gridCoverageFactory.create("testData", new BufferedImage(1, 1,
                    BufferedImage.TYPE_BYTE_GRAY), new ReferencedEnvelope(0, 1, 0, 1,
                    DefaultGeographicCRS.WGS84));
        }

        @Override
        public GridCoverage2D read(GeneralParameterValue[] parameters)
                throws IllegalArgumentException, IOException {

            return gridCoverageFactory.create("testData", new BufferedImage(1, 1,
                    BufferedImage.TYPE_BYTE_GRAY), new ReferencedEnvelope(0, 1, 0, 1,
                    DefaultGeographicCRS.WGS84));
        }
    }
}
