/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class SuppressionTest {

    private static Suppression parse(String text, List<String> rejected) {
        return Suppression.parse(text, rejected);
    }

    @Test
    public void idsAreSplitOnCommasSemicolonsAndWhiteSpace() {
        List<String> rejected = new ArrayList<>();
        Suppression s = parse(" CVE-2026-0002,CVE-2026-0001;\tGHSA-aaaa-bbbb-cccc\nCVE-2026-0001 ", rejected);
        assertEquals("CVE-2026-0001, CVE-2026-0002, GHSA-aaaa-bbbb-cccc", s.toString());
        assertTrue(rejected.toString(), rejected.isEmpty());
    }

    @Test
    public void idsAreWrittenInTheFormTheFeedUses() {
        Suppression s = parse("cve-2026-0001 ghsa-AAAA-bbbb-CCCC", new ArrayList<>());
        assertEquals("CVE-2026-0001, GHSA-aaaa-bbbb-cccc", s.toString());
    }

    @Test
    public void nothingOrBlankSuppressesNothing() {
        for (String text : new String[] {null, "", "  \n "}) {
            assertSame(text, Suppression.none(), parse(text, new ArrayList<>()));
        }
        assertTrue(Suppression.none().isEmpty());
    }

    @Test
    public void whatIsNotAnIdIsRejectedAndTheRestKept() {
        List<String> rejected = new ArrayList<>();
        Suppression s = parse("CVE-2026-0001 nonsense CVE-26-1 GHSA-xyz bad\nline", rejected);
        assertEquals("CVE-2026-0001", s.toString());
        assertEquals(List.of("nonsense", "CVE-26-1", "GHSA-xyz", "bad", "line"), rejected);
    }

    @Test
    public void rejectedTextIsSafeToLog() {
        List<String> rejected = new ArrayList<>();
        Suppression.of(List.of("bad forged line"), rejected);
        assertEquals(List.of("bad forged line"), rejected);
    }

    @Test
    public void theListIsCapped() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < Suppression.MAX_IDS + 5; i++) {
            tokens.add(String.format("CVE-2026-%04d", 1000 + i));
        }
        List<String> rejected = new ArrayList<>();
        Suppression s = Suppression.of(tokens, rejected);
        assertEquals(Suppression.MAX_IDS, s.toString().split(", ").length);
        assertEquals(5, rejected.size());
    }

    // ---- the REST body --------------------------------------------------------------------------------------------

    @Test
    public void aBodyWithoutAListSuppliesNone() {
        for (String body : new String[] {null, "", "  ", "{}", "{\"other\": 1}", "{\"suppressedCves\": null}"}) {
            assertNull(body, Suppression.fromRequest(body));
        }
    }

    @Test
    public void anEmptyListIsSuppliedAndSuppressesNothing() {
        Suppression s = Suppression.fromRequest("{\"suppressedCves\": []}");
        assertTrue(s.isEmpty());
    }

    @Test
    public void aListIsReadLikeTheProperty() {
        Suppression s = Suppression.fromRequest("{\"suppressedCves\": [\"cve-2026-0001\", \"GHSA-aaaa-bbbb-cccc\"]}");
        assertEquals("CVE-2026-0001, GHSA-aaaa-bbbb-cccc", s.toString());
    }

    @Test
    public void aBadBodyIsRefusedWithAMessage() {
        String[] bad = {
            "not json",
            "[1]",
            "{\"suppressedCves\": \"CVE-2026-0001\"}",
            "{\"suppressedCves\": [1]}",
            "{\"suppressedCves\": [\"CVE-2026-0001\", null]}",
            "{\"suppressedCves\": [\"nonsense\"]}",
            "{\"suppressedCves\": [\"CVE-2026-0001\"]} trailing"
        };
        for (String body : bad) {
            IllegalArgumentException e =
                    assertThrows(body, IllegalArgumentException.class, () -> Suppression.fromRequest(body));
            assertFalse(e.getMessage(), e.getMessage().isEmpty());
        }
    }
}
