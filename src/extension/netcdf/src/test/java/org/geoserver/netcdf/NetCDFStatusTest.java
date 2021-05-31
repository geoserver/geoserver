/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.netcdf;

import static org.junit.Assert.assertTrue;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class NetCDFStatusTest extends GeoServerSystemTestSupport {
    @Test
    public void test() {
        assertModuleStatus("gs-netcdf", "NetCDF Coverage format");
    }

    @Test
    public void testNetCDFStatus() {

        NetCDFStatus nStatus = new NetCDFStatus();

        // these should always return "something"
        assertTrue(nStatus.getModule().length() > 0);
        assertTrue(nStatus.getName().length() > 0);
        assertTrue(nStatus.getComponent().get().length() > 0);
        assertTrue(nStatus.getMessage().get().length() > 0);

        assertTrue(nStatus.getVersion().isPresent());
        assertTrue(nStatus.getMessage().get().contains("NETCDF-4 Binary"));
    }
}
