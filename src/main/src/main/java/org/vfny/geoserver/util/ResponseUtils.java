/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.util.logging.Logging;


public final class ResponseUtils {

    static Logger LOGGER = Logging.getLogger(ResponseUtils.class);

    /**
     * @deprecated moved to {@link org.geoserver.ows.util.ResponseUtils#encodeXML(String)}.
     */
    public static String encodeXML(String inData) {
        return org.geoserver.ows.util.ResponseUtils.encodeXML(inData);
    }

    /**
     * @deprecated moved to {@link org.geoserver.ows.util.ResponseUtils#writeEscapedString(Writer, String)}
     */
    public static void writeEscapedString(Writer writer, String string)
        throws IOException {
        org.geoserver.ows.util.ResponseUtils.writeEscapedString(writer, string);
    }

    /**
     * Profixies a metadata link url interpreting a localhost url as a back reference to the server.
     * <p>
     * If <tt>link</tt> is not a localhost url it is left untouched.
     * </p>
     */
    public static String proxifyMetadataLink(MetadataLinkInfo link, String baseURL) {
        String content = link.getContent();
        try {
            URI uri = new URI(content);
            try {
                if (uri.getHost() == null) {
                    //interpret no host as backreference to server
                    Map<String, String> kvp = null;
                    if (uri.getQuery() != null && !"".equals(uri.getQuery())) {
                        Map<String, Object> parsed = KvpUtils.parseQueryString("?" + uri.getQuery());
                        kvp = new HashMap<String, String>();
                        for (Entry<String, Object> entry : parsed.entrySet()) {
                            kvp.put(entry.getKey(), (String) entry.getValue());
                        }
                    }

                    content = buildURL(baseURL, uri.getPath(), kvp, URLType.RESOURCE);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        "Unable to create proper back referece for metadata url: "
                                + content, e);
            }
        } catch (URISyntaxException e) {
        }
        return content;
    }
}
