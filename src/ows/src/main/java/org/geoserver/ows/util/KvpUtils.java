/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ows.KvpParser;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;

/**
 * Utility class for reading Key Value Pairs from a http query string.
 *
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @author Gabriel Rold?n, Axios
 * @author Justin Deoliveira, TOPP
 * @author Carlo Cancellieri Geo-Solutions SAS
 * @version $Id$
 */
public class KvpUtils {
    /** Class logger */
    private static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.requests.readers");

    /**
     * Defines how to tokenize a string by using some sort of delimiter.
     *
     * <p>Default implementation uses {@link String#split(String)} with the regular expression
     * provided at the constructor. More specialized subclasses may just override <code>
     * readFlat(String)</code>.
     *
     * @author Gabriel Roldan
     * @since 1.6.0
     */
    public static class Tokenizer {
        private String regExp;

        public Tokenizer(String regExp) {
            this.regExp = regExp;
        }

        private String getRegExp() {
            return regExp;
        }

        public String toString() {
            return getRegExp();
        }

        public List readFlat(final String rawList) {
            if ((rawList == null || rawList.trim().equals(""))) {
                return Collections.EMPTY_LIST;
            } else if (rawList.equals("*")) {
                // handles explicit unconstrained case
                return Collections.EMPTY_LIST;
            }
            // -1 keeps trailing empty strings in the pack
            String[] split = rawList.split(getRegExp(), -1);
            return new ArrayList(Arrays.asList(split));
        }
    }
    /** Delimeter for KVPs in the raw string */
    public static final Tokenizer KEYWORD_DELIMITER = new Tokenizer("&");

    /** Delimeter that seperates keywords from values */
    public static final Tokenizer VALUE_DELIMITER = new Tokenizer("=");

    /** Delimeter for outer value lists in the KVPs */
    public static final Tokenizer OUTER_DELIMETER =
            new Tokenizer("\\)\\(") {
                public List readFlat(final String rawList) {
                    List list = new ArrayList(super.readFlat(rawList));
                    final int len = list.size();
                    if (len > 0) {
                        String first = (String) list.get(0);
                        if (first.startsWith("(")) {
                            list.set(0, first.substring(1));
                        }
                        String last = (String) list.get(len - 1);
                        if (last.endsWith(")")) {
                            list.set(len - 1, last.substring(0, last.length() - 1));
                        }
                    }
                    return list;
                }
            };

    /** Delimeter for inner value lists in the KVPs */
    public static final Tokenizer INNER_DELIMETER = new Tokenizer(",");

    /** Delimeter for multiple filters in a CQL filter list (<code>";"</code>) */
    public static final Tokenizer CQL_DELIMITER = new Tokenizer(";");

    /**
     * Attempts to parse out the proper typeNames from the FeatureId filters. It simply uses the
     * value before the '.' character.
     *
     * @param rawFidList the strings after the FEATUREID url component. Should be found using
     *     kvpPairs.get("FEATUREID") in this class or one of its children
     * @return A list of typenames, made from the featureId filters.
     */
    @SuppressWarnings("rawtypes")
    public static List getTypesFromFids(String rawFidList) {
        List typeList = new ArrayList();
        List unparsed = readNested(rawFidList);
        Iterator i = unparsed.listIterator();

        while (i.hasNext()) {
            List ids = (List) i.next();
            ListIterator innerIterator = ids.listIterator();

            while (innerIterator.hasNext()) {
                String fid = innerIterator.next().toString();
                LOGGER.finer("looking at featureId" + fid);

                String typeName = fid.substring(0, fid.lastIndexOf("."));
                LOGGER.finer("adding typename: " + typeName + " from fid");
                typeList.add(typeName);
            }
        }

        return typeList;
    }

    /** Calls {@link #readFlat(String)} with the {@link #INNER_DELIMETER}. */
    public static List readFlat(String rawList) {
        return readFlat(rawList, INNER_DELIMETER);
    }

    /**
     * Reads a tokenized string and turns it into a list.
     *
     * <p>In this method, the tokenizer is actually responsible to scan the string, so this method
     * is just a convenience to maintain backwards compatibility with the old {@link
     * #readFlat(String, String)} and to easy the use of the default tokenizers {@link
     * #KEYWORD_DELIMITER}, {@link #INNER_DELIMETER}, {@link #OUTER_DELIMETER} and {@link
     * #VALUE_DELIMITER}.
     *
     * <p>Note that if the list is unspecified (ie. is null) or is unconstrained (ie. is ''), then
     * the method returns an empty list.
     *
     * @param rawList The tokenized string.
     * @param tokenizer The delimeter for the string tokens.
     * @return A list of the tokenized string.
     * @see Tokenizer
     */
    public static List readFlat(final String rawList, final Tokenizer tokenizer) {
        return tokenizer.readFlat(rawList);
    }

