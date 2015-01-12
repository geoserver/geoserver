/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.data.test.SystemTestData;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.storage.blobstore.file.FileBlobStore;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.datum.PixelInCell;

public class CachingGridCoverage2DReaderTest extends GridCoverageCacheBaseTest {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

    }

    @Test
    public void testReaderCaching() throws GeoWebCacheException, IOException, NoSuchFieldException,
            SecurityException, IllegalArgumentException, IllegalAccessException {
        CoverageInfo info = coverageInfo;
        ResourcePool resourcePool = catalog.getResourcePool();
        CachingGridCoverage2DReader cachingReader = null;

        try {
            cachingReader = CachingGridCoverage2DReader.wrap(resourcePool, gridCoveragesCache,
                    info, "World", null);

            assertTrue(CRS.equalsIgnoreMetadata(info.getCRS(),
                    cachingReader.getCoordinateReferenceSystem()));

            assertEquals(info.getNativeBoundingBox(),
                    new ReferencedEnvelope(cachingReader.getOriginalEnvelope()));

            assertEquals(info.getGrid().getGridToCRS(),
                    cachingReader.getOriginalGridToWorld(PixelInCell.CELL_CENTER));

            ParameterValue<GridGeometry2D> gridGeometryParam = GeoTiffFormat.READ_GRIDGEOMETRY2D
                    .createValue();
            GridGeometry origGrid = info.getGrid();
            GridEnvelope origGridRange = origGrid.getGridRange();

            // Reading a fourth part across X and half part across Y
            GridEnvelope reducedRange = new GridEnvelope2D(origGridRange.getLow(0),
                    origGridRange.getLow(1), origGridRange.getSpan(0), origGridRange.getSpan(1) * 2);
            ReferencedEnvelope envelope = info.getNativeBoundingBox();
            ReferencedEnvelope newEnvelope = new ReferencedEnvelope(envelope.getMinX(),
                    envelope.getMinX() + envelope.getSpan(0) / 4d, envelope.getMinY(),
                    envelope.getMinY() + envelope.getSpan(1) / 2d,
                    envelope.getCoordinateReferenceSystem());
            GridGeometry2D readGridGeometry = new GridGeometry2D(reducedRange, newEnvelope);

            gridGeometryParam.setValue(readGridGeometry);
            GridCoverage2D gridCoverage = cachingReader
                    .read(new GeneralParameterValue[] { gridGeometryParam });
            assertNotNull(gridCoverage);

            if (blobStore instanceof FileBlobStore) {
                FileBlobStore fileBlobStore = (FileBlobStore) blobStore;
                Field pathRef = fileBlobStore.getClass().getDeclaredField("path");
                pathRef.setAccessible(true);
                String path = (String) pathRef.get(fileBlobStore);

                // Checking that the associated blobstore contains 16 tiles
                String globalPath = path + "/wcs_Worldcov/myEPSG_4326_03/0_0";
                final File file = new File(globalPath);
                String[] tilesList = file.list();
                assertEquals(16, tilesList.length);
            }

        } finally {
            if (cachingReader != null) {
                try {
                    cachingReader.dispose();
                } catch (Throwable t) {

                }
            }
        }

        info = catalog.getCoverageByName("watertemp");
        try {
            cachingReader = CachingGridCoverage2DReader.wrap(resourcePool, gridCoveragesCache,
                    info, "watertemp", null);

            assertTrue(cachingReader instanceof StructuredGridCoverage2DReader);
            assertTrue(CRS.equalsIgnoreMetadata(info.getCRS(),
                    cachingReader.getCoordinateReferenceSystem()));

            assertEquals(info.getNativeBoundingBox(),
                    new ReferencedEnvelope(cachingReader.getOriginalEnvelope()));

            assertEquals(info.getGrid().getGridToCRS(),
                    cachingReader.getOriginalGridToWorld(PixelInCell.CELL_CENTER));

            // TODO: Add tests for filtering
        } finally {
            if (cachingReader != null) {
                try {
                    cachingReader.dispose();
                } catch (Throwable t) {

                }
            }
        }
    }
}
