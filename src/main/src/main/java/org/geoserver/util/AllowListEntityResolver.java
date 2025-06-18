/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static java.util.function.Predicate.not;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

    public static final String ENTITY_RESOLUTION_UNRESTRICTED_INTERNAL = "ENTITY_RESOLUTION_UNRESTRICTED_INTERNAL";

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

    /** Only allow XSD URIs that do not contain a URI query or fragment */
    private static final Pattern XSD_URIS = Pattern.compile("(?i)^[^?#;]*\\.xsd$");

    /** Checks if a URL contains escaped periods, slashes or backslashes */
    private static final Pattern BANNED_ESCAPES = Pattern.compile("(?i)^.*%(2e|2f|5c).*$");

    /** Checks if a file path starts with a Windows driver letter */
    private static final Pattern WINDOWS_DRIVE = Pattern.compile("^/[a-zA-Z]:/.*$");

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
            uri = normalize(uri);
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

            String proxyBase = (GeoServerExtensions.getProperty(Requests.PROXY_PARAM) != null)
                    ? GeoServerExtensions.getProperty(Requests.PROXY_PARAM)
                    : geoServer != null ? geoServer.getSettings().getProxyBaseUrl() : null;
            if (urlStartsWith(uri, proxyBase)) {
                LOGGER.finest("resolveEntity proxy base: " + uri);
                return null;
            } else if (geoServer != null && isDataDirectorySchema(systemId)) {
                LOGGER.finest("resolveEntity data directory: " + systemId);
                return null;
            } else if (urlStartsWith(uri, baseURL)) {
                // baseURL is only used by unit tests
                LOGGER.finest("resolveEntity base url: " + uri);
                return null;
            }
        } catch (Exception e) {
            // do nothing
        }

        // do not allow external entities
        throw new SAXException(ERROR_MESSAGE_BASE + systemId);
    }

    /**
     * Looks up the location of the gs-main jar file to determine the location of the GeoServer webapp's lib directory.
     */
    private String getGeoServerLibDir() {
        // check if this restriction is disabled
        if (Boolean.parseBoolean(GeoServerExtensions.getProperty(ENTITY_RESOLUTION_UNRESTRICTED_INTERNAL))) {
            return "";
        }
        // this code assumes that /DEFAULT_LOGGING.xml is unique to the gs-main jar file
        // for Jetty or Tomcat, the URL will be like the example below
        //  IN jar:file:/path/to/geoserver.war/WEB-INF/lib/gs-main-2.28.0.jar!/DEFAULT_LOGGING.xml
        // OUT jar:file:/path/to/geoserver.war/WEB-INF/lib/
        // for WildFly, it will be the same except starting with "vfs:" instead of "jar:file:"
        // and without a ! in the path
        //  IN vfs:/path/to/geoserver.war/WEB-INF/lib/gs-main-2.28.0.jar/DEFAULT_LOGGING.xml
        // OUT vfs:/path/to/geoserver.war/WEB-INF/lib/
        // other web servers should behave similarly to Jetty and Tomcat but have not been tested
        String url = getClass().getResource("/DEFAULT_LOGGING.xml").toString();
        url = url.substring(0, url.lastIndexOf('/'));
        return url.substring(0, url.lastIndexOf('/') + 1);
    }

    private boolean isDataDirectorySchema(String systemId) throws IOException {
        String uri = normalize(systemId);
        if (!uri.startsWith("file:")) {
            return false;
        }
        GeoServerResourceLoader resourceLoader = geoServer.getCatalog().getResourceLoader();
        String path = resourceLoader.get("workspaces").dir().getPath();
        return urlStartsWith(uri, normalize("file:", toAbsolutePath(path), true, false));
    }

    private static String normalize(String uri) throws IOException {
        if (!URI.create(uri).isAbsolute()) {
            uri = "file:" + uri;
        }
        if (!uri.startsWith("vfs:/")) {
            // verify that it is a valid URL
            new URL(uri);
        }
        String lower = uri.toLowerCase();
        if (!XSD_URIS.matcher(uri).matches() || BANNED_ESCAPES.matcher(uri).matches()) {
            throw new IllegalArgumentException("Invalid XSD URI: " + uri);
        } else if (lower.startsWith("jar:file:/")) {
            uri = normalize("jar:file:", uri.substring(9), IS_OS_WINDOWS, true);
        } else if (lower.startsWith("vfs:/")) {
            uri = normalize("vfs:", uri.substring(4), IS_OS_WINDOWS, false);
        } else if (lower.startsWith("https://")) {
            uri = normalize("https:", uri.substring(6), true, false);
        } else if (lower.startsWith("http://")) {
            uri = normalize("http:", uri.substring(5), true, false);
        } else if (IS_OS_WINDOWS && lower.startsWith("file:////")) {
            uri = normalize("file:", uri.substring(7), true, false);
        } else if (lower.startsWith("file:///")) {
            uri = normalize("file:", toAbsolutePath(uri.substring(7)), false, false);
        } else if (IS_OS_WINDOWS && lower.startsWith("file://")) {
            uri = normalize("file:", uri.substring(5), true, false);
        } else if (lower.startsWith("file:")) {
            uri = normalize("file:", toAbsolutePath(uri.substring(5)), false, false);
        } else {
            throw new IllegalArgumentException("Unsupported XSD URI protocol: " + uri);
        }
        return uri;
    }

    private static String normalize(String scheme, String path, boolean allowHost, boolean archive) {
        String prefix = "/";
        if (allowHost && path.startsWith("//") && !path.startsWith("///")) {
            prefix = path.substring(0, path.indexOf('/', 2) + 1);
        } else if (IS_OS_WINDOWS && WINDOWS_DRIVE.matcher(path).find()) {
            prefix = path.substring(0, 4);
        }
        path = path.substring(prefix.length());
        String suffix = "";
        if (archive) {
            suffix = path.substring(path.indexOf('!'));
            path = path.substring(0, path.length() - suffix.length());
        }
        List<String> names = Arrays.stream(path.split("/"))
                .filter(not(String::isEmpty))
                .filter(not("."::equals))
                .collect(Collectors.toList());
        for (int index = names.indexOf(".."); index >= 0; index = names.indexOf("..")) {
            // remove the ..
            names.remove(index);
            if (index > 0) {
                // remove previous directory name
                names.remove(index - 1);
            }
        }
        return scheme + prefix + String.join("/", names) + suffix;
    }

    private static String toAbsolutePath(String path) {
        path = new File(path).getAbsolutePath().replace(File.separator, "/");
        return (path.startsWith("/") ? "" : "/") + path;
    }

    private static boolean urlStartsWith(String url, String allowedUrl) {
        if (allowedUrl == null) {
            return false;
        }
        allowedUrl = allowedUrl.endsWith("/") ? allowedUrl : allowedUrl + "/";
        return url.toLowerCase().startsWith(allowedUrl.toLowerCase());
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
