/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;
import org.junit.Test;

public class LibraryModuleStatusTest {

    @Test
    public void testLibraryVersion() {
        LibraryModuleStatus status =
                new LibraryModuleStatus("gs-test", "Test", "GeoTools Metadata", Version.class.getName());
        assertTrue(status.isAvailable());
        assertEquals(
                "GeoTools Metadata version: " + GeoTools.getVersion(Version.class),
                status.getMessage().orElseThrow());
    }

    @Test
    public void testLibraryMissing() {
        LibraryModuleStatus status = new LibraryModuleStatus("gs-test", "Test", "Missing", "org.example.Missing");
        assertFalse(status.isAvailable());
        assertEquals("Missing not found", status.getMessage().orElseThrow());
    }
}
