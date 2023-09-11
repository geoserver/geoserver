/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.Before;
import org.junit.Test;

public class WCSUtilsTest extends GeoServerSystemTestSupport {

    private static final double EPS = 1e-6;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefaultRasterLayers();
    }

    @Before
    public void resetLayers() throws IOException {
        getTestData().addDefaultRasterLayer(SystemTestData.TASMANIA_DEM, getCatalog());
        getTestData().addDefaultRasterLayer(SystemTestData.ROTATED_CAD, getCatalog());
    }

    @Test
    public void fitEnvelopeOutside() throws Exception {
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(SystemTestData.TASMANIA_DEM));
        // native envelope is
        // [(144.999999999998, -42.999999999986805), (145.9999999999976,-40.99999999998761)]
        // resolution is 0.00833333333333
        // set a native envelope containing the native one
        ReferencedEnvelope configured =
                new ReferencedEnvelope(144.96, 146.06, -43.1, -40.9, ci.getCRS());
        ci.setNativeBoundingBox(configured);
        getCatalog().save(ci);
        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);

        ReferencedEnvelope fitted = WCSUtils.fitEnvelope(ci, reader);
        // results verified with a spreadsheet doing repeated additions/subtractions
        assertEquals(144.9583333, fitted.getMinimum(0), EPS);
        assertEquals(146.0583333, fitted.getMaximum(0), EPS);
        assertEquals(-43.09999999, fitted.getMinimum(1), EPS);
        assertEquals(-40.89999999, fitted.getMaximum(1), EPS);
    }

    @Test
    public void fitEnvelopeInside() throws Exception {
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(SystemTestData.TASMANIA_DEM));
        // native envelope is
        // [(144.999999999998, -42.999999999986805), (145.9999999999976,-40.99999999998761)]
        // resolution is 0.00833333333333
        // set a native envelope smaller than native
        ReferencedEnvelope configured =
                new ReferencedEnvelope(145.07, 145.93, -42.92, -41.06, ci.getCRS());
        ci.setNativeBoundingBox(configured);
        getCatalog().save(ci);
        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);

        ReferencedEnvelope fitted = WCSUtils.fitEnvelope(ci, reader);
        assertEquals(145.0666666, fitted.getMinimum(0), EPS);
        assertEquals(145.9333333, fitted.getMaximum(0), EPS);
        assertEquals(-42.91666666, fitted.getMinimum(1), EPS);
        assertEquals(-41.05833333, fitted.getMaximum(1), EPS);
    }

    @Test
    public void fitEnvelopeTooSmall() throws Exception {
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(SystemTestData.TASMANIA_DEM));
        // native envelope is
        // [(144.999999999998, -42.999999999986805), (145.9999999999976,-40.99999999998761)]
        // resolution is 0.00833333333333
        // set a native envelope that's smaller than a pixel
        ReferencedEnvelope configured =
                new ReferencedEnvelope(145, 145.001, -43, -42.999, ci.getCRS());
        ci.setNativeBoundingBox(configured);
        getCatalog().save(ci);
        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);

        // the original corner is maintained, the other is pushed to be at least a pixel
        ReferencedEnvelope fitted = WCSUtils.fitEnvelope(ci, reader);
        assertEquals(144.9999999, fitted.getMinimum(0), EPS);
        assertEquals(145.0083333, fitted.getMaximum(0), EPS);
        assertEquals(-42.99999999, fitted.getMinimum(1), EPS);
        assertEquals(-42.99166666, fitted.getMaximum(1), EPS);
    }

    @Test
    public void fitGridGeometry() throws Exception {
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(SystemTestData.TASMANIA_DEM));
        // same config as fitEnvelopeTooSmall, makes for an easy grid geometry
        ReferencedEnvelope configured =
                new ReferencedEnvelope(145, 145.001, -43, -42.999, ci.getCRS());
        ci.setNativeBoundingBox(configured);
        getCatalog().save(ci);
        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);

        GridGeometry2D fgg = WCSUtils.fitGridGeometry(ci, reader);
        // just one pixel
        GridEnvelope2D gridRange = fgg.getGridRange2D();
        assertEquals(1, gridRange.getSpan(0));
        assertEquals(1, gridRange.getSpan(1));
        // check the affine, origin is the expected one (top left of fitted envelope)
        AffineTransform2D at = (AffineTransform2D) fgg.getGridToCRS(PixelInCell.CELL_CENTER);
        assertEquals(144.9999999, at.getTranslateX(), EPS);
        assertEquals(-42.99166666, at.getTranslateY(), EPS);
        // scale has been preserved
        assertEquals(0.00833333333333, at.getScaleX(), EPS);
        assertEquals(-0.00833333333333, at.getScaleY(), EPS);
    }

    @Test
    public void fitGridGeometryRotated() throws Exception {
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(SystemTestData.ROTATED_CAD));
        ci.setNativeBoundingBox(
                new ReferencedEnvelope(1402800, 1402900, 5000000, 5000100, ci.getNativeCRS()));
        getCatalog().save(ci);

        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        GridGeometry2D fgg = WCSUtils.fitGridGeometry(ci, reader);
        // check the affine, origin is the expected one (top left of fitted envelope)
        AffineTransform2D at = (AffineTransform2D) fgg.getGridToCRS(PixelInCell.CELL_CENTER);
        assertEquals(1402800, at.getTranslateX(), EPS);
        assertEquals(5000100, at.getTranslateY(), EPS);
        // scale has been preserved
        assertEquals(0.1128513, at.getScaleX(), EPS);
        assertEquals(-0.1128513, at.getScaleY(), EPS);
        // area is now square
        GridEnvelope2D gridRange = fgg.getGridRange2D();
        assertEquals(886, gridRange.getSpan(0));
        assertEquals(886, gridRange.getSpan(1));
    }

    @Test
    public void fitReprojected() throws Exception {
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(SystemTestData.TASMANIA_DEM));
        ci.setSRS("EPSG:3857");
        ci.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
        ReferencedEnvelope forcedEnvelope =
                new ReferencedEnvelope(
                        16141326, 16252645, -5311971, -5012341, CRS.decode("EPSG:3857"));
        ci.setNativeBoundingBox(forcedEnvelope);
        ci.setNativeCRS(CRS.decode("EPSG:3857"));
        getCatalog().save(ci);

        // no envelope fitting in case of reprojection, the declared one is forced
        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        ReferencedEnvelope envelope = WCSUtils.fitEnvelope(ci, reader);
        assertEquals(forcedEnvelope, envelope);

        // grid geometry is positioned based on the forced envelope
        GridGeometry2D fgg = WCSUtils.fitGridGeometry(ci, reader);
        AffineTransform2D fg2w = (AffineTransform2D) fgg.getGridToCRS();
        assertEquals(16141326, fg2w.getTranslateX(), 1d);
        assertEquals(-5012341, fg2w.getTranslateY(), 1d);

        // Scale factors: for comparison let's build a diagonal segment at the source of the data
        MathTransform mt =
                CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRS.decode("EPSG:3857"));
        double nativeRes = 0.00833333333333;
        double[] points = {145, -41, 145 + nativeRes, -41 + nativeRes};
        mt.transform(points, 0, points, 0, 2);
        // scales should more or less match the segment offsets
        // (WCSUtils uses the full coverage extent so we need a sizeable tolerance)
        assertEquals(points[2] - points[0], fg2w.getScaleX(), 20d);
        assertEquals(points[3] - points[1], -fg2w.getScaleY(), 20d);
    }
}
