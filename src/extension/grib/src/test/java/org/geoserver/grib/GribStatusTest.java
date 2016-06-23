package org.geoserver.grib;

import static org.junit.Assert.assertTrue;

import org.geoserver.grib.GribStatus;
import org.junit.Test;

public class GribStatusTest {
    @Test
    public void testNetCDFStatus() {
        GribStatus status = new GribStatus();

        // these should always return "something"
        assertTrue(status.getModule().length() > 0);
        assertTrue(status.getName().length() > 0);
        assertTrue(status.getComponent().get().length() > 0);
        assertTrue(status.getMessage().get().length() > 0);

        assertTrue(status.getVersion().isPresent());
        assertTrue(status.getMessage().get().contains("isGribAvailable"));
    }
}
