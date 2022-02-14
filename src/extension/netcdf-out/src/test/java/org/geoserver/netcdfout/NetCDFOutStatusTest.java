/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.netcdfout;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class NetCDFOutStatusTest {

    @Test
    public void test() {
        try (ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("applicationContextStatus.xml")) {
            assertNotNull(context);

            Optional<ModuleStatus> status =
                    GeoServerExtensions.extensions(ModuleStatus.class, context).stream()
                            .filter(s -> s.getModule().equalsIgnoreCase("gs-netcdf-out"))
                            .findFirst();
            assertTrue(status.isPresent());
        }
    }

    @Test
    public void testNetCDFOutStatus() {
        NetCDFOutStatus status = new NetCDFOutStatus();

        // these should always return "something"
        assertTrue(status.getModule().length() > 0);
        assertTrue(status.getName().length() > 0);
        assertTrue(status.getComponent().get().length() > 0);
        assertTrue(status.getMessage().get().length() > 0);

        assertTrue(status.getVersion().isPresent());
        assertTrue(status.getMessage().get().contains("NETCDF-4 Binary"));
    }
}
