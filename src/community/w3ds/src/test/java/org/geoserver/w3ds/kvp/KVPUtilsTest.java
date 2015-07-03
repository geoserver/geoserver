/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
* This code is licensed under the GPL 2.0 license, available at the root
* application directory.
*/
package org.geoserver.w3ds.kvp;

import junit.framework.TestCase;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.logging.Logger;


/**
 * Test case for KVPUtils.
 * 
 * @author Glen Blanchard
 * @since 2.5.x
 * 
 */

public class KVPUtilsTest extends TestCase {
    
    protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.w3ds");

    public void testParseBBoxWithNullProducingEnvelope() throws Exception {

        CoordinateReferenceSystem crs = KVPUtils.parseCRS("EPSG:900913");
        String input = "0,0,100,100";

        Envelope crsEnvelope = CRS.getEnvelope(crs);
        assertNull("This envelope needs to be null so that we will trigger the fault or we will get a false positive", crsEnvelope);
        Envelope env = KVPUtils.parseBbox(input, crs, LOGGER);
        assertEquals("testStandardRequest",100.0,env.getMaximum(0));
    }
    

}
