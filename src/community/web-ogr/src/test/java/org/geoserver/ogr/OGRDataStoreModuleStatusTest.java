/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geotools.data.ogr.jni.JniOGRDataStoreFactory;
import org.junit.Test;

public class OGRDataStoreModuleStatusTest {

    @Test
    public void testStatus() {
        OGRDataStoreModuleStatus status = new OGRDataStoreModuleStatus();
        boolean available = new JniOGRDataStoreFactory().isAvailable();
        assertEquals(available, status.isAvailable());

        String message = status.getMessage().orElseThrow();
        assertTrue(message, message.startsWith("JNI GDAL Wrapper Version: "));
        assertTrue(message, message.contains(available ? "GDAL version: " : "GDAL native library not found"));
    }
}
