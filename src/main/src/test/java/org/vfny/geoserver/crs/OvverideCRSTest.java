package org.vfny.geoserver.crs;

import java.io.File;

import org.geoserver.test.GeoServerTestSupport;
import org.geotools.referencing.CRS;
import org.geotools.referencing.datum.BursaWolfParameters;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;

public class OvverideCRSTest extends GeoServerTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        new File(getTestData().getDataDirectoryRoot(), "epsg").mkdir();
        getTestData().copyTo(OvverideCRSTest.class.getResourceAsStream("override_epsg.properties"), "epsg/override_epsg.properties");
    }
    
    public void testOverride() throws Exception {
        CoordinateReferenceSystem epsg3003 = CRS.decode("EPSG:3003");
        DefaultGeodeticDatum datum3003 = (DefaultGeodeticDatum) (((ProjectedCRS)  epsg3003).getDatum());
        BursaWolfParameters[] bwParamArray3003 = datum3003.getBursaWolfParameters();
        assertEquals(1, bwParamArray3003.length);
        BursaWolfParameters bw3003 = bwParamArray3003[0];
        assertEquals(-104.1, bw3003.dx);
        assertEquals(-49.1, bw3003.dy);
        assertEquals(-9.9, bw3003.dz);
        assertEquals(0.971, bw3003.ex);
        assertEquals(-2.917, bw3003.ey);
        assertEquals(0.714, bw3003.ez);
        assertEquals(-11.68, bw3003.ppm);
        
        // without an override they should be the same as 3002
        CoordinateReferenceSystem epsg3002 = CRS.decode("EPSG:3002");
        DefaultGeodeticDatum datum3002 = (DefaultGeodeticDatum) (((ProjectedCRS)  epsg3002).getDatum());
        BursaWolfParameters[] bwParamArray3002 = datum3002.getBursaWolfParameters();
        assertEquals(1, bwParamArray3002.length);
        BursaWolfParameters bw3002 = bwParamArray3002[0];
        assertFalse(bw3002.equals(bw3003));
    }
}
