/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.updateschecker.RunningVersion.Kind;
import org.junit.Test;

public class RunningVersionTest {

    private static Kind kind(String raw) {
        return RunningVersion.of(raw).getKind();
    }

    @Test
    public void plainReleaseIsComparable() {
        RunningVersion v = RunningVersion.of("2.27.0");
        assertEquals(Kind.RELEASE, v.getKind());
        assertTrue(v.isRelease());
        assertEquals("2.27.0", v.getRaw());
        assertEquals("2.27.x", v.getRelease().getSeries());
    }

    @Test
    public void developmentAndPreReleaseBuilds() {
        assertEquals(Kind.DEVELOPMENT, kind("3.1.0-SNAPSHOT"));
        assertEquals(Kind.DEVELOPMENT, kind("3.0.0-RC1"));
        assertEquals(Kind.DEVELOPMENT, kind("2.28.0-M1"));
        assertEquals(Kind.DEVELOPMENT, kind("2.28.0-beta"));
        assertEquals(Kind.DEVELOPMENT, kind("2.28.0-alpha"));
        // no patch component
        assertEquals(Kind.DEVELOPMENT, kind("3.0-RC"));
        assertEquals(Kind.DEVELOPMENT, kind("2.28-M0"));
        assertEquals(Kind.DEVELOPMENT, kind("2.28"));
        assertNull(RunningVersion.of("3.1.0-SNAPSHOT").getRelease());
    }

    @Test
    public void undeterminedVersionIsAnExplicitState() {
        assertEquals(Kind.UNKNOWN, kind(null));
        assertEquals(Kind.UNKNOWN, kind(""));
        assertEquals(Kind.UNKNOWN, kind("   "));
        assertEquals(Kind.UNKNOWN, kind("unknown"));
        assertEquals(Kind.UNKNOWN, kind("abc"));
        assertNull(RunningVersion.of(null).getRaw());
    }
}
