/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ArrayUtils;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.GeoServerExtensions;

/**
 * Utility class performing operations related to http respones.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @author Andrea Aime, OpenGeo
 */
public class ResponseUtils {
    // the path that does contain the GeoServer internal XML schemas
    public static final String SCHEMAS = "schemas";

    /**
     * Parses the passed string, and encodes the special characters (used in xml for special
     * purposes) with the appropriate codes. e.g. '&lt;' is changed to '&amp;lt;'
     *
     * @param inData The string to encode into xml.
     * @return the encoded string. Returns null, if null is passed as argument
     */
    public static String encodeXML(String inData) {
        // return null, if null is passed as argument
        if (inData == null) {
            return null;
        }

        // if no special characters, just return
        // (for optimization. Though may be an overhead, but for most of the
        // strings, this will save time)
        if ((inData.indexOf('&') == -1)
                && (inData.indexOf('<') == -1)
                && (inData.indexOf('>') == -1)
                && (inData.indexOf('\'') == -1)
                && (inData.indexOf('\"') == -1)) {
            return inData;
        }

        // get the length of input String
        int length = inData.length();

        // create a StringBuffer of double the size (size is just for guidance
        // so as to reduce increase-capacity operations. The actual size of
        // the resulting string may be even greater than we specified, but is
        // extremely rare)
        StringBuffer buffer = new StringBuffer(2 * length);

        char charToCompare;

        // iterate over the input String
        for (int i = 0; i < length; i++) {
            charToCompare = inData.charAt(i);

            // if the ith character is special character, replace by code
            if (charToCompare == '&') {
                buffer.append("&amp;");
            } else if (charToCompare == '<') {
                buffer.append("&lt;");
            } else if (charToCompare == '>') {
                buffer.append("&gt;");
            } else if (charToCompare == '\"') {
                buffer.append("&quot;");
            } else if (charToCompare == '\'') {
                buffer.append("&apos;");
            } else {
                buffer.append(charToCompare);
            }
        }

        // return the encoded string
        return buffer.toString();
    }

