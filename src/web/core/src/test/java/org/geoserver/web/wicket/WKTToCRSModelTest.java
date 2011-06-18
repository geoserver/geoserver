package org.geoserver.web.wicket;

import junit.framework.TestCase;

import org.apache.wicket.model.Model;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class WKTToCRSModelTest extends TestCase {
    
    public void testNullSRS() throws Exception {
        Model wkt = new Model(null); 
        WKTToCRSModel crs = new WKTToCRSModel(wkt);
        assertNull(crs.getObject());
        crs.setObject(null);
        assertEquals(null, wkt.getObject());
    }
    
    public void testNonNullSRS() throws Exception {
        CoordinateReferenceSystem utm32n = CRS.decode("EPSG:32632");
        CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
        Model wkt = new Model(utm32n.toString()); 
        WKTToCRSModel crs = new WKTToCRSModel(wkt);
        assertTrue(CRS.equalsIgnoreMetadata(utm32n, crs.getObject()));
        crs.setObject(wgs84);
        assertEquals(wgs84.toString(), wkt.getObject());
    }

}
