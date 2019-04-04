/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geotools.referencing.CRS;
import org.geotools.referencing.datum.BursaWolfParameters;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;

@Category(SystemTest.class)
public class OverrideCRSTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        new File(testData.getDataDirectoryRoot(), "user_projections").mkdir();
        testData.copyTo(
                OverrideCRSTest.class.getResourceAsStream("test_override_epsg.properties"),
                "user_projections/epsg_overrides.properties");

        CRS.reset("all");
        testData.setUpSecurity();
    }

    @Test
    public void testOverride() throws Exception {
        CoordinateReferenceSystem epsg3003 = CRS.decode("EPSG:3003");
        DefaultGeodeticDatum datum3003 =
                (DefaultGeodeticDatum) (((ProjectedCRS) epsg3003).getDatum());
        BursaWolfParameters[] bwParamArray3003 = datum3003.getBursaWolfParameters();
        assertEquals(1, bwParamArray3003.length);
        BursaWolfParameters bw3003 = bwParamArray3003[0];
        double tol = 1E-7;
        assertEquals(-104.1, bw3003.dx, tol);
        assertEquals(-49.1, bw3003.dy, tol);
        assertEquals(-9.9, bw3003.dz, tol);
        assertEquals(0.971, bw3003.ex, tol);
        assertEquals(-2.917, bw3003.ey, tol);
        assertEquals(0.714, bw3003.ez, tol);
        assertEquals(-11.68, bw3003.ppm, tol);

        // without an override they should be the same as 3002
        CoordinateReferenceSystem epsg3002 = CRS.decode("EPSG:3002");
        DefaultGeodeticDatum datum3002 =
                (DefaultGeodeticDatum) (((ProjectedCRS) epsg3002).getDatum());
        BursaWolfParameters[] bwParamArray3002 = datum3002.getBursaWolfParameters();
        assertEquals(1, bwParamArray3002.length);
        BursaWolfParameters bw3002 = bwParamArray3002[0];
        assertFalse(bw3002.equals(bw3003));
    }
}
