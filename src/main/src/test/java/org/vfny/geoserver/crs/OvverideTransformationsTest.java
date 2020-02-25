/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.referencing.CRS;
import org.geotools.referencing.factory.epsg.CoordinateOperationFactoryUsingWKT;
import org.junit.AfterClass;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;

public class OvverideTransformationsTest extends GeoServerSystemTestSupport {

    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private static final String SOURCE_CRS = "EPSG:TEST1";
    private static final String TARGET_CRS = "EPSG:TEST2";

    private static final double[] SRC_TEST_POINT = {39.592654167, 3.084896111};
    private static final double[] DST_TEST_POINT = {39.594235744481225, 3.0844689951999427};
    private static String OLD_TMP_VALUE;

    @AfterClass
    public static void clearTemp() {
        if (OLD_TMP_VALUE == null) {
            System.clearProperty(JAVA_IO_TMPDIR);
        } else {
            System.setProperty(JAVA_IO_TMPDIR, OLD_TMP_VALUE);
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        OLD_TMP_VALUE = System.getProperty(JAVA_IO_TMPDIR);
        System.setProperty(JAVA_IO_TMPDIR, new File("./target").getCanonicalPath());

        super.onSetUp(testData);

        GeoServerResourceLoader loader1 = getResourceLoader();
        GeoServerResourceLoader loader2 = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        // setup the grid file, the definitions and the tx overrides
        new File(testData.getDataDirectoryRoot(), "user_projections").mkdir();
        testData.copyTo(
                OvverideTransformationsTest.class.getResourceAsStream("test_epsg.properties"),
                "user_projections/epsg.properties");
        testData.copyTo(
                OvverideTransformationsTest.class.getResourceAsStream(
                        "test_epsg_operations.properties"),
                "user_projections/epsg_operations.properties");
        testData.copyTo(
                OvverideTransformationsTest.class.getResourceAsStream("stgeorge.las"),
                "user_projections/stgeorge.las");
        testData.copyTo(
                OvverideTransformationsTest.class.getResourceAsStream("stgeorge.los"),
                "user_projections/stgeorge.los");

        CRS.reset("all");
    }

    /** Test method for {@link CoordinateOperationFactoryUsingWKT#createCoordinateOperation}. */
    @Test
    public void testCreateOperationFromCustomCodes() throws Exception {
        // Test CRSs
        CoordinateReferenceSystem source = CRS.decode(SOURCE_CRS);
        CoordinateReferenceSystem target = CRS.decode(TARGET_CRS);
        MathTransform mt = CRS.findMathTransform(source, target, true);

        // Test MathTransform
        double[] p = new double[2];
        mt.transform(SRC_TEST_POINT, 0, p, 0, 1);
        assertEquals(p[0], DST_TEST_POINT[0], 1e-8);
        assertEquals(p[1], DST_TEST_POINT[1], 1e-8);
    }

    /** Test method for {@link CoordinateOperationFactoryUsingWKT#createCoordinateOperation}. */
    @Test
    public void testOverrideEPSGOperation() throws Exception {
        // Test CRSs
        CoordinateReferenceSystem source = CRS.decode("EPSG:4269");
        CoordinateReferenceSystem target = CRS.decode("EPSG:4326");
        MathTransform mt = CRS.findMathTransform(source, target, true);

        // Test MathTransform
        double[] p = new double[2];
        mt.transform(SRC_TEST_POINT, 0, p, 0, 1);
        assertEquals(p[0], DST_TEST_POINT[0], 1e-8);
        assertEquals(p[1], DST_TEST_POINT[1], 1e-8);
    }

    /** Check we are actually using the EPSG database for anything not in override */
    @Test
    public void testFallbackOnEPSGDatabaseStd() throws Exception {
        // Test CRSs
        CoordinateReferenceSystem source = CRS.decode("EPSG:3002");
        CoordinateReferenceSystem target = CRS.decode("EPSG:4326");
        CoordinateOperation co =
                CRS.getCoordinateOperationFactory(true).createOperation(source, target);
        ConcatenatedOperation cco = (ConcatenatedOperation) co;
        // the EPSG one only has two steps, the non EPSG one 4
        assertEquals(2, cco.getOperations().size());
    }

    /** See if we can use the stgeorge grid shift files as the ESPG db would like us to */
    @Test
    public void testNadCon() throws Exception {
        CoordinateReferenceSystem crs4138 = CRS.decode("EPSG:4138");
        CoordinateReferenceSystem crs4326 = CRS.decode("EPSG:4326");
        MathTransform mt = CRS.findMathTransform(crs4138, crs4326);

        assertTrue(mt.toWKT().contains("NADCON"));

        double[] src = new double[] {-169.625, 56.575};
        double[] expected = new double[] {-169.62744, 56.576034};
        double[] p = new double[2];
        mt.transform(src, 0, p, 0, 1);
        assertEquals(expected[0], p[0], 1e-6);
        assertEquals(expected[1], p[1], 1e-6);
    }
}
