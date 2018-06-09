/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

/**
 * Test for the format options / SQL View parameters in a request.
 *
 * @author Robert Coup
 */
public class FormatOptionsKvpParserTest extends TestCase {

    private FormatOptionsKvpParser parser;

    @Override
    protected void setUp() throws Exception {
        parser = new FormatOptionsKvpParser();
    }

    /**
     * Tests normal-style format options
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testPairs() throws Exception {
        Map<String, String> expected =
                new HashMap<String, String>() {
                    {
                        put("key1", "value1");
                        put("key2", "value2");
                        put("key3", "true");
                        put("key4", "value4");
                    }
                };

        Map<String, String> actual =
                (Map<String, String>) parser.parse("key1:value1;key2:value2;key3;key4:value4");
        assertEquals(expected, actual);
    }

    /**
     * Tests format options with escaped separators
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testEscapedSeparators() throws Exception {
        Map<String, String> expected =
                new HashMap<String, String>() {
                    {
                        put("key1", "value:1");
                        put("key2", "value:2");
                        put("key3", "value:3;ZZZ");
                    }
                };

        Map<String, String> actual =
                (Map<String, String>)
                        parser.parse("key1:value\\:1;key2:value\\:2;key3:value\\:3\\;ZZZ");
        assertEquals(expected, actual);
    }

    /**
     * Tests format options with embedded separators
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testEmbeddedSeparators() throws Exception {
        Map<String, String> expected =
                new HashMap<String, String>() {
                    {
                        put("key1", "value:1");
                        put("key2", "value:2");
                        put("key3", "value:3:ZZ;XX");
                    }
                };

        Map<String, String> actual =
                (Map<String, String>)
                        parser.parse("key1:value:1;key2:value:2;key3:value:3\\:ZZ\\;XX");
        assertEquals(expected, actual);
    }

    /**
     * Tests format options with embedded separators
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testErrors() throws Exception {
        Map<String, String> expected =
                new HashMap<String, String>() {
                    {
                        put("key1", "value:1");
                        put("key2", "value:2");
                        put("key3", "value:3");
                    }
                };

        Map<String, String> actual =
                (Map<String, String>) parser.parse("key1:value:1;key2:value:2;key3:value:3");
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }
}
