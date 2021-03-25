/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.junit.Assert;
import org.junit.Test;

public class KvpUtilsTest {
    @Test
    public void testEmptyString() {
        Assert.assertEquals(0, KvpUtils.readFlat("").size());
    }

    @Test
    public void testTrailingEmtpyStrings() {
        Assert.assertEquals(
                Arrays.asList(new String[] {"x", "", "x", "", ""}), KvpUtils.readFlat("x,,x,,"));
    }

    @Test
    public void testEmtpyNestedString() {
        List result = KvpUtils.readNested("");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(0, ((List) result.get(0)).size());
    }

    @Test
    public void testNullKvp() {
        KvpMap<String, String> result = KvpUtils.toStringKVP(null);
        Assert.assertNull(result);
    }

    @Test
    public void testStarNestedString() {
        List result = KvpUtils.readNested("*");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(0, ((List) result.get(0)).size());
    }

    @Test
    public void testWellKnownTokenizers() {

        String[] expected = {"1", "2", "3", ""};
        List actual = KvpUtils.readFlat("1,2,3,", KvpUtils.INNER_DELIMETER);
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

    @Test
    public void testRadFlatUnkownDelimiter() {

        final String[] expected = {"1", "2", "3", ""};
        List actual = KvpUtils.readFlat("1^2^3^", "\\^");
        assertKvp(expected, actual);

        actual = KvpUtils.readFlat("1-2-3-", "-");
        assertKvp(expected, actual);
    }

    private void assertKvp(String[] expected, List actual) {
        List expectedList = Arrays.asList(expected);
        Assert.assertEquals(expectedList.size(), actual.size());
        Assert.assertEquals(expectedList, actual);
    }

    @Test
    public void testEscapedTokens() {
        // test trivial scenarios
        List<String> actual = KvpUtils.escapedTokens("", ',');
        Assert.assertEquals(Arrays.asList(""), actual);

        actual = KvpUtils.escapedTokens(",", ',');
        Assert.assertEquals(Arrays.asList("", ""), actual);

        actual = KvpUtils.escapedTokens("a,b", ',');
        Assert.assertEquals(Arrays.asList("a", "b"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',');
        Assert.assertEquals(Arrays.asList("a", "b", "c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 2);
        Assert.assertEquals(Arrays.asList("a", "b,c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 1);
        Assert.assertEquals(Arrays.asList("a,b,c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 0);
        Assert.assertEquals(Arrays.asList("a", "b", "c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 1000);
        Assert.assertEquals(Arrays.asList("a", "b", "c"), actual);

        // test escaped data
        actual = KvpUtils.escapedTokens("\\\\,\\\\", ',');
        Assert.assertEquals(Arrays.asList("\\\\", "\\\\"), actual);

        actual = KvpUtils.escapedTokens("a\\,b,c", ',');
        Assert.assertEquals(Arrays.asList("a\\,b", "c"), actual);

        actual = KvpUtils.escapedTokens("a\\,b,c,d", ',', 2);
        Assert.assertEquals(Arrays.asList("a\\,b", "c,d"), actual);

        // test error conditions
        try {
            KvpUtils.escapedTokens(null, ',');
            Assert.fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }

        try {
            KvpUtils.escapedTokens("", '\\');
            Assert.fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }

        try {
            KvpUtils.escapedTokens("\\", '\\');
            Assert.fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testUnescape() {
        // test trivial scenarios
        String actual = KvpUtils.unescape("abc");
        Assert.assertEquals("abc", actual);

        // test escape sequences
        actual = KvpUtils.unescape("abc\\\\");
        Assert.assertEquals("abc\\", actual);

        actual = KvpUtils.unescape("abc\\d");
        Assert.assertEquals("abcd", actual);

        // test error conditions
        try {
            KvpUtils.unescape(null);
            Assert.fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }

        try {
            KvpUtils.unescape("\\");
            Assert.fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testParseQueryString() {
        Map<String, Object> kvp =
                KvpUtils.parseQueryString(
                        "geoserver?request=WMS&version=1.0.0&CQL_FILTER=NAME='geoserver'");
        Assert.assertEquals(3, kvp.size());
        Assert.assertEquals("WMS", kvp.get("request"));
        Assert.assertEquals("1.0.0", kvp.get("version"));
        Assert.assertEquals("NAME='geoserver'", kvp.get("CQL_FILTER"));
    }

    @Test
    public void testParseQueryStringRepeated() {
        Map<String, Object> kvp =
                KvpUtils.parseQueryString(
                        "geoserver?request=WMS&version=1.0.0&version=2.0.0&CQL_FILTER=NAME"
                                + "='geoserver'");
        Assert.assertEquals(3, kvp.size());
        Assert.assertEquals("WMS", kvp.get("request"));
        assertArrayEquals(new String[] {"1.0.0", "2.0.0"}, (String[]) kvp.get("version"));
        Assert.assertEquals("NAME='geoserver'", kvp.get("CQL_FILTER"));
    }
}