    /**
     * Reads a tokenized string and turns it into a list. In this method, the tokenizer is quite
     * flexible. Note that if the list is unspecified (ie. is null) or is unconstrained (ie. is ''),
     * then the method returns an empty list.
     *
     * <p>If possible, use the method version that receives a well known {@link #readFlat(String,
     * org.geoserver.ows.util.KvpUtils.Tokenizer) Tokenizer}, as there might be special cases to
     * catch out, like for the {@link #OUTER_DELIMETER outer delimiter "()"}. If this method
     * delimiter argument does not match a well known Tokenizer, it'll use a simple string
     * tokenization based on splitting out the strings with the raw passed in delimiter.
     *
     * @param rawList The tokenized string.
     * @param delimiter The delimeter for the string tokens.
     * @return A list of the tokenized string.
     * @see #readFlat(String, org.geoserver.ows.util.KvpUtils.Tokenizer)
     */
    public static List readFlat(String rawList, String delimiter) {
        Tokenizer delim;
        if (KEYWORD_DELIMITER.getRegExp().equals(delimiter)) {
            delim = KEYWORD_DELIMITER;
        } else if (VALUE_DELIMITER.getRegExp().equals(delimiter)) {
            delim = VALUE_DELIMITER;
        } else if (OUTER_DELIMETER.getRegExp().equals(delimiter)) {
            delim = OUTER_DELIMETER;
        } else if (INNER_DELIMETER.getRegExp().equals(delimiter)) {
            delim = INNER_DELIMETER;
        } else if (CQL_DELIMITER.getRegExp().equals(delimiter)) {
            delim = CQL_DELIMITER;
        } else {
            LOGGER.fine("Using not a well known kvp tokenization delimiter: " + delimiter);
            delim = new Tokenizer(delimiter);
        }
        return readFlat(rawList, delim);
    }

