/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.util.Requests;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * Restricted EntityResolver allowing connections to geoserver base proxy, and OGC / W3C content, and those provided by
 * GEOSERVER_ENTITY_RESOLUTION.
 *
 * @author Jody Garnett (GeoCat)
 */
public class AllowListEntityResolver implements EntityResolver2, Serializable {

    public static final String ENTITY_RESOLUTION_RESTRICT_INTERNAL = "ENTITY_RESOLUTION_RESTRICT_INTERNAL";

    /** Wildcard '*' location indicating unrestricted http(s) access */
    public static String UNRESTRICTED = "*";

    /** Location of Open Geospatical Consortium schemas for OGC OpenGIS standards */
    public static String OGC1 = "schemas.opengis.net";

    public static String OGC2 = "www.opengis.net";
    public static String OGC = OGC1 + "|" + OGC2;

    /** Location of {@code http://inspire.ec.europa.eu/schemas/ } XSD documents for INSPIRE program */
    public static String INSPIRE = "inspire.ec.europa.eu/schemas";

    /** Location of W3C schema documents (for xlink, etc...) */
    public static String W3C = "www.w3.org";

    /** Prefix used for SAXException message */
    private static final String ERROR_MESSAGE_BASE = "Entity resolution disallowed for ";

    protected static final Logger LOGGER = Logging.getLogger(AllowListEntityResolver.class);

    /**
     * Internal uri references.
     *
     * <ul>
     *   <li>allow schema parsing for validation.
     *   <li>jar - internal schema reference
     *   <li>vfs - internal schema reference (JBoss/WildFly)
     * </ul>
     */
    private static final Pattern INTERNAL_URIS = Pattern.compile("(?i)(jar:file|vfs)[^?#;]*\\.xsd");

    /** Block query strings in external uri references */
    private static final Pattern EXTERNAL_URIS = Pattern.compile("(?i)[^?#;]*\\.xsd");

    /** Allowed http(s) locations */
    private final Pattern ALLOWED_URIS;

    /** Base URL from request used to identify internal http(s) references. */
    private final String baseURL;

    /** GeoServer used to identify internal http(s) references as provided by proxy base. */
    private final GeoServer geoServer;

    /** The path to the GeoServer webapp lib directory. */
    private final String geoServerLib;

    /**
     * AllowListEntityResolver willing to resolve commong ogc and w3c entities, and those relative to GeoServer proxy
     * base url.
     *
     * @param geoServer Used to obtain settings for proxy base url
     */
    public AllowListEntityResolver(GeoServer geoServer) {
        this(geoServer, null);
    }

    /**
     * AllowListEntityResolver willing to resolve common ogc and w3c entities, and those relative a base url.
     *
     * @param geoServer Used to obtain settings for proxy base url
     * @param baseURL Base url provided by current request
     */
    public AllowListEntityResolver(GeoServer geoServer, String baseURL) {
        this.geoServer = geoServer;
        this.baseURL = baseURL;

        if (EntityResolverProvider.ALLOW_LIST == null || EntityResolverProvider.ALLOW_LIST.isEmpty()) {
            // Restrict using the built-in allow list
            ALLOWED_URIS = Pattern.compile("(?i)(http|https)://("
                    + Pattern.quote(W3C)
                    + "|"
                    + Pattern.quote(OGC1)
                    + "|"
                    + Pattern.quote(OGC2)
                    + "|"
                    + Pattern.quote(INSPIRE)
                    + ")/[^?#;]*\\.xsd");
        } else {
            StringBuilder pattern = new StringBuilder("(?i)(http|https)://(");
            boolean first = true;
            for (String allow : EntityResolverProvider.ALLOW_LIST) {
                if (first) {
                    first = false;
                } else {
                    pattern.append('|');
                }
                pattern.append(Pattern.quote(allow));
            }
            pattern.append(")/[^?#;]*\\.xsd");
            String regex = pattern.toString();
            LOGGER.fine("ENTITY_RESOLUTION_ALLOWLIST processed:" + regex);

            ALLOWED_URIS = Pattern.compile(regex);
        }
        this.geoServerLib = getGeoServerLibDir();
    }

