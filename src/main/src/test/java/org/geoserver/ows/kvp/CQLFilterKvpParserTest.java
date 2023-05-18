/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

public class CQLFilterKvpParserTest {

    private CQLFilterKvpParser parser;

    @Before
    public void setUp() {
        parser = new CQLFilterKvpParser();
    }

    @Test
    public void testNull() throws Exception {
        Object parsed = parser.parse(null);
        assertNull(parsed);
    }

    @Test
    public void testEmpty() throws Exception {
        Object parsed = parser.parse("");
        assertNull(parsed);
    }
}
