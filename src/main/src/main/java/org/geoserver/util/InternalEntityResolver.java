/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.geoserver.config.GeoServer;
import org.geotools.util.logging.Logging;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * Internal EntityResolver allowing connections to GeoServer contents, and OGC / W3C content.
 *
 * @author Jody Garnett (GeoCat)
 */
public class InternalEntityResolver implements EntityResolver2, Serializable {

    /** Prefix used for SAXException message */
    public static final String ERROR_MESSAGE_BASE = "Entity resolution disallowed for ";

    protected static final Logger LOGGER = Logging.getLogger(InternalEntityResolver.class);

    /** Internal uri references */
    private static final Pattern INTERNAL_URIS = Pattern.compile("(?i)(jar:file|vfs)[^?#;]*\\.xsd");

    /** OGC Schema references */
    private static final Pattern OGC_URIS =
            Pattern.compile(
                    "(?i)(http|https)://(schemas.opengis.net|www.opengis.net)/[^?#;]*\\.xsd");

    /** W3C Schema references */
    private static final Pattern W3C_URIS =
            Pattern.compile("(?i)(http|https)://(www.w3.org)/[^?#;]*\\.xsd");

    /** Base URL from request used to identify internal http(s) references. */
    private final String baseURL;

    /** GeoServer used to identify internal http(s) references as provided by proxy base. */
    private final GeoServer geoServer;

    /**
     * InternalEntityResolver willing to resolve commong ogc and w3c entities, and those relative to
     * GeoServer proxy base url.
     *
     * @param geoServer Used to obtain settings for proxy base url
     */
    public InternalEntityResolver(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.baseURL = null;
    }

    /**
     * InternalEntityResolver willing to resolve commong ogc and w3c entities, and those relative a
     * base url.
     *
     * @param geoServer Used to obtain settings for proxy base url
     * @param baseURL Base url provided by current request
     */
    public InternalEntityResolver(GeoServer geoServer, String baseURL) {
        this.geoServer = geoServer;
        this.baseURL = baseURL;
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        return resolveEntity(null, publicId, null, systemId);
    }

    @Override
    public InputSource getExternalSubset(String name, String baseURI)
            throws SAXException, IOException {
        return resolveEntity(name, null, baseURI, null);
    }

    @Override
    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId)
            throws SAXException, IOException {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(
                    String.format(
                            "resolveEntity request: name=%s, publicId=%s, baseURI=%s, systemId=%s",
                            name, publicId, baseURI, systemId));
        }

        try {
            String uri;
            if (URI.create(systemId).isAbsolute()) {
                uri = systemId;
            } else {
                // use the baseURI to convert a relative systemId to absolute
                if (baseURI == null) {
                    throw new SAXException(ERROR_MESSAGE_BASE + systemId);
                }
                if ((baseURI.endsWith(".xsd") || baseURI.endsWith(".XSD"))
                        && baseURI.lastIndexOf('/') != -1) {
                    uri = baseURI.substring(0, baseURI.lastIndexOf('/')) + '/' + systemId;
                } else {
                    uri = baseURI + '/' + systemId;
                }
                // double check this is the same result as with URL
                String check = new URL(new URL(baseURI), systemId).toString();
                if (!uri.equals(check)) {
                    uri = check;
                }
            }
            // check if the absolute systemId is an allowed URI jar or vfs reference
            if (INTERNAL_URIS.matcher(uri).matches()) {
                return null;
            }
            // Allow select external locations
            if (OGC_URIS.matcher(uri).matches() || W3C_URIS.matcher(uri).matches()) {
                return null;
            }

            String uri_lowercase = uri.toLowerCase();
            if (geoServer != null) {
                final String PROXY_BASE = geoServer.getSettings().getProxyBaseUrl();
                if (PROXY_BASE != null && uri_lowercase.startsWith(PROXY_BASE.toLowerCase())) {
                    return null;
                }
            }
            if (baseURL != null && uri_lowercase.startsWith(baseURL.toLowerCase())) {
                return null;
            }
        } catch (Exception e) {
            // do nothing
        }

        // do not allow external entities
        throw new SAXException(ERROR_MESSAGE_BASE + systemId);
    }
}
