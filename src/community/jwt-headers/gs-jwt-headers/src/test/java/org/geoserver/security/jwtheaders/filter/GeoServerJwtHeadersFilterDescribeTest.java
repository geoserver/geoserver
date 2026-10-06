/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Test;

/** The troubleshooting description of a header value never carries a token signature, nor raw line breaks. */
public class GeoServerJwtHeadersFilterDescribeTest {

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String jwt(String claims) {
        return b64("{\"alg\":\"RS256\"}") + "." + b64(claims) + ".SIGNATURE-SEGMENT";
    }

    @Test
    public void testJwtDecodedWithoutSignature() {
        String description = GeoServerJwtHeadersFilter.describeHeaderValue("Bearer " + jwt("{\"sub\":\"s-1\"}"));

        assertTrue(description, description.contains("RS256"));
        assertTrue(description, description.contains("s-1"));
        assertFalse(description, description.contains("SIGNATURE-SEGMENT"));
    }

    @Test
    public void testEncryptedTokenNotShown() {
        assertEquals(
                "an encrypted token (not shown)",
                GeoServerJwtHeadersFilter.describeHeaderValue("eyJhbGciOiJSU0EtT0FFUCJ9.a2V5.aXY.Y2lwaGVy.dGFn"));
    }

    @Test
    public void testOtherValuesShownAsIs() {
        assertEquals("no value", GeoServerJwtHeadersFilter.describeHeaderValue(null));
        assertEquals("alice", GeoServerJwtHeadersFilter.describeHeaderValue("alice"));
        String json = "{\"email\":\"first.last@example.org\"}";
        assertEquals(json, GeoServerJwtHeadersFilter.describeHeaderValue(json));
    }

    @Test
    public void testClaimsCannotBreakTheLogLine() {
        String description = GeoServerJwtHeadersFilter.describeHeaderValue(jwt("{\"sub\":\"a\\n01 Jan forged line\"}"));
        assertFalse(description, description.contains("\n"));

        String printable = GeoServerJwtHeadersFilter.printable("first\nsecond\r\u0007");
        assertEquals("first\\nsecond\\r\\u0007", printable);
    }

    @Test
    public void testLongLinesShortened() {
        String printable = GeoServerJwtHeadersFilter.printable("x".repeat(5000));
        assertTrue(printable.endsWith("... (5000 characters)"));
        assertTrue(printable.length() < 4100);
    }
}
