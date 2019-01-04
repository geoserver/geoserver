/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.kvp;

import static org.geoserver.opensearch.eo.kvp.SearchRequestKvpReader.*;
import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class RangePatternsTest {

    @Test
    public void testFullRangePattern() {
        // check matches
        assertFullRangeMatch("[10,20]", "[", "10", "20", "]");
        assertFullRangeMatch("]10,20[", "]", "10", "20", "[");
        assertFullRangeMatch("[2010-09-21,2010-09-22]", "[", "2010-09-21", "2010-09-22", "]");
        // check failures
        assertPatternNotMatch(FULL_RANGE_PATTERN, "abcd");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "10");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "10,20");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "2010-09-21");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "[10");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "10]");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "10,20,30");
    }

    private void assertFullRangeMatch(
            String testRange, String g1, String g2, String g3, String g4) {
        Matcher matcher = FULL_RANGE_PATTERN.matcher(testRange);
        assertTrue(matcher.matches());
        assertEquals(g1, matcher.group(1));
        assertEquals(g2, matcher.group(2));
        assertEquals(g3, matcher.group(3));
        assertEquals(g4, matcher.group(4));
    }

    private void assertPatternNotMatch(Pattern pattern, String testRange) {
        Matcher matcher = pattern.matcher(testRange);
        assertFalse(matcher.matches());
    }

    @Test
    public void testLeftRangePattern() {
        // check matches
        assertLeftRangeMatch("[10", "[", "10");
        assertLeftRangeMatch("]10", "]", "10");
        assertLeftRangeMatch("[2010-09-21", "[", "2010-09-21");

        // check failures
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "abcd");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10,20");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "2010-09-21");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "[10,20]");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10]");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10,20,30");
    }

    private void assertLeftRangeMatch(String testRange, String g1, String g2) {
        Matcher matcher = LEFT_RANGE_PATTERN.matcher(testRange);
        assertTrue(matcher.matches());
        assertEquals(g1, matcher.group(1));
        assertEquals(g2, matcher.group(2));
    }

    @Test
    public void testRightRangePattern() {
        // check matches
        assertRightRangeMatch("10]", "10", "]");
        assertRightRangeMatch("10[", "10", "[");
        assertRightRangeMatch("2010-09-21]", "2010-09-21", "]");

        // check failures
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "abcd");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10,20");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "2010-09-21");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "[10,20]");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10]");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10,20,30");
    }

    private void assertRightRangeMatch(String testRange, String g1, String g2) {
        Matcher matcher = RIGHT_RANGE_PATTERN.matcher(testRange);
        assertTrue(matcher.matches());
        assertEquals(g1, matcher.group(1));
        assertEquals(g2, matcher.group(2));
    }
}