    /**
     * Reads a nested tokenized string and turns it into a list. This method is much more specific
     * to the KVP get request syntax than the more general readFlat method. In this case, the outer
     * tokenizer '()' and inner tokenizer ',' are both from the specification. Returns a list of
     * lists.
     *
     * @param rawList The tokenized string.
     * @return A list of lists, containing outer and inner elements.
     */
    public static List readNested(String rawList) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("reading nested: " + rawList);
        }

        List kvpList = new ArrayList(10);

        // handles implicit unconstrained case
        if (rawList == null) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("found implicit all requested");
            }

            kvpList.add(Collections.EMPTY_LIST);
            return kvpList;

            // handles explicit unconstrained case
        } else if (rawList.equals("*")) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("found explicit all requested");
            }

            kvpList.add(Collections.EMPTY_LIST);
            return kvpList;

            // handles explicit, constrained element lists
        } else {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("found explicit requested");
            }

            // handles multiple elements list case
            if (rawList.startsWith("(")) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest("reading complex list");
                }

                List outerList = readFlat(rawList, OUTER_DELIMETER);
                Iterator i = outerList.listIterator();

                while (i.hasNext()) {
                    kvpList.add(readFlat((String) i.next(), INNER_DELIMETER));
                }

                // handles single element list case
            } else {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest("reading simple list");
                }

                kvpList.add(readFlat(rawList, INNER_DELIMETER));
            }

            return kvpList;
        }
    }

    /**
     * Cleans an HTTP string and returns pure ASCII as a string.
     *
     * @param raw The HTTP-encoded string.
     * @return The string with the url escape characters replaced.
     */
    public static String clean(String raw) {
        LOGGER.finest("raw request: " + raw);

        String clean = null;

        if (raw != null) {
            try {
                clean = java.net.URLDecoder.decode(raw, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                LOGGER.finer("Bad encoding for decoder " + e);
            }
        } else {
            return "";
        }

        LOGGER.finest("cleaned request: " + raw);

        return clean;
    }

    /** @param kvp unparsed/unormalized kvp set */
    public static KvpMap normalize(Map kvp) {
        if (kvp == null) {
            return null;
        }

        // create a normalied map
        KvpMap normalizedKvp = new KvpMap();

        for (Iterator itr = kvp.entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry entry = (Map.Entry) itr.next();
            String key = (String) entry.getKey();
            Object value = null;

            if (entry.getValue() instanceof String) {
                value = trim((String) entry.getValue());
            } else if (entry.getValue() instanceof String[]) {
                String[] values = (String[]) entry.getValue();
                // we use a set so that mere value repetition (a common error for which the OWS spec
                // leaves the server up to decide what to do) does not cause the result to be a
                // String[]
                LinkedHashSet<String> normalized = new LinkedHashSet<String>();
                for (String v : values) {
                    v = trim(v);
                    if (v != null) {
                        normalized.add(v);
                    }
                }
                if (normalized.size() == 0) {
                    value = null;
                } else if (normalized.size() == 1) {
                    value = normalized.iterator().next();
                } else {
                    value = (String[]) normalized.toArray(new String[normalized.size()]);
                }
            }

            // convert key to lowercase
            normalizedKvp.put(key.toLowerCase(), value);
        }

        return normalizedKvp;
    }

    private static String trim(String value) {
        // trim the string
        if (value != null) {
            value = value.trim();
        }
        return value;
    }

    /**
     * Parses a map of key value pairs.
     *
     * <p>Important: This method modifies the map, overriding original values with parsed values.
     *
     * <p>This routine performs a lookup of {@link KvpParser} to parse the kvp entries.
     *
     * <p>If an individual parse fails, this method saves the exception, and adds it to the list
     * that is returned.
     *
     * @param kvp raw or unparsed kvp.
     * @return A list of errors that occured.
     */
    public static List<Throwable> parse(Map kvp) {

        // look up parser objects
        List<KvpParser> parsers = GeoServerExtensions.extensions(KvpParser.class);

        // strip out parsers which do not match current service/request/version
        String service = KvpUtils.getSingleValue(kvp, "service");
        String version = KvpUtils.getSingleValue(kvp, "version");
        String request = KvpUtils.getSingleValue(kvp, "request");

        purgeParsers(parsers, service, version, request);

        // parser the kvp's
        ArrayList<Throwable> errors = new ArrayList<Throwable>();
        for (Iterator<Map.Entry<Object, Object>> itr = kvp.entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry<Object, Object> entry = itr.next();
            String key = (String) entry.getKey();

            // find the parser for this key value pair
            KvpParser parser = findParser(key, service, request, version, parsers);

            // parse the value
            Object parsed = null;
            if (parser != null) {
                try {
                    if (entry.getValue() instanceof String) {
                        String value = (String) entry.getValue();
                        parsed = parser.parse(value);
                    } else {
                        String[] values = (String[]) entry.getValue();
                        List<Object> result = new ArrayList<Object>();
                        for (String v : values) {
                            result.add(parser.parse(v));
                        }
                        parsed = result;
                    }
                } catch (Throwable t) {
                    // dont throw any exceptions yet, befor the service is
                    // known
                    errors.add(t);
                }
            }

            // We only change the value of the parameter if the parser was found and no exception is
            // thrown (parsed != null) If so (==null) it is
            // untouched (remains a String)
            if (parsed != null) {
                entry.setValue(parsed);
            }
        }

        return errors;
    }

    /**
     * Strip out parsers which do not match current service/request/version
     *
     * @param parsers list of {@link KvpParser} to purge (see {@link
     *     GeoServerExtensions#extensions(Class)})
     * @param service the service parameter from the kvp (can be null)
     * @param version the version parameter from the kvp (can be null)
     * @param request the request parameter from the kvp (can be null)
     */
    public static void purgeParsers(
            List<KvpParser> parsers,
            final String service,
            final String version,
            final String request) {
        for (Iterator<KvpParser> p = parsers.iterator(); p.hasNext(); ) {
            KvpParser parser = p.next();

            if (parser.getService() != null && !parser.getService().equalsIgnoreCase(service)) {
                p.remove();
            } else if (parser.getVersion() != null
                    && !parser.getVersion().toString().equals(version)) {
                p.remove();
            } else if (parser.getRequest() != null
                    && !parser.getRequest().equalsIgnoreCase(request)) {
                p.remove();
            }
        }
    }

    /**
     * Find a parser for the passed key into registered parsers ({@link KvpParser})
     *
     * @param key the key matching the value to parse
     * @param service the service parameter from the kvp (can be null)
     * @param version the version parameter from the kvp (can be null)
     * @param request the request parameter from the kvp (can be null)
     * @param parsers the purged parsers list (see {@link #purgeParsers(List, String, String,
     *     String)}
     * @return the found parser or null (if no parser is found)
     * @throws IllegalStateException if more than one candidate parser is found
     */
    public static KvpParser findParser(
            final String key,
            final String service,
            final String request,
            final String version,
            Collection<KvpParser> parsers) {
        // find the parser for this key value pair
        KvpParser parser = null;
        final Iterator<KvpParser> pitr = parsers.iterator();
        while (pitr.hasNext()) {
            KvpParser candidate = pitr.next();
            if (key.equalsIgnoreCase(candidate.getKey())) {
                if (parser == null) {
                    parser = candidate;
                } else {
                    // if target service matches, it is a closer match
                    String trgService = candidate.getService();
                    if (trgService != null && trgService.equalsIgnoreCase(service)) {
                        // determine if this parser more closely matches the request
                        String curService = parser.getService();
                        if (curService == null) {
                            parser = candidate;
                        } else {
                            // both match, filter by version
                            Version curVersion = parser.getVersion();
                            Version trgVersion = candidate.getVersion();
                            if (trgVersion != null) {
                                if (curVersion == null && trgVersion.toString().equals(version)) {
                                    parser = candidate;
                                }
                            } else {
                                if (curVersion == null) {
                                    // ambiguous, unable to match
                                    throw new IllegalStateException(
                                            "Multiple kvp parsers: " + parser + "," + candidate);
                                }
                            }
                        }
                    }
                }
            }
        }
        return parser;
    }

    /**
     * Parse this key value pair using registered parsers ({@link KvpParser})
     *
     * @param key the key matching the value to parse
     * @param value the value to parse
     * @param service the service parameter from the kvp (can be null)
     * @param version the version parameter from the kvp (can be null)
     * @param request the request parameter from the kvp (can be null)
     * @param parsers the purged parsers list (see {@link #purgeParsers(List, String, String,
     *     String)}
     * @return the parsed value or null (if no parser is found)
     * @throws Exception if the selected parser throws an exception
     * @throws IllegalStateException if more than one candidate parser is found
     */
    public static Object parseKey(
            final String key,
            final String value,
            final String service,
            final String request,
            final String version,
            List<KvpParser> parsers)
            throws Exception {
        // find the parser for this key value pair
        KvpParser parser = findParser(key, service, request, version, parsers);
        if (parser == null) {
            return null;
        }
        return parser.parse(value);
    }

    /**
     * Returns a single value for the specified key from the raw KVP, or throws an exception if
     * multiple different values are found
     *
     * @param kvp map of key value pairs
     * @param key key used to lookup a single value
     */
    public static String getSingleValue(Map kvp, String key) {
        Object value = kvp.get(key);
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else {
            String[] strings = (String[]) value;
            if (strings.length == 0) {
                return null;
            }
            String result = strings[0];
            for (int i = 1; i < strings.length; i++) {
                if (!result.equals(strings[i])) {
                    throw new ServiceException(
                            "Single value expected for request parameter "
                                    + key
                                    + " but instead found: "
                                    + Arrays.toString(strings),
                            ServiceException.INVALID_PARAMETER_VALUE,
                            key);
                }
            }

            return result;
        }
    }

    /**
     * Parses the parameters in the path query string. Normally this is done by the servlet
     * container but in a few cases (testing for example) we need to emulate the container instead.
     *
     * @param path a url in the form path?k1=v1&amp;k2=v2&amp;,,,
     */
    public static Map<String, Object> parseQueryString(String path) {
        int index = path.indexOf('?');

        if (index == -1) {
            return Collections.EMPTY_MAP;
        }

        String queryString = path.substring(index + 1);
        StringTokenizer st = new StringTokenizer(queryString, "&");
        Map<String, Object> result = new HashMap<String, Object>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            String[] keyValuePair;
            int idx = token.indexOf('=');
            if (idx > 0) {
                keyValuePair = new String[2];
                keyValuePair[0] = token.substring(0, idx);
                keyValuePair[1] = token.substring(idx + 1);
            } else {
                keyValuePair = new String[1];
                keyValuePair[0] = token;
            }

            // check for any special characters
            if (keyValuePair.length > 1) {
                // replace any equals or & characters
                try {
                    // if this one does not work first check if the url encoded content is really
                    // properly encoded. I had good success with this:
                    // http://meyerweb.com/eric/tools/dencoder/
                    keyValuePair[1] = URLDecoder.decode(keyValuePair[1], "ISO-8859-1");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Totally unexpected... is your JVM busted?", e);
                }
            }

            String key = keyValuePair[0];
            String value = keyValuePair.length > 1 ? keyValuePair[1] : "";
            if (result.get(key) == null) {
                result.put(key, value);
            } else {
                String[] array;
                Object oldValue = result.get(key);
                if (oldValue instanceof String) {
                    array = new String[2];
                    array[0] = (String) oldValue;
                    array[1] = value;
                } else {
                    String[] oldArray = (String[]) oldValue;
                    array = new String[oldArray.length + 1];
                    System.arraycopy(oldArray, 0, array, 0, oldArray.length);
                    array[oldArray.length] = value;
                }
                result.put(key, array);
            }
        }

        return result;
    }

    /**
     * Tokenize a String using the specified separator character and the backslash as an escape
     * character (see OGC WFS 1.1.0 14.2.2). Escape characters within the tokens are not resolved.
     *
     * @param s the String to parse
     * @param separator the character that separates tokens
     * @return list of tokens
     */
    public static List<String> escapedTokens(String s, char separator) {
        return escapedTokens(s, separator, 0);
    }

    /**
     * Tokenize a String using the specified separator character and the backslash as an escape
     * character (see OGC WFS 1.1.0 14.2.2). Escape characters within the tokens are not resolved.
     *
     * @param s the String to parse
     * @param separator the character that separates tokens
     * @param maxTokens ignoring escaped separators, the maximum number of tokens to return. A value
     *     of 0 has no maximum.
     * @return list of tokens
     */
    public static List<String> escapedTokens(String s, char separator, int maxTokens) {
        if (s == null) {
            throw new IllegalArgumentException("The String to parse may not be null.");
        }
        if (separator == '\\') {
            throw new IllegalArgumentException("The separator may not be a backslash.");
        }
        if (maxTokens <= 0) {
            maxTokens = Integer.MAX_VALUE;
        }
        List<String> ret = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        int tokenCount = 1;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == separator && !escaped && tokenCount < maxTokens) {
                ret.add(sb.toString());
                sb.setLength(0);
                tokenCount++;
            } else {
                if (escaped) {
                    escaped = false;
                    sb.append('\\');
                    sb.append(c);
                } else if (c == '\\') {
                    escaped = true;
                } else {
                    sb.append(c);
                }
            }
        }
        if (escaped) {
            throw new IllegalStateException(
                    "The specified String ends with an incomplete escape sequence.");
        }
        ret.add(sb.toString());
        return ret;
    }

    /**
     * Resolve escape sequences in a String.
     *
     * @param s the String to unescape
     * @return resolved String
     */
    public static String unescape(String s) {
        if (s == null) {
            throw new IllegalArgumentException("The String to unescape may not be null.");
        }
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) {
                escaped = false;
                sb.append(c);
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        if (escaped) {
            throw new IllegalArgumentException(
                    "The specified String ends with an incomplete escape sequence.");
        }
        return sb.toString();
    }

    public static String caseInsensitiveParam(Map params, String paramname, String defaultValue) {
        String value = defaultValue;

        for (Object o : params.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            if (entry.getKey() instanceof String) {
                if (paramname.equalsIgnoreCase((String) entry.getKey())) {
                    Object obj = entry.getValue();
                    value =
                            obj instanceof String
                                    ? (String) obj
                                    : (obj instanceof String[])
                                            ? ((String[]) obj)[0].toLowerCase()
                                            : value;
                }
            }
        }

        return value;
    }

    public static void merge(Map options, Map addition) {
        for (Object o : addition.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            if (entry.getValue() == null) options.remove(entry.getKey());
            else options.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Extracts the first value for the specified parameter (the kvp can contain either a single
     * string, or an array of values)
     *
     * @param kvp map of key value pairs
     * @param param retrieve the first value for the parameter
     */
    public static String firstValue(Map kvp, String param) {
        Object o = kvp.get(param);
        if (o == null) {
            return null;
        } else if (o instanceof String) {
            return (String) o;
        } else {
            String[] values = (String[]) o;
            if (values.length > 0) {
                return values[0];
            } else {
                return null;
            }
        }
    }
}
