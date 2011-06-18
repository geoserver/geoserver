package org.geoserver.wfs.response;

import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestResult;

public class OGRWrapperTest extends TestCase {

    private OGRWrapper ogr;

    @Override
    public void run(TestResult result) {
        if (!Ogr2OgrTestUtil.isOgrAvailable())
            System.out.println("Skipping ogr2ogr wrapper tests, ogr2ogr could not be found, " + getName());
        else
            super.run(result);
    }
    
    @Override
    protected void setUp() throws Exception {
        ogr = new OGRWrapper(Ogr2OgrTestUtil.getOgr2Ogr(), Ogr2OgrTestUtil.getGdalData());
    }
    
    public void testAvaialable() {
        // kind of a smoke test, since ogr2ogrtestutil uses the same command!
        ogr.isAvailable();
    }
    
    public void testFormats() {
        Set<String> formats = ogr.getSupportedFormats();
        // well, we can't know which formats ogr was complied with, but at least there will be one, right?
        assertTrue(formats.size() > 0);
        
        // these work on my machine, with fwtools 2.2.8
        //assertTrue(formats.contains("KML"));
        //assertTrue(formats.contains("CSV"));
        //assertTrue(formats.contains("ESRI Shapefile"));
        //assertTrue(formats.contains("MapInfo File"));
    }
}
