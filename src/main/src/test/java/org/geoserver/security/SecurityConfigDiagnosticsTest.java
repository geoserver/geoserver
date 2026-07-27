/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.security.SecurityConfigDiagnostics.ComponentType;
import org.geoserver.security.SecurityConfigDiagnostics.DisabledComponent;
import org.junit.Test;

/**
 * Unit tests for the log-injection sanitization applied when a {@link DisabledComponent} is constructed. The persisted
 * name / alias / source plugin originate from a possibly attacker-controlled data directory and are echoed verbatim to
 * the server log, so CR / LF / control characters (which could forge additional log lines) must be neutralized at the
 * construction boundary.
 */
public class SecurityConfigDiagnosticsTest {

    private static final String CR = "\r";
    private static final String LF = "\n";
    private static final String TAB = "\t";

    @Test
    public void testCarriageReturnAndLineFeedAreStrippedFromName() {
        // a crafted folder name trying to forge a second, fake log line
        DisabledComponent c = new DisabledComponent(
                ComponentType.AUTHENTICATION_FILTER,
                "evil" + CR + LF + "2026-01-01 INFO Forged log line: server compromised",
                null,
                null,
                "reason");

        assertFalse("CR must be stripped", c.name().contains(CR));
        assertFalse("LF must be stripped", c.name().contains(LF));
        // the content survives (collapsed to a single line) so the operator still sees what was attempted
        assertTrue(c.name().startsWith("evil"));
        assertTrue(c.name().contains("Forged log line"));
    }

    @Test
    public void testControlCharactersAreStrippedFromAliasAndSourcePlugin() {
        DisabledComponent c = new DisabledComponent(
                ComponentType.ROLE_SERVICE, "name", "keycloak" + TAB + "Adapter", "plug" + CR + "in", "reason");

        // the tab (a control char) becomes a space and whitespace runs collapse to a single space
        assertEquals("keycloak Adapter", c.alias());
        assertFalse(c.alias().contains(TAB));
        assertEquals("plug in", c.sourcePlugin());
        assertFalse(c.sourcePlugin().contains(CR));
    }

    @Test
    public void testLineFeedInReasonIsStripped() {
        // the reason can embed an untrusted className, so it is line-flattened too
        DisabledComponent c = new DisabledComponent(
                ComponentType.AUTHENTICATION_FILTER,
                "name",
                null,
                null,
                "Filter class is not available: com.evil" + CR + LF + "FAKE LOG ENTRY");

        assertFalse("reason must not contain CR", c.reason().contains(CR));
        assertFalse("reason must not contain LF", c.reason().contains(LF));
        assertTrue(c.reason().contains("FAKE LOG ENTRY"));
    }

    @Test
    public void testOverlongNameIsCapped() {
        String huge = "x".repeat(5000);
        DisabledComponent c = new DisabledComponent(ComponentType.AUTHENTICATION_FILTER, huge, null, null, "reason");

        // a pathological value cannot bloat a log line: capped to the short-identifier bound (+ ellipsis)
        assertTrue("name length should be capped", c.name().length() <= 210);
        assertTrue(c.name().length() < huge.length());
    }

    @Test
    public void testNullValuesArePreserved() {
        DisabledComponent c = new DisabledComponent(ComponentType.AUTHENTICATION_FILTER, null, null, null, null);
        assertNull(c.name());
        assertNull(c.alias());
        assertNull(c.sourcePlugin());
        assertNull(c.reason());
    }

    @Test
    public void testCleanValuesPassThroughUnchanged() {
        DisabledComponent c = new DisabledComponent(
                ComponentType.AUTHENTICATION_FILTER,
                "keycloak",
                "keycloakAdapter",
                "gs-sec-keycloak (Keycloak)",
                "Created by the no longer installed plugin gs-sec-keycloak.");
        assertEquals("keycloak", c.name());
        assertEquals("keycloakAdapter", c.alias());
        assertEquals("gs-sec-keycloak (Keycloak)", c.sourcePlugin());
        assertEquals("Created by the no longer installed plugin gs-sec-keycloak.", c.reason());
    }
}
