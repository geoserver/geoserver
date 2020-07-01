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
 */
public class URLKvpParser extends KvpParser {

    private final Boolean FIX_URL_FIRST = Boolean.getBoolean("org.geoserver.kvp.urlfix");
    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public URLKvpParser(String key) {
        super(key, URL.class);
    }

    public Object parse(String value) throws Exception {
        if (FIX_URL_FIRST) return new URL(fixURL(value));
        else {
            try {
                return new URL(value);
            } catch (MalformedURLException e) {
                return new URL(fixURL(value));
            }
        }
    }

    /**
     * URLEncoder.encode does not respect the RFC 2396, so we rolled our own little encoder. It's
     * not complete, but should work in most cases
     */
    public static String fixURL(String url) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);

            // From RFC, "Only alphanumerics [0-9a-zA-Z], the special
            // characters "$-_.+!*'(),", and reserved characters used
            // for their reserved purposes may be used unencoded within a URL
            // Here we keep all the good ones, and remove the few uneeded in their
            // ascii range. We also keep / and : to make sure basic URL elements
            // don't get encoded
            if ((c > ' ') && (c < '{') && ("\"\\<>%^[]`+$,".indexOf(c) == -1)) {
                sb.append(c);
            } else {
                sb.append("%").append(Integer.toHexString(c));
            }
        }

        return sb.toString();
    }
}
