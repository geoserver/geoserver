/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.net.URI;
import org.geoserver.ows.KvpParser;

/**
 * Parse URI key value pair parameter of the form <code>'key=&lt;uri&gt;'</code>.
 *
 * <p>This implementation should be used to reference external resource, using {@link URI#toURL()}
 * to obtain URL when connecting.
 */
public class URIKvpParser extends KvpParser {

    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public URIKvpParser(String key) {
        super(key, URI.class);
    }

    @Override
    public URI parse(String value) throws Exception {
        try {
            return new URI(value);
        } catch (Exception t) {
            return URI.create(uriEncode(value));
        }
    }

    /**
     * URLEncoder.encode does not respect the URI RFC 2396, so we rolled our own little encoder.
     * It's not complete, but should work in most cases.
     *
     * <p>Use of URIKvpParser recommended as a direct implementation of RFC 2396.
     *
     * @param uri String representation of uri
     * @return URL with fixes applied for URI RFC 2396
     */
    public static String uriEncode(String uri) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);

            // From URI RFC, "Only alphanumerics [0-9a-zA-Z], the special
            // characters "$-_.+!*'(),", and reserved characters used
            // for their reserved purposes may be used unencoded within a URL
            // Here we keep all the good ones, and remove the few unneeded in their
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