    /**
     * Looks up the location of the gs-main jar file to determine the location of the GeoServer webapp's lib directory.
     */
    private String getGeoServerLibDir() {
        // check if this restriction is disabled
        if ("false".equalsIgnoreCase(GeoServerExtensions.getProperty(ENTITY_RESOLUTION_RESTRICT_INTERNAL))) {
            return "";
        }
        // this code assumes that /DEFAULT_LOGGING.xml is unique to the gs-main jar file
        // for Jetty or Tomcat, the URL will be like the example below
        // for WildFly, it will be the same except starting with "vfs:" instead of "jar:file:"
        // other web servers should behave similarly to Jetty and Tomcat but have not been tested
        // IN  jar:file:/path/to/geoserver.war/WEB-INF/lib/gs-main-2.28.0.jar!/DEFAULT_LOGGING.xml
        // OUT jar:file:/path/to/geoserver.war/WEB-INF/lib/
        String url = getClass().getResource("/DEFAULT_LOGGING.xml").toString();
        url = url.substring(0, url.lastIndexOf('/'));
        return url.substring(0, url.lastIndexOf('/') + 1);
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return resolveEntity(null, publicId, null, systemId);
    }

    @Override
    public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
        return resolveEntity(name, null, baseURI, null);
    }

    @Override
    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId)
            throws SAXException, IOException {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(String.format(
                    "resolveEntity request: name=%s, publicId=%s, baseURI=%s, systemId=%s",
                    name, publicId, baseURI, systemId));
        }

        try {
            String uri;
            if (systemId == null) {
                if (name != null) {
                    LOGGER.finest("resolveEntity name: " + name);
                    return null;
                }
                throw new SAXException("External entity systemId not provided");
            }

            if (URI.create(systemId).isAbsolute()) {
                uri = systemId;
            } else {
                // use the baseURI to convert a relative systemId to absolute
                if (baseURI == null) {
                    throw new SAXException(ERROR_MESSAGE_BASE + systemId);
                }
                if ((baseURI.endsWith(".xsd") || baseURI.endsWith(".XSD")) && baseURI.lastIndexOf('/') != -1) {
                    uri = baseURI.substring(0, baseURI.lastIndexOf('/')) + '/' + systemId;
                } else {
                    uri = baseURI + '/' + systemId;
                }
            }
            // check if the absolute systemId is an allowed URI jar or vfs reference
            if (INTERNAL_URIS.matcher(uri).matches() && uri.startsWith(this.geoServerLib)) {
                LOGGER.finest("resolveEntity internal: " + uri);
                return null;
            }
            // Allow select external locations
            if (ALLOWED_URIS.matcher(uri).matches()) {
                LOGGER.finest("resolveEntity allowed: " + uri);
                return null;
            }

            if (EXTERNAL_URIS.matcher(uri).matches()) {
                String proxyBase = (GeoServerExtensions.getProperty(Requests.PROXY_PARAM) != null)
                        ? GeoServerExtensions.getProperty(Requests.PROXY_PARAM)
                        : geoServer != null ? geoServer.getSettings().getProxyBaseUrl() : null;
                if (urlStartsWith(uri, proxyBase)) {
                    LOGGER.finest("resolveEntity proxy base: " + uri);
                    return null;
                } else if (geoServer != null && isDataDirectorySchema(systemId)) {
                    LOGGER.finest("resolveEntity data directory: " + uri);
                    return null;
                } else if (urlStartsWith(uri, baseURL)) {
                    // baseURL is only used by unit tests
                    LOGGER.finest("resolveEntity base url: " + uri);
                    return null;
                }
            }
        } catch (Exception e) {
            // do nothing
        }

        // do not allow external entities
        throw new SAXException(ERROR_MESSAGE_BASE + systemId);
    }

    private static boolean urlStartsWith(String url, String allowedUrl) {
        if (allowedUrl == null) {
            return false;
        }
        allowedUrl = allowedUrl.endsWith("/") ? allowedUrl : allowedUrl + "/";
        return url.toLowerCase().startsWith(allowedUrl.toLowerCase());
    }

    private boolean isDataDirectorySchema(String systemId) throws IOException {
        GeoServerResourceLoader resourceLoader = geoServer.getCatalog().getResourceLoader();
        String path = resourceLoader.get("workspaces").dir().getCanonicalPath();
        if (systemId.startsWith("file:")) systemId = systemId.substring(5);
        String canonicalSystemId = new File(systemId).getCanonicalPath();
        return canonicalSystemId.startsWith(path);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("AllowListEntityResolver:( ");
        builder.append(this.baseURL);
        builder.append(" ");
        builder.append(this.ALLOWED_URIS);
        builder.append(")");
        return builder.toString();
    }
}
