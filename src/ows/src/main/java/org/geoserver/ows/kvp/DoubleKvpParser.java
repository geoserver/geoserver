/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import org.geoserver.ows.KvpParser;

/**
 * Parses double kvp's of the form 'key=&lt;double&gt;'.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class DoubleKvpParser extends KvpParser {
    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public DoubleKvpParser(String key) {
        super(key, Double.class);
    }

    public Object parse(String value) throws Exception {
        return Double.valueOf(value);
    }
}
