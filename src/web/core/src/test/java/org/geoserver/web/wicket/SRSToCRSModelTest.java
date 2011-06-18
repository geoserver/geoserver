package org.geoserver.web.wicket;

import junit.framework.TestCase;

import org.apache.wicket.model.Model;
import org.geotools.referencing.CRS;

public class SRSToCRSModelTest extends TestCase {
    
    public void testNullSRS() throws Exception {
        Model srs = new Model(null); 
        SRSToCRSModel crs = new SRSToCRSModel(srs);
        assertNull(crs.getObject());
        crs.setObject(null);
        assertEquals(null, srs.getObject());
    }
    
    public void testNonNullSRS() throws Exception {
        Model srs = new Model("EPSG:32632"); 
        SRSToCRSModel crs = new SRSToCRSModel(srs);
        assertEquals(CRS.decode("EPSG:32632"), crs.getObject());
        crs.setObject(CRS.decode("EPSG:4326"));
        assertEquals("EPSG:4326", srs.getObject());
    }

}
