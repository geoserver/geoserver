/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import static java.util.Map.entry;

import java.text.ParseException;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the format options / SQL View parameters in a request.
 *
 * @author Robert Coup
 */
public class FormatOptionsKvpParserTest {

    private FormatOptionsKvpParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new FormatOptionsKvpParser();
    }

    /**
     * Tests normal-style format options
     *
     * @throws ParseException if the string can't be parsed.
     */
    @Test
    public void testPairs() throws Exception {
        Map<String, String> expected =
                Map.ofEntries(
                        entry("key1", "value1"),
                        entry("key2", "value2"),
                        entry("key3", "true"),
                        entry("key4", "value4"));

        @SuppressWarnings("unchecked")
        Map<String, String> actual =
                (Map<String, String>) parser.parse("key1:value1;key2:value2;key3;key4:value4");
        Assert.assertEquals(expected, actual);
    }

    /**
     * Tests format options with escaped separators
     *
     * @throws ParseException if the string can't be parsed.
     */
    @Test
    public void testEscapedSeparators() throws Exception {
        Map<String, String> expected =
                Map.ofEntries(
                        entry("key1", "value:1"),
                        entry("key2", "value:2"),
                        entry("key3", "value:3;ZZZ"));

        @SuppressWarnings("unchecked")
        Map<String, String> actual =
                (Map<String, String>)
                        parser.parse("key1:value\\:1;key2:value\\:2;key3:value\\:3\\;ZZZ");
        Assert.assertEquals(expected, actual);
    }

    /**
     * Tests format options with embedded separators
     *
     * @throws ParseException if the string can't be parsed.
     */
    @Test
    public void testEmbeddedSeparators() throws Exception {
        Map<String, String> expected =
                Map.ofEntries(
                        entry("key1", "value:1"),
                        entry("key2", "value:2"),
                        entry("key3", "value:3:ZZ;XX"));

        @SuppressWarnings("unchecked")
        Map<String, String> actual =
                (Map<String, String>)
                        parser.parse("key1:value:1;key2:value:2;key3:value:3\\:ZZ\\;XX");
        Assert.assertEquals(expected, actual);
    }

    /**
     * Tests format options with embedded separators
     *
     * @throws ParseException if the string can't be parsed.
     */
    @Test
    public void testErrors() throws Exception {
        Map<String, String> expected =
                Map.ofEntries(
                        entry("key1", "value:1"),
                        entry("key2", "value:2"),
                        entry("key3", "value:3"));

        @SuppressWarnings("unchecked")
        Map<String, String> actual =
                (Map<String, String>) parser.parse("key1:value:1;key2:value:2;key3:value:3");
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertEquals(expected, actual);
    }
}
