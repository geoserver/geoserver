/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class OGRWrapperTest {

    private OGRWrapper ogr;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(Ogr2OgrTestUtil.isOgrAvailable());
        ogr =
                new OGRWrapper(
                        Ogr2OgrTestUtil.getOgr2Ogr(),
                        Collections.singletonMap("GDAL_DATA", Ogr2OgrTestUtil.getGdalData()));
    }

    @Test
    public void testAvaialable() {
        // kind of a smoke test, since ogr2ogrtestutil uses the same command!
        ogr.isAvailable();
    }

    @Test
    public void testFormats() {
        Set<String> formats = ogr.getSupportedFormats();
        // well, we can't know which formats ogr was complied with, but at least there will be one,
        // right?
        assertTrue(formats.size() > 0);

        // these work on my machine, with fwtools 2.2.8
        // assertTrue(formats.contains("KML"));
        // assertTrue(formats.contains("CSV"));
        // assertTrue(formats.contains("ESRI Shapefile"));
        // assertTrue(formats.contains("MapInfo File"));
    }
}
