/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KvpMapTest {

    @Test
    public void testCaseInsensitive() {
        KvpMap map = new KvpMap();
        map.put("foo", "bar");

        assertEquals("bar", map.get("FOO"));
        assertEquals("bar", map.get("foo"));
        assertEquals("bar", map.getOrDefault("foo", null));
        assertEquals("bar", map.getOrDefault("FOO", null));
    }
}
