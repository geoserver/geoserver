/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.lang.reflect.Method;
import org.geoserver.platform.ModuleStatusImpl;
import org.junit.Test;

public class UpdatesCheckerModuleStatusTest {

    @Test
    public void theModuleIsListedWithItsNameAndDocumentation() {
        UpdatesCheckerModuleStatus status = new UpdatesCheckerModuleStatus();
        assertEquals("gs-updates-checker", status.getModule());
        assertEquals("Check for Updates", status.getName());
        assertEquals(
                "community/updates-checker/index.html",
                status.getDocumentation().orElse(null));
        assertTrue(status.isAvailable());
        assertTrue(status.isEnabled());
    }

    /** The category and contact exist from GeoServer 3.0 only; where they do, the module sets them. */
    @Test
    public void theCategoryAndContactAreSetWhereGeoServerHasThem() throws Exception {
        UpdatesCheckerModuleStatus status = new UpdatesCheckerModuleStatus();
        Method getCategory;
        try {
            getCategory = ModuleStatusImpl.class.getMethod("getCategory");
        } catch (NoSuchMethodException e) {
            assumeNoException("this GeoServer has no module category", e);
            return;
        }
        assertEquals("COMMUNITY", String.valueOf(getCategory.invoke(status)));
        assertEquals(
                "gs-updates-checker@geoguru.africa",
                ModuleStatusImpl.class.getMethod("getContact").invoke(status));
    }
}
