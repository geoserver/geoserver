/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * The range parser drives CVE-applicability decisions, so this corpus is built from the real
 * {@code vulnerable_versions} strings of the live feed ({@code vulnerable-versions-corpus.txt}: 43 distinct strings
 * from 33 advisories) plus a case for every operator, bound and failure mode in the specification.
 */
public class VersionRangeTest {

    private static VersionRange range(String text) {
        return VersionRange.parse(text);
    }

    private static boolean matches(String range, String version) {
        VersionRange r = range(range);
        assertTrue("should be parseable: " + range, r.isParseable());
        return r.matches(Version.parse(version));
    }

    // ---- real strings from the feed --------------------------------------------------------------------------

    @Test
    public void realCorpusIsParseableExceptTheOneRangeThatCanNeverMatch() throws Exception {
        List<String> unparseable = new ArrayList<>();
        int total = 0;
        try (InputStream in = getClass().getResourceAsStream("vulnerable-versions-corpus.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                total++;
                if (!range(line).isParseable()) {
                    unparseable.add(line);
                }
            }
        }
        assertEquals(43, total);
        assertEquals(Arrays.asList(">2.23.5, <2.25.0, >=2.25.3"), unparseable);
    }

    @Test
    public void ghsaJvpx6qxgWhgcRows() {
        // the three real rows of GHSA-jvpx-6qxg-whgc: exact version, open-ended lower bound, and an empty set
        assertTrue(matches("3.0.0", "3.0.0"));
        assertFalse(matches("3.0.0", "3.0.1"));
        assertTrue(matches(">=2.28.0", "2.28.0"));
        assertTrue(matches(">=2.28.0", "3.0.1"));
        assertFalse(matches(">=2.28.0", "2.27.6"));

        VersionRange never = range(">2.23.5, <2.25.0, >=2.25.3");
        assertFalse(never.isParseable());
        assertFalse(never.matches(Version.parse("2.27.5")));
    }

    // ---- operators and bounds --------------------------------------------------------------------------------

    @Test
    public void eachOperatorAtAndAroundItsBound() {
        assertTrue(matches("< 2.23.5", "2.23.4"));
        assertFalse(matches("< 2.23.5", "2.23.5"));
        assertFalse(matches("< 2.23.5", "2.23.6"));

        assertTrue(matches("<= 2.26.3", "2.26.3"));
        assertTrue(matches("<= 2.26.3", "2.26.2"));
        assertFalse(matches("<= 2.26.3", "2.26.4"));

        assertTrue(matches("> 2.0.0", "2.0.1"));
        assertFalse(matches("> 2.0.0", "2.0.0"));

        assertTrue(matches(">= 2.0.0", "2.0.0"));
        assertFalse(matches(">= 2.0.0", "1.99.99"));

        assertTrue(matches("= 2.27.0", "2.27.0"));
        assertFalse(matches("= 2.27.0", "2.27.1"));
    }

    @Test
    public void bareVersionMeansExactMatch() {
        assertTrue(matches("2.27.0", "2.27.0"));
        assertFalse(matches("2.27.0", "2.26.9"));
        assertFalse(matches("2.27.0", "2.27.1"));
    }

    @Test
    public void commaSeparatedClausesAreAnded() {
        assertTrue(matches(">= 2.24.0, < 2.24.4", "2.24.0"));
        assertTrue(matches(">= 2.24.0, < 2.24.4", "2.24.3"));
        assertFalse(matches(">= 2.24.0, < 2.24.4", "2.24.4"));
        assertFalse(matches(">= 2.24.0, < 2.24.4", "2.23.9"));
    }

    @Test
    public void allVersionsRange() {
        assertTrue(matches(">= 1.0.0", "1.0.0"));
        assertTrue(matches(">= 1.0.0", "3.1.0"));
        assertFalse(matches(">= 1.0.0", "0.9.9"));
    }

    @Test
    public void whitespaceVariants() {
        assertTrue(matches(">=2.28.0", "2.28.0"));
        assertTrue(matches(">= 2.28.0", "2.28.0"));
        assertTrue(matches("  >=   2.28.0  ", "2.28.0"));
        assertTrue(matches(">=2.0.0,<2.24.4", "2.10.0"));
        assertTrue(matches(">=2.0.0 ,  < 2.24.4", "2.10.0"));
        assertTrue(matches(" 2.27.0 ", "2.27.0"));
    }