    /**
     * Writes <code>string</code> into writer, escaping &amp;, ', ", ^lt;, and &gt; with the XML
     * excape strings.
     */
    public static void writeEscapedString(Writer writer, String string) throws IOException {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            if (c == '<') {
                writer.write("&lt;");
            } else if (c == '>') {
                writer.write("&gt;");
            } else if (c == '&') {
                writer.write("&amp;");
            } else if (c == '\'') {
                writer.write("&apos;");
            } else if (c == '"') {
                writer.write("&quot;");
            } else {
                writer.write(c);
            }
        }
    }

    /**
     * Appends a query string to a url.
     *
     * <p>This method checks <code>url</code> to see if the appended query string requires a '?' or
     * '&amp;' to be prepended.
     *
     * <p>This code can be used to make sure the url ends with ? or &amp; by calling
     * appendQueryString(url, "")
     *
     * @param url The base url.
     * @param queryString The query string to be appended, should not contain the '?' character.
     * @return A full url with the query string appended.
     *     <p>TODO: remove this and replace with Requetss.appendQueryString
     */
    public static String appendQueryString(String url, String queryString) {
        if (url.endsWith("?") || url.endsWith("&")) {
            return url + queryString;
        }

        if (url.indexOf('?') != -1) {
            return url + "&" + queryString;
        }

        return url + "?" + queryString;
    }

    /**
     * Strips the query string off a request url.
     *
     * @param url The url.
     * @return The original minus the query string.
     */
    public static String stripQueryString(String url) {
        int index = url.indexOf('?');

        if (index == -1) {
            return url;
        }

        return url.substring(0, index);
    }

    /**
     * Returns the query string part of a request url.
     *
     * <p>If the url does not have a query string compopnent, the empty string is returned.
     *
     * @param url The url.
     * @return The query string part of the url.
     */
    public static String getQueryString(String url) {
        int index = url.indexOf('?');

        if (index == -1 || index == url.length() - 1) {
            return "";
        }

        return url.substring(index + 1);
    }

    /**
     * Returns the parent url of a url.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>http://foo.com/bar/foo --&gt; http://foo.com/bar
     *   <li>http://foo.com/bar/ --&gt; http://foo.com
     *   <li>http://foo.com/bar --&gt; http://foo.com
     * </ul>
     */
    public static String getParentUrl(String url) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        int index = url.lastIndexOf('/');
        if (index == -1) {
            return url;
        }

        return url.substring(0, index);
    }

    /**
     * Given a set of path components a full path is built
     *
     * @param pathComponents The set of path components
     * @return The full url with the path appended. TODO: remove this and replace with
     *     Requetss.appendContextPath
     */
    public static String appendPath(String... pathComponents) {
        StringBuilder result = new StringBuilder(pathComponents[0]);
        for (int i = 1; i < pathComponents.length; i++) {
            String component = pathComponents[i];
            boolean endsWithSlash = result.charAt(result.length() - 1) == '/';
            boolean startsWithSlash = component.startsWith("/");
            if (endsWithSlash && startsWithSlash) {
                result.setLength(result.length() - 1);
            } else if (!endsWithSlash && !startsWithSlash) {
                result.append("/");
            }
            result.append(component);
        }

        return result.toString();
    }

    /**
     * Strips any remaining part from a path, returning only the first component.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>foo/bar --&gt; foo
     *   <li>/foo/bar --&gt; /foo
     * </ul>
     */
    public static String stripRemainingPath(String path) {
        int i = 0;
        if (path.startsWith("/")) {
            i = 1;
        }

        int index = path.indexOf('/', i);
        if (index > -1) {
            return path.substring(0, index);
        }
        return path;
    }

    /**
     * Strips off the first compontent of a path.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>foo/bar --gt; bar
     *   <li>/foo/bar --gt; bar
     *   <li>/foo/bar/foobar --gt; bar/foobar
     *   <li>/foo --gt; ""
     * </ul>
     */
    public static String stripBeginningPath(String path) {
        int i = 0;
        if (path.startsWith("/")) {
            i = 1;
        }

        int index = path.indexOf('/', i);
        if (index > -1) {
            return path.substring(index + 1);
        }

        return "";
    }

    /**
     * Strips off the extension of a path.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>foo/bar.xml --gt; foo/bar
     *   <li>bar.xml --gt; bar
     *   <li>foo/bar --gt; foo/bar
     * </ul>
     *
     * @return the path minus the extension.
     */
    public static String stripExtension(String path) {
        String ext = getExtension(path);
        if (ext != null) {
            return path.substring(0, path.length() - ext.length() - 1);
        }
        return path;
    }

    /**
     * Returns the last component of a path.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>/foo/bar --&gt; bar
     *   <li>foo/bar/ --&gt; bar
     *   <li>/foo --&gt; foo
     *   <li>foo --&gt; foo
     * </ul>
     *
     * @param path the Path
     * @return the last component of the path
     */
    public static String getLastPartOfPath(String path) {
        int i = path.length();
        if (path.endsWith("/")) {
            i--;
        }

        int j = path.lastIndexOf("/");
        if (j == -1) {
            return path;
        }
        return path.substring(j + 1, i);
    }

    /**
     * Returns the file extension from a uri string.
     *
     * <p>If the uri does not specify an extension, null is returned.
     *
     * @param uri the uri.
     * @return The extension, example "txt", or null if it does not exist.
     */
    public static String getExtension(String uri) {
        int slash = uri.lastIndexOf('/');
        if (slash != -1) {
            uri = uri.substring(slash + 1);
        }
        int dot = uri.lastIndexOf('.');
        if (dot != -1) {
            return uri.substring(dot + 1);
        }
        return null;
    }

    /**
     * Ensures a path is absolute (starting with '/').
     *
     * @param path The path.
     * @return The path starting with '/'.
     */
    public static String makePathAbsolute(String path) {
        if (path.startsWith("/")) {
            return path;
        }

        return "/" + path;
    }

    /**
     * Builds and mangles a URL given its constitutent components. The components will be eventually
     * modified by registered {@link URLMangler} instances to handle proxies or add security tokens
     *
     * @param baseURL the base URL, containing host, port and application
     * @param path the path after the application name
     * @param kvp the GET request parameters
     * @param type URL type
     */
    public static String buildURL(
            String baseURL, String path, Map<String, String> kvp, URLType type) {
        // prepare modifiable parameters
        StringBuilder baseURLBuffer = new StringBuilder(baseURL);
        StringBuilder pathBuffer = new StringBuilder(path != null ? path : "");
        Map<String, String> kvpBuffer = new LinkedHashMap<String, String>();
        if (kvp != null) kvpBuffer.putAll(kvp);

        // run all of the manglers
        for (URLMangler mangler : GeoServerExtensions.extensions(URLMangler.class)) {
            mangler.mangleURL(baseURLBuffer, pathBuffer, kvpBuffer, type);
        }

        // compose the final URL
        String result = appendPath(baseURLBuffer.toString(), pathBuffer.toString());
        StringBuilder params = new StringBuilder();
        for (Map.Entry<String, String> entry : kvpBuffer.entrySet()) {
            params.append(entry.getKey());
            params.append("=");
            String value = entry.getValue();
            if (value != null) {
                String encoded = urlEncode(value);
                params.append(encoded);
            }
            params.append("&");
        }
        if (params.length() > 1) {
            params.setLength(params.length() - 1);
            result = appendQueryString(result, params.toString());
        }

        return result;
    }

    /**
     * Builds and mangles a URL for a schema contained in GeoServer
     *
     * @param baseURL the base URL, containing host, port and application
     * @param path the path inside the schema location (.../geoserver/schemas/...)
     */
    public static String buildSchemaURL(String baseURL, String path) {
        return buildURL(baseURL, appendPath(SCHEMAS, path), null, URLType.RESOURCE);
    }

    /**
     * Pulls out the base url ( from the client point of view ), from the given request object.
     *
     * @return A String of the form "&lt;scheme&gt;://&lt;server&gt;:&lt;port&gt;/&lt;context&gt;/"
     */
    public static String baseURL(HttpServletRequest req) {
        StringBuffer sb = new StringBuffer(req.getScheme());
        sb.append("://").append(req.getServerName());
        if (!("http".equals(req.getScheme()) && req.getServerPort() == 80
                || "https".equals(req.getScheme()) && req.getServerPort() == 443)) {
            sb.append(":").append(req.getServerPort());
        }
        sb.append(req.getContextPath()).append("/");
        return sb.toString();
    }

    /**
     * Convenience method to build a KVP parameter map
     *
     * @param parameters sequence of keys and values
     */
    public static Map<String, String> params(String... parameters) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (parameters.length % 2 != 0)
            throw new IllegalArgumentException(
                    "The parameters sequence should be "
                            + "composed of key/value pairs, but the params passed are odd in number");

        for (int i = 0; i < parameters.length; ) {
            String key = parameters[i++];
            String value = parameters[i++];
            result.put(key, value);
        }

        return result;
    }

    /**
     * URL encodes the value towards the UTF-8 charset
     *
     * @param value the string you want encoded
     * @param exclude any reserved URL character that and should not be percent encoded (such as
     *     '/').
     */
    public static String urlEncode(String value, char... exclude) {
        StringBuilder resultStr = new StringBuilder();
        byte[] encArray;
        try {
            encArray = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(
                    "This is unexpected", e); /* should not happen¸ UTF-8 is always supported */
        }
        for (byte enc : encArray) {
            if (enc >= 'A' && enc <= 'Z'
                    || enc >= 'a' && enc <= 'z'
                    || enc >= '0' && enc <= '9'
                    || enc == '-'
                    || enc == '_'
                    || enc == '.'
                    || enc == '~'
                    || ArrayUtils.contains(exclude, (char) enc)) {
                resultStr.append((char) enc);
            } else {
                resultStr.append(String.format("%%%02X", enc));
            }
        }
        return resultStr.toString();
    }

    /** URL decods the value using ISO-8859-1 as the reference charset */
    public static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("This is unexpected", e);
        }
    }
}
