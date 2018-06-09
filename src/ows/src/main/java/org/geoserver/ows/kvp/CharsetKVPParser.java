/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.nio.charset.Charset;
import org.geoserver.ows.KvpParser;

/**
 * Parses a charset name into a {@link Charset} object
 *
 * @author Andrea Aime - OpenGeo
 */
public class CharsetKVPParser extends KvpParser {

    public CharsetKVPParser(String key) {
        super(key, Charset.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        return Charset.forName(value);
    }
}
