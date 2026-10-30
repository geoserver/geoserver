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

public class VersionTest {

    @Test
    // "3.0.1.1" is a four-part version, not an IP address
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public void parsesOnlyPlainReleases() {
        Version v = Version.parse("2.27.6");
        assertNotNull(v);
        assertEquals("2.27.x", v.getSeries());
        assertEquals("2.27.6", v.toString());

        assertNull(Version.parse("3.1.0-SNAPSHOT"));
        assertNull(Version.parse("3.0"));
        assertNull(Version.parse("3.0.1.1"));
        assertNull(Version.parse("abc"));
        assertNull(Version.parse(""));
        assertNull(Version.parse(null));
        assertNull(Version.parse("2.27.x"));
    }

    @Test
    public void comparesNumericallyNotAsText() {
        assertTrue(Version.parse("2.10.0").compareTo(Version.parse("2.9.0")) > 0);
        assertTrue(Version.parse("2.9.12").compareTo(Version.parse("2.9.9")) > 0);
        assertTrue(Version.parse("3.0.0").compareTo(Version.parse("2.99.99")) > 0);
        assertEquals(0, Version.parse("3.0.1").compareTo(Version.parse("3.0.1")));
        assertEquals(0, Version.parse("3.0.1").compareTo(Version.parse(" 3.0.1 ")));
    }

    @Test
    public void comparesSeriesNumerically() {
        assertTrue(Version.compareSeries("2.10.x", "2.9.x") > 0);
        assertTrue(Version.compareSeries("3.0.x", "2.28.x") > 0);
        assertEquals(0, Version.compareSeries("2.27.x", "2.27.x"));
        assertTrue(Version.isSeries("2.27.x"));
        assertFalse(Version.isSeries("2.27.0"));
        assertFalse(Version.isSeries(null));
    }
}
