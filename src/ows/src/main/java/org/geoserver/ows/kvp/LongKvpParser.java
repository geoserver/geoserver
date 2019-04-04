/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import org.geoserver.ows.KvpParser;

/** Parses long kvp's of the form 'key=&lt;long&gt;'. */
public class LongKvpParser extends KvpParser {
    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public LongKvpParser(String key) {
        super(key, Long.class);
    }

    public Object parse(String value) throws Exception {
        return Long.valueOf(value);
    }
}
