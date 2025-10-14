/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.geoserver.catalog.CoverageInfo;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.geometry.Bounds;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.util.NullProgressListener;
import org.geotools.geometry.GeneralBounds;
import org.geotools.process.raster.ContourProcess;
import org.geotools.util.factory.GeoTools;
import org.junit.Test;

/**
 * Test class for the contour process.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class ContourProcessTest extends BaseRasterToVectorTest {

    /**
     * Test basic capabilities for the contour process. It works on the DEM tiff and produces a shapefile. Nothing more
     * nothing less.
     */
    @Test
    public void testProcessStandaloneBasicValues() throws Exception {
        GridCoverage2D gc = extractCoverageSubset();

        // extract just two isolines
        final double[] levels = new double[2];
        levels[0] = 1500;
        levels[1] = 1700;
        final ContourProcess process = new ContourProcess();
        final SimpleFeatureCollection fc =
                process.execute(gc, 0, levels, null, false, false, null, new NullProgressListener());

        assertNotNull(fc);
        assertTrue(fc.size() > 0);

        try (SimpleFeatureIterator fi = fc.features()) {
            while (fi.hasNext()) {
                SimpleFeature sf = fi.next();
                Double value = (Double) sf.getAttribute("value");
                assertTrue(value == 1500.0 || value == 1700.0);
            }
        }
    }

    private GridCoverage2D extractCoverageSubset() throws IOException {
        // get the coverage
        CoverageInfo dem = getCatalog().getCoverageByName(DEM.getLocalPart());
        GridCoverage2D gc = (GridCoverage2D) dem.getGridCoverage(null, GeoTools.getDefaultHints());

        // extract only a small part of it
        Bounds fullEnvelope = gc.getEnvelope();
        GeneralBounds subset = new GeneralBounds(fullEnvelope.getCoordinateReferenceSystem());
        double minX = fullEnvelope.getMinimum(0);
        double minY = fullEnvelope.getMinimum(1);
        double offsetX = fullEnvelope.getSpan(0) / 5;
        double offsetY = fullEnvelope.getSpan(1) / 5;
        subset.setEnvelope(minX + offsetX, minY + offsetY, minX + offsetX * 2, minY + offsetY * 2);
        gc = (GridCoverage2D) new Operations(null).crop(gc, subset);

        scheduleForDisposal(gc);

        return gc;
    }

    /**
     * Test basic capabilities for the contour process. It works on the DEM tiff and produces a shapefile. Nothing more
     * nothing less.
     */
    @Test
    public void testProcessStandaloneBasicInterval() throws Exception {
        final GridCoverage2D gc = extractCoverageSubset();

        final double step = 100;
        final ContourProcess process = new ContourProcess();
        final SimpleFeatureCollection fc =
                process.execute(gc, 0, null, Double.valueOf(step), false, false, null, new NullProgressListener());

        assertNotNull(fc);
        assertTrue(fc.size() > 0);

        try (SimpleFeatureIterator fi = fc.features()) {
            while (fi.hasNext()) {
                SimpleFeature sf = fi.next();
                Double value = (Double) sf.getAttribute("value");
                assertTrue(value > 0);
            }
        }
    }
}
