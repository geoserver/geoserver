/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.geoserver.ows.util.KvpUtils;

public class KvpUtilsTest extends TestCase {
    public void testEmptyString() {
        assertEquals(0, KvpUtils.readFlat("").size());
    }

    public void testTrailingEmtpyStrings() {
        assertEquals(
                Arrays.asList(new String[] {"x", "", "x", "", ""}), KvpUtils.readFlat("x,,x,,"));
    }

    public void testEmtpyNestedString() {
        List result = KvpUtils.readNested("");
        assertEquals(1, result.size());
        assertEquals(0, ((List) result.get(0)).size());
    }

    public void testStarNestedString() {
        List result = KvpUtils.readNested("*");
        assertEquals(1, result.size());
        assertEquals(0, ((List) result.get(0)).size());
    }

    public void testWellKnownTokenizers() {
        String[] expected;
        List actual;

        expected = new String[] {"1", "2", "3", ""};
        actual = KvpUtils.readFlat("1,2,3,", KvpUtils.INNER_DELIMETER);
        assertKvp(expected, actual);

        expected = new String[] {"abc", "def", ""};
        actual = KvpUtils.readFlat("(abc)(def)()", KvpUtils.OUTER_DELIMETER);
        assertKvp(expected, actual);

        expected = new String[] {"abc"};
        actual = KvpUtils.readFlat("(abc)", KvpUtils.OUTER_DELIMETER);
        assertKvp(expected, actual);

        expected = new String[] {""};
        actual = KvpUtils.readFlat("()", KvpUtils.OUTER_DELIMETER);
        assertKvp(expected, actual);

        expected = new String[] {"", "A=1", "B=2", ""};
        actual = KvpUtils.readFlat(";A=1;B=2;", KvpUtils.CQL_DELIMITER);
        assertKvp(expected, actual);

        expected = new String[] {"ab", "cd", "ef", ""};
        actual = KvpUtils.readFlat("ab&cd&ef&", KvpUtils.KEYWORD_DELIMITER);
        assertKvp(expected, actual);

        expected = new String[] {"A", "1 "};
        actual = KvpUtils.readFlat("A=1 ", KvpUtils.VALUE_DELIMITER);
        assertKvp(expected, actual);
    }

    public void testRadFlatUnkownDelimiter() {
        List actual;

        final String[] expected = new String[] {"1", "2", "3", ""};
        actual = KvpUtils.readFlat("1^2^3^", "\\^");
        assertKvp(expected, actual);

        actual = KvpUtils.readFlat("1-2-3-", "-");
        assertKvp(expected, actual);
    }

    private void assertKvp(String[] expected, List actual) {
        List expectedList = Arrays.asList(expected);
        assertEquals(expectedList.size(), actual.size());
        assertEquals(expectedList, actual);
    }

    public void testEscapedTokens() {
        // test trivial scenarios
        List<String> actual = KvpUtils.escapedTokens("", ',');
        assertEquals(Arrays.asList(""), actual);

        actual = KvpUtils.escapedTokens(",", ',');
        assertEquals(Arrays.asList("", ""), actual);

        actual = KvpUtils.escapedTokens("a,b", ',');
        assertEquals(Arrays.asList("a", "b"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',');
        assertEquals(Arrays.asList("a", "b", "c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 2);
        assertEquals(Arrays.asList("a", "b,c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 1);
        assertEquals(Arrays.asList("a,b,c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 0);
        assertEquals(Arrays.asList("a", "b", "c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 1000);
        assertEquals(Arrays.asList("a", "b", "c"), actual);

        // test escaped data
        actual = KvpUtils.escapedTokens("\\\\,\\\\", ',');
        assertEquals(Arrays.asList("\\\\", "\\\\"), actual);

        actual = KvpUtils.escapedTokens("a\\,b,c", ',');
        assertEquals(Arrays.asList("a\\,b", "c"), actual);

        actual = KvpUtils.escapedTokens("a\\,b,c,d", ',', 2);
        assertEquals(Arrays.asList("a\\,b", "c,d"), actual);

        // test error conditions
        try {
            KvpUtils.escapedTokens(null, ',');
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {;
        }

        try {
            KvpUtils.escapedTokens("", '\\');
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {;
        }

        try {
            KvpUtils.escapedTokens("\\", '\\');
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {;
        }
    }

    public static void testUnescape() {
        // test trivial scenarios
        String actual = KvpUtils.unescape("abc");
        assertEquals("abc", actual);

        // test escape sequences
        actual = KvpUtils.unescape("abc\\\\");
        assertEquals("abc\\", actual);

        actual = KvpUtils.unescape("abc\\d");
        assertEquals("abcd", actual);

        // test error conditions
        try {
            KvpUtils.unescape(null);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {;
        }

        try {
            KvpUtils.unescape("\\");
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {;
        }
    }

    public static void testParseQueryString() {
        Map<String, Object> kvp =
                KvpUtils.parseQueryString(
                        "geoserver?request=WMS&version=1.0.0&CQL_FILTER=NAME='geoserver'");
        assertEquals(3, kvp.size());
        assertEquals("WMS", kvp.get("request"));
        assertEquals("1.0.0", kvp.get("version"));
        assertEquals("NAME='geoserver'", kvp.get("CQL_FILTER"));
    }

    public static void testParseQueryStringRepeated() {
        Map<String, Object> kvp =
                KvpUtils.parseQueryString(
                        "geoserver?request=WMS&version=1.0.0&version=2.0.0&CQL_FILTER=NAME='geoserver'");
        assertEquals(3, kvp.size());
        assertEquals("WMS", kvp.get("request"));
        assertTrue(Arrays.equals(new String[] {"1.0.0", "2.0.0"}, (String[]) kvp.get("version")));
        assertEquals("NAME='geoserver'", kvp.get("CQL_FILTER"));
    }
}