    @Test
    public void numericOrderingNotTextOrdering() {
        // as text, "2.9.0" > "2.10.0"; numerically 2.9.0 < 2.10.0
        assertTrue(matches("< 2.10.0", "2.9.0"));
        assertFalse(matches("< 2.9.0", "2.10.0"));
        assertTrue(matches(">= 2.9.0, < 2.10.0", "2.9.5"));
        assertFalse(matches(">= 2.9.0, < 2.10.0", "2.10.0"));
        assertTrue(matches("> 2.9.9", "2.10.0"));
    }

    @Test
    public void boundaryOfEveryClauseInACompoundRange() {
        String r = ">2.10.0,<2.24.4";
        assertFalse(matches(r, "2.10.0"));
        assertTrue(matches(r, "2.10.1"));
        assertTrue(matches(r, "2.24.3"));
        assertFalse(matches(r, "2.24.4"));
    }

    @Test
    public void redundantClausesStillParse() {
        // the real feed has "< 2.20.4, <2.19.6": two upper bounds, the tighter one wins
        assertTrue(matches("< 2.20.4, <2.19.6", "2.19.5"));
        assertFalse(matches("< 2.20.4, <2.19.6", "2.19.6"));
        assertFalse(matches("< 2.20.4, <2.19.6", "2.20.0"));
    }

    // ---- ranges that must be reported unparseable ------------------------------------------------------------

    private static void assertUnparseable(String text) {
        VersionRange r = range(text);
        assertFalse("should be unparseable: '" + text + "'", r.isParseable());
        assertFalse(r.matches(Version.parse("2.27.0")));
    }

    @Test
    public void commitShaIsUnparseable() {
        assertUnparseable("df11a650c650b2b5a73fe6c6b0e6d0d8a6e2a8c1");
        assertUnparseable("df11a650c650...");
    }

    @Test
    public void emptyAndMalformedStringsAreUnparseable() {
        assertUnparseable(null);
        assertUnparseable("");
        assertUnparseable("   ");
        assertUnparseable(",");
        assertUnparseable(">= 2.0.0,");
        assertUnparseable(", < 3.0.0");
        assertUnparseable(">=");
        assertUnparseable("2.27");
        assertUnparseable("2.27.x");
        assertUnparseable("2.27.0-SNAPSHOT");
        assertUnparseable("v2.27.0");
        assertUnparseable("== 2.27.0");
        assertUnparseable("~2.27.0");
        assertUnparseable("^2.27.0");
        assertUnparseable("* ");
        assertUnparseable(">= 2.0.0 || < 1.0.0");
        assertUnparseable("> = 2.0.0");
        assertUnparseable(">= 2.0.0 < 3.0.0");
        assertUnparseable("not a version");
    }

    @Test
    public void tooManyClausesIsUnparseable() {
        StringBuilder text = new StringBuilder(">=1.0.0");
        for (int i = 0; i < 30; i++) {
            text.append(",<9.0.0");
        }
        assertUnparseable(text.toString());
    }

    @Test
    public void rangesThatCanNeverMatchAreUnparseable() {
        assertUnparseable(">2.23.5, <2.25.0, >=2.25.3");
        assertUnparseable(">3.0.0, <2.0.0");
        assertUnparseable(">2.0.0, <2.0.0");
        assertUnparseable(">=2.0.0, <2.0.0");
        assertUnparseable(">2.0.0, <=2.0.0");
        assertUnparseable("= 2.0.0, = 3.0.0");
        assertUnparseable("= 2.0.0, > 2.0.0");
        assertUnparseable("2.5.0, < 2.5.0");
    }

    @Test
    public void rangesThatTouchAtOneVersionCanMatch() {
        assertTrue(matches(">=2.0.0, <=2.0.0", "2.0.0"));
        assertTrue(matches("= 2.0.0, >= 2.0.0", "2.0.0"));
        assertTrue(matches("2.0.0, <= 2.0.0", "2.0.0"));
    }
}
