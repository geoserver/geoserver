/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import java.io.File;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;

public class GeoIPPostProcessorTest {

    static LookupService geoipLookup;

    @BeforeClass
    public static void createGeoIPDatabase() throws IOException {
        File f = new File("GeoLiteCity.dat");
        if (!f.exists()) {
            return;
        }

        geoipLookup = new LookupService(f);
    }

    @Test
    public void testLookup() throws Exception {
        if (geoipLookup == null) {
            return;
        }

        Location loc = geoipLookup.getLocation("64.147.114.82");
        assertEquals("United States", loc.countryName);
        assertEquals("New York", loc.city);

        loc = geoipLookup.getLocation("192.168.1.103");
        assertNull(loc);
    }
}
