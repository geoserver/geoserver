/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.geoserver.config.GeoServer;
import org.geotools.util.logging.Logging;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * Restricted EntityResolver allowing connections to geoserver base proxy, and OGC / W3C content,
 * and those provided by GEOSERVER_ENTITY_RESOLUTION.
 *
 * @author Jody Garnett (GeoCat)
 */
public class AllowListEntityResolver implements EntityResolver2, Serializable {

    /** Location of Open Geospatical Consortium schemas for OGC OpenGIS standards */
    private static String OGC = "schemas.opengis.net|www.opengis.net";

    /**
     * Location of {@code http://inspire.ec.europa.eu/schemas/ } XSD documents for INSPIRE program
     */
    private static String INSPIRE = "inspire.ec.europa.eu/schemas";

    /** Location of W3C schema documents (for xlink, etc...) */
    private static String W3C = "www.w3.org";

    /** Prefix used for SAXException message */
    private static final String ERROR_MESSAGE_BASE = "Entity resolution disallowed for ";

    protected static final Logger LOGGER = Logging.getLogger(AllowListEntityResolver.class);

    /** Internal uri references */
    private static final Pattern INTERNAL_URIS = Pattern.compile("(?i)(jar:file|vfs)[^?#;]*\\.xsd");

    /** Allowed http(s) locations */
    private final Pattern ALLOWED_URIS;

    /** Base URL from request used to identify internal http(s) references. */
    private final String baseURL;

    /** GeoServer used to identify internal http(s) references as provided by proxy base. */
    private final GeoServer geoServer;

    /**
     * AllowListEntityResolver willing to resolve commong ogc and w3c entities, and those relative
     * to GeoServer proxy base url.
     *
     * @param geoServer Used to obtain settings for proxy base url
     */
    public AllowListEntityResolver(GeoServer geoServer) {
        this(geoServer, null);
    }

    /**
     * AllowListEntityResolver willing to resolve common ogc and w3c entities, and those relative a
     * base url.
     *
     * @param geoServer Used to obtain settings for proxy base url
     * @param baseURL Base url provided by current request
     */
    public AllowListEntityResolver(GeoServer geoServer, String baseURL) {
        this.geoServer = geoServer;
        this.baseURL = baseURL;
        if (EntityResolverProvider.ALLOW_LIST == null
                || EntityResolverProvider.ALLOW_LIST.length == 0) {
            // Restrict using the built-in allow list
            ALLOWED_URIS =
                    Pattern.compile(
                            "(?i)(http|https)://("
                                    + W3C
                                    + "|"
                                    + OGC
                                    + "|"
                                    + INSPIRE
                                    + ")/[^?#;]*\\.xsd");
        } else {
            StringBuilder pattern = new StringBuilder("(?i)(http|https)://(");
            pattern.append(W3C).append('|');
            pattern.append(OGC).append('|');
            pattern.append(INSPIRE);
            for (String allow : EntityResolverProvider.ALLOW_LIST) {
                pattern.append('|').append(allow);
            }
            pattern.append(")/[^?#;]*\\.xsd");
            String regex = pattern.toString();
            LOGGER.fine("ENTITY_RESOLUTION_ALLOWLIST processed:" + regex);

            ALLOWED_URIS = Pattern.compile(regex);
        }
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
            }
            // check if the absolute systemId is an allowed URI jar or vfs reference
            if (INTERNAL_URIS.matcher(uri).matches()) {
                LOGGER.finest("resolveEntity internal: " + uri);
                return null;
            }
            // Allow select external locations
            if (ALLOWED_URIS.matcher(uri).matches()) {
                LOGGER.finest("resolveEntity allowed: " + uri);
                return null;
            }

            String uri_lowercase = uri.toLowerCase();
            if (geoServer != null) {
                final String PROXY_BASE = geoServer.getSettings().getProxyBaseUrl();
                if (PROXY_BASE != null && uri_lowercase.startsWith(PROXY_BASE.toLowerCase())) {
                    LOGGER.finest("resolveEntity proxy base: " + uri);
                    return null;
                }
            }
            if (baseURL != null && uri_lowercase.startsWith(baseURL.toLowerCase())) {
                LOGGER.finest("resolveEntity proxy base: " + uri);
                return null;
            }
        } catch (Exception e) {
            // do nothing
        }

        // do not allow external entities
        throw new SAXException(ERROR_MESSAGE_BASE + systemId);
    }
}
