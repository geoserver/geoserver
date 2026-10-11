/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdatesCheckerVersionTest {

    @Test
    // "3.0.1.1" is a four-part version, not an IP address
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public void parsesOnlyPlainReleases() {
        UpdatesCheckerVersion v = UpdatesCheckerVersion.parse("2.27.6");
        assertNotNull(v);
        assertEquals("2.27.x", v.getSeries());
        assertEquals("2.27.6", v.toString());

        assertNull(UpdatesCheckerVersion.parse("3.1.0-SNAPSHOT"));
        assertNull(UpdatesCheckerVersion.parse("3.0"));
        assertNull(UpdatesCheckerVersion.parse("3.0.1.1"));
        assertNull(UpdatesCheckerVersion.parse("abc"));
        assertNull(UpdatesCheckerVersion.parse(""));
        assertNull(UpdatesCheckerVersion.parse(null));
        assertNull(UpdatesCheckerVersion.parse("2.27.x"));
    }

    @Test
    public void comparesNumericallyNotAsText() {
        assertTrue(UpdatesCheckerVersion.parse("2.10.0").compareTo(UpdatesCheckerVersion.parse("2.9.0")) > 0);
        assertTrue(UpdatesCheckerVersion.parse("2.9.12").compareTo(UpdatesCheckerVersion.parse("2.9.9")) > 0);
        assertTrue(UpdatesCheckerVersion.parse("3.0.0").compareTo(UpdatesCheckerVersion.parse("2.99.99")) > 0);
        assertEquals(0, UpdatesCheckerVersion.parse("3.0.1").compareTo(UpdatesCheckerVersion.parse("3.0.1")));
        assertEquals(UpdatesCheckerVersion.parse("3.0.1"), UpdatesCheckerVersion.parse(" 3.0.1 "));
    }

    @Test
    public void comparesSeriesNumerically() {
        assertTrue(UpdatesCheckerVersion.compareSeries("2.10.x", "2.9.x") > 0);
        assertTrue(UpdatesCheckerVersion.compareSeries("3.0.x", "2.28.x") > 0);
        assertEquals(0, UpdatesCheckerVersion.compareSeries("2.27.x", "2.27.x"));
        assertTrue(UpdatesCheckerVersion.isSeries("2.27.x"));
        assertFalse(UpdatesCheckerVersion.isSeries("2.27.0"));
        assertFalse(UpdatesCheckerVersion.isSeries(null));
    }
}
