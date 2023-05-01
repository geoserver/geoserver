/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.net.MalformedURLException;
import java.net.URL;
import org.geoserver.ows.KvpParser;

/**
 * Parses url kvp's of the form 'key=&lt;url&gt;'.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @deprecated Use URIKvpParser
 */
public class URLKvpParser extends KvpParser {
    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public URLKvpParser(String key) {
        super(key, URL.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            return new URL(fixURL(value));
        }
    }

    /**
     * URLEncoder.encode does not respect the URI RFC 2396, so we rolled our own little encoder.
     * It's not complete, but should work in most cases.
     *
     * <p>Use of URIKvpParser recommended as a direct implementation of RFC 2396.
     *
     * @param url String representation of url
     * @return URL with fixes applied for URI RFC 2396
     */
    public static String fixURL(String url) {
        return URIKvpParser.uriEncode(url);
    }
}
