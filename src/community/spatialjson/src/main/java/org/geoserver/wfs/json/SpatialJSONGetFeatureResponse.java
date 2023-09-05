/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import com.google.common.base.Utf8;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.data.util.TemporalUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;

/**
 * A GetFeatureInfo response handler specialized in producing JSON and JSONP data in
 * <em>SpatialJSON</em> format for a GetFeature request.
 *
 * @author Carsten Klein, DataGis
 */
public class SpatialJSONGetFeatureResponse extends GeoJSONGetFeatureResponse {

    /**
     * The minimum length in bytes (in UTF-8 encoding) a string must have in order to be added to
     * the shared string table. Strings shorter than this are generally not considered a candidate
     * for the shared string table.
     */
    private static final int SST_MINIMUM_BYTE_LENGTH = 2;

    /** The key value into the optional FORMAT_OPTIONS map for the {@code sharedstrings} option */
    private static final String SHARED_STRINGS_OPTION_KEY = "sharedstrings";

    /**
     * Default value for the {@code sharedstrings} format option.
     *
     * <p>Use {@code true} to create shared strings from all String type properties, or {@code
     * false} to <em>not</em> create a shared string table. Alternatively, a comma-separated list of
     * property names may be specified from which to create shared strings. (The latter makes no
     * sense as a default value, however.)
     */
    private static final String SHARED_STRINGS_OPTION_DEFAULT = "true";

    /**
     * Parses and returns the basic MIME type, that is the type without any additional parameters,
     * from the specified format string.
     *
     * @param format the format string to parse
     */
    private static String parseMimeType(String format) {
        int pos = format.indexOf(';');
        return pos != -1 ? format.substring(0, pos).trim() : format;
    }

    public SpatialJSONGetFeatureResponse(GeoServer gs, String format) {
        super(gs, format, JSONType.isJsonpMimeType(parseMimeType(format)));
    }

    /** capabilities output format string. */
    @Override
    public String getCapabilitiesElementName() {
        return getOutputFormats().isEmpty() ? null : getOutputFormats().iterator().next();
    }

    /** Returns the mime type */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return getOutputFormats().isEmpty() ? null : getOutputFormats().iterator().next();
    }

    /**
     * Builds, configures and returns {@link GeoJSONBuilder}. This method actually instantiates
     * class {@link GeoJSONBuilderWithContext}, an extended version of the {@code GeoJSONBuilder}
     * with an additional data context.
     *
     * @see Context
     */
    @Override
    protected GeoJSONBuilder getGeoJSONBuilder(
            FeatureCollectionResponse featureCollection, Writer outWriter) {
        final GeoJSONBuilder jsonWriter = new GeoJSONBuilderWithContext(outWriter);
        int numDecimals = getNumDecimals(featureCollection.getFeature(), gs, gs.getCatalog());
        jsonWriter.setNumberOfDecimals(numDecimals);
        jsonWriter.setEncodeMeasures(
                encodeMeasures(featureCollection.getFeature(), gs.getCatalog()));
        return jsonWriter;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation operation)
            throws IOException {
        Name typeName = null;
        for (FeatureCollection fc : featureCollection.getFeatures()) {
            FeatureType schema = fc.getSchema();
            if (typeName == null) {
                typeName = schema.getName();
            } else if (!typeName.equals(schema.getName())) {
                throw new WFSException(
                                "Query returned an inhomogenous list of feature types but "
                                        + "output format SpatialJSON supports encoding a single "
                                        + "feature type per request only",
                                "InvalidParameterValue")
                        .locator("outputFormat");
            }
            if (!(schema instanceof SimpleFeatureType)) {
                // this feature collection contains complex features
                throw new WFSException(
                                "Feature type "
                                        + typeName.toString()
                                        + " contains complex features but output format "
                                        + "SpatialJSON supports encoding simple features only",
                                "InvalidParameterValue")
                        .locator("outputFormat");
            }
        }
        super.write(featureCollection, output, operation);
    }

    /**
     * Modified version of {@link GeoJSONGetFeatureResponse#encodeSimpleFeatures} writing simple
     * features in SpatialJSON format.
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected FeaturesInfo encodeSimpleFeatures(
            GeoJSONBuilder jsonWriter,
            List<FeatureCollection> resultsList,
            boolean featureBounding,
            Operation operation) {

        String id_option = getIdOption();
        CoordinateReferenceSystem crs = null;
        boolean hasGeom = false;
        long featureCount = 0;

        List<String> propertyNames = new ArrayList<>();
        String geometryName = null;
        SharedStringTable sharedStringTable = new SharedStringTable();

        Map<String, String> formatOptions = getFormatOptions();
        String sharedStringsOption = getSharedStringsOption(formatOptions);

        // null => do not create a string table
        Set<Pattern> sharedStringAttributes = null;

        Context context;
        try {
            context = (Context) jsonWriter;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Passed GeoJSONBuilder instance has no Context associated", e);
        }

        if (!"false".equals(sharedStringsOption)) {
            if (!"true".equals(sharedStringsOption) && !"*".equals(sharedStringsOption)) {
                String mode;
                if (sharedStringsOption.startsWith("glob:")) {
                    mode = "glob";
                    sharedStringsOption = sharedStringsOption.substring(5);
                } else if (sharedStringsOption.startsWith("re:")) {
                    mode = "re";
                    sharedStringsOption = sharedStringsOption.substring(3);
                } else {
                    // mode `text` is default
                    mode = "text";
                    if (sharedStringsOption.startsWith("text:")) {
                        // nevertheless need to remove prefix if specified
                        sharedStringsOption = sharedStringsOption.substring(5);
                    }
                }
                // create a string table only if at least one property name or pattern is given
                if (sharedStringsOption.length() > 0) {
                    sharedStringAttributes =
                            new LinkedPatternHashSet(
                                    KvpUtils.escapedTokens(sharedStringsOption, ',').stream()
                                            .map(KvpUtils::unescape)
                                            .collect(Collectors.toSet()),
                                    mode);
                }
            } else {
                // empty set => allow adding all string properties to the string table
                sharedStringAttributes = Collections.emptySet();
            }
        }

        for (FeatureCollection collection : resultsList) {
            try (FeatureIterator iterator = collection.features()) {
                SimpleFeatureType fType;
                List<AttributeDescriptor> types = null;
                GeometryDescriptor defaultGeomType = null;
                // encode each simple feature
                while (iterator.hasNext()) {
                    // get next simple feature
                    SimpleFeature simpleFeature = (SimpleFeature) iterator.next();
                    featureCount++;
                    // start writing the JSON feature object
                    jsonWriter.object();
                    jsonWriter.key("type").value("Feature");
                    fType = simpleFeature.getFeatureType();
                    types = fType.getAttributeDescriptors();
                    // write the simple feature id
                    if (id_option == null) {
                        // no specific attribute nominated, use the simple feature id
                        jsonWriter.key("id").value(simpleFeature.getID());
                    } else if (id_option.length() != 0) {
                        // a specific attribute was nominated to be used as id
                        Object value = simpleFeature.getAttribute(id_option);
                        jsonWriter.key("id").value(value);
                    }
                    // set that axis order that should be used to write geometries
                    defaultGeomType = fType.getGeometryDescriptor();
                    if (defaultGeomType != null) {
                        CoordinateReferenceSystem featureCrs =
                                defaultGeomType.getCoordinateReferenceSystem();
                        jsonWriter.setAxisOrder(CRS.getAxisOrder(featureCrs));
                        if (crs == null) {
                            crs = featureCrs;
                        }
                        if (featureCount == 1) {
                            // do this only once
                            geometryName = defaultGeomType.getLocalName();
                        }
                    } else {
                        // If we don't know, assume EAST_NORTH so that no swapping occurs
                        jsonWriter.setAxisOrder(CRS.AxisOrder.EAST_NORTH);
                    }
                    // start writing the simple feature geometry JSON object
                    Geometry aGeom = (Geometry) simpleFeature.getDefaultGeometry();
                    if (aGeom != null || writeNullGeometries()) {
                        jsonWriter.key("geometry");
                        // Write the geometry, whether it is a null or not
                        if (aGeom != null) {
                            jsonWriter.writeGeom(aGeom);
                            hasGeom = true;
                        } else {
                            jsonWriter.value(null);
                        }
                    }
                    // start writing feature properties JSON object
                    jsonWriter.key("properties");
                    jsonWriter.array();
                    for (int j = 0, k = 0; j < types.size(); j++, k++) {
                        Object value = simpleFeature.getAttribute(j);
                        AttributeDescriptor ad = types.get(j);
                        if (id_option != null && id_option.equals(ad.getLocalName())) {
                            k--;
                            continue; // skip this value as it is used as the id
                        }
                        if (ad instanceof GeometryDescriptor) {
                            // This is an area of the spec where they
                            // decided to 'let convention evolve',
                            // that is how to handle multiple
                            // geometries. My take is to print the
                            // geometry here if it's not the default.
                            // If it's the default that you already
                            // printed above, so you don't need it here.
                            if (!ad.equals(defaultGeomType)) {
                                if (value == null) {
                                    jsonWriter.value(null);
                                } else {
                                    // if it was the default geometry, it has been written above
                                    // already
                                    jsonWriter.writeGeom((Geometry) value);
                                }
                                if (featureCount == 1) {
                                    // do this only once
                                    propertyNames.add(ad.getLocalName());
                                }
                            } else {
                                k--;
                            }
                        } else if (Date.class.isAssignableFrom(ad.getType().getBinding())
                                && TemporalUtils.isDateTimeFormatEnabled()) {
                            // Temporal types print handling
                            jsonWriter.value(TemporalUtils.printDate((Date) value));
                            if (featureCount == 1) {
                                // do this only once
                                propertyNames.add(ad.getLocalName());
                            }
                        } else {
                            if ((value instanceof Double && Double.isNaN((Double) value))
                                    || value instanceof Float && Float.isNaN((Float) value)) {
                                jsonWriter.value(null);
                            } else if ((value instanceof Double
                                            && ((Double) value) == Double.POSITIVE_INFINITY)
                                    || value instanceof Float
                                            && ((Float) value) == Float.POSITIVE_INFINITY) {
                                jsonWriter.value("Infinity");
                            } else if ((value instanceof Double
                                            && ((Double) value) == Double.NEGATIVE_INFINITY)
                                    || value instanceof Float
                                            && ((Float) value) == Float.NEGATIVE_INFINITY) {
                                jsonWriter.value("-Infinity");
                            } else {
                                if (value instanceof CharSequence
                                        && sharedStringAttributes != null
                                        && (sharedStringAttributes.isEmpty()
                                                || ((LinkedPatternHashSet) sharedStringAttributes)
                                                        .containsMatched(ad.getLocalName()))) {
                                    value = sharedStringTable.add(value.toString(), k);
                                }
                                jsonWriter.value(value);
                            }
                            if (featureCount == 1) {
                                // do this only once
                                propertyNames.add(ad.getLocalName());
                            }
                        }
                    }
                    jsonWriter.endArray(); // end the properties

                    // Bounding box for feature in properties
                    ReferencedEnvelope refenv =
                            ReferencedEnvelope.reference(simpleFeature.getBounds());
                    if (featureBounding && !refenv.isEmpty()) {
                        jsonWriter.writeBoundingBox(refenv);
                    }

                    writeExtraFeatureProperties(simpleFeature, operation, jsonWriter);

                    jsonWriter.endObject(); // end the feature
                }
            }
        }
        context.setPropertyNames(propertyNames);
        context.setGeometryName(geometryName);
        context.setSharedStringTable(sharedStringTable);
        return new FeaturesInfo(crs, hasGeom, featureCount);
    }

    /** Writes collection properties like schema information, shared string table etc. */
    @Override
    protected void writeExtraCollectionProperties(
            FeatureCollectionResponse response, Operation operation, GeoJSONBuilder jw) {
        Context context;
        try {
            context = (Context) jw;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Passed GeoJSONBuilder instance has no Context associated.", e);
        }

        // write mandatory schema information
        jw.key("schemaInformation").object();
        jw.key("propertyNames").array();
        for (String name : context.getPropertyNames()) {
            jw.value(name);
        }
        jw.endArray();

        if (context.getGeometryName() != null) {
            jw.key("geometryName").value(context.getGeometryName());
        }
        jw.endObject();

        // write optional shared string table
        SharedStringTable sharedStringTable = context.getSharedStringTable();
        if (sharedStringTable != null && !sharedStringTable.isEmpty()) {
            jw.key("sharedStrings").object();
            jw.key("indexes").array();
            for (Integer index : sharedStringTable.getIndexes()) {
                jw.value(index);
            }
            jw.endArray();
            jw.key("table").array();
            for (String value : sharedStringTable.getStrings()) {
                jw.value(value);
            }
            jw.endArray();
            jw.endObject();
        }

        super.writeExtraCollectionProperties(response, operation, jw);
    }

    /** Returns the {@code FORMAT_OPTIONS} map for this request. */
    private Map<String, String> getFormatOptions() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return null;
        }
        Map<String, Object> kvp = request.getKvp();
        if (!(kvp.get("FORMAT_OPTIONS") instanceof Map)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> formatOptions = (Map<String, String>) kvp.get("FORMAT_OPTIONS");
        if (formatOptions == null || formatOptions.isEmpty()) {
            return null;
        }
        return formatOptions;
    }

    /**
     * Returns the {@code sharedstrings} format option.
     *
     * @param formatOptions the {@code FORMAT_OPTIONS} to obtain shared strings option from
     * @return the {@code sharedstrings} format option, or {@code "false"} if the option was
     *     specified but is empty ({@code ""}) or {@link #SHARED_STRINGS_OPTION_DEFAULT} if the
     *     {@code sharedstrings} format option was not specified at all
     */
    private String getSharedStringsOption(Map<String, String> formatOptions) {
        String result = null;
        if (formatOptions != null) {
            result = formatOptions.get(SHARED_STRINGS_OPTION_KEY);
        }
        return result == null
                ? SHARED_STRINGS_OPTION_DEFAULT
                : result.length() == 0 ? "false" : result;
    }

    /**
     * A {@code LinkedHashSet} implementation for storing Java Regular Expression Patterns. This
     * class has an additional method {@link #containsMatched(CharSequence)} to determine whether
     * this set contains a pattern that matches the specified character sequence entirely.
     *
     * <p>The only constructor of this class takes a collection of character sequences from which it
     * creates its patterns. The specified {@code mode} parameter defines what kind of patterns
     * these string expressions do represent:
     *
     * <ul>
     *   <li><b>{@code text}</b>: interpret specified string expressions as literal text
     *   <li><b>{@code glob}</b>: interpret specified string expressions as glob pattern
     * </ul>
     *
     * <p>For all other values of mode (including {@code null}) the specified string expressions are
     * interpreted as a Java Regular Expression.
     *
     * <p>For a brief description of the supported glob pattern syntax, see method {@link
     * #globToPattern(String)}.
     */
    private static class LinkedPatternHashSet extends LinkedHashSet<Pattern> {

        /** the serial version UID */
        private static final long serialVersionUID = 1L;

        /** result cache for method {@link #containsMatched(CharSequence)} */
        private final Map<CharSequence, Boolean> cache = new HashMap<>();

        /**
         * Converts a collection of patterns (character sequences) to a collection of Patterns. The
         * specified {@code mode} parameter defines how to convert each pattern.
         *
         * <ul>
         *   <li><b>{@code text}</b>: interpret each element as literal text
         *   <li><b>{@code glob}</b>: interpret each element as glob pattern
         * </ul>
         *
         * <p>For all other values of mode each element is interpreted as a Java Regular Expression.
         *
         * @param c the char sequences to convert to Patterns
         * @param mode the conversion mode
         * @return the converted Patterns
         * @see Pattern
         */
        protected static Collection<Pattern> getPatterns(Collection<CharSequence> c, String mode) {
            switch (mode) {
                case "glob":
                    return c.stream()
                            .map(s -> globToPattern(s.toString()))
                            .collect(Collectors.toList());

                case "text":
                    return c.stream()
                            .map(s -> Pattern.compile(Pattern.quote(s.toString())))
                            .collect(Collectors.toList());

                default:
                    return c.stream()
                            .map(s -> Pattern.compile(s.toString()))
                            .collect(Collectors.toList());
            }
        }

        /**
         * Converts the specified glob pattern to an equivalent Java Regular Expression Pattern.
         *
         * <p>Supports some basic glob pattern syntax only:
         *
         * <table>
         * <tr>
         * <td>{@code x}</td>
         * <td>matches the character x</td>
         * </tr>
         * <tr>
         * <td>{@code \\}</td>
         * <td>matches the backslash character</td>
         * </tr>
         * <tr>
         * <td>{@code *}</td>
         * <td>matches any number of any characters including none</td>
         * </tr>
         * <tr>
         * <td>{@code ?}</td>
         * <td>matches any single character</td>
         * </tr>
         * <tr>
         * <td>{@code [abc]}</td>
         * <td>matches one character given in the bracket</td>
         * </tr>
         * <tr>
         * <td>{@code [a-z]}</td>
         * <td>matches one character from the range given in the bracket</td>
         * </tr>
         * <tr>
         * <td>{@code [!abc]}</td>
         * <td>matches one character that is not given in the bracket</td>
         * </tr>
         * <tr>
         * <td>{@code [!a-z]}</td>
         * <td>matches one character that is not from the range given in the bracket</td>
         * </tr>
         * </table>
         *
         * <p>Like with Java Regular Expression character classes, supports specifying multiple
         * ranges and mixing ranges with discrete sets of characters ({@code [a-fxyz0-9ijk]}).
         *
         * <p>However, glob pattern character classes do <em>not</em> support:
         *
         * <ul>
         *   <li>predefined character classes
         *   <li>escaping characters with a backslash
         *   <li>nested character classes (like union, intersection or subtraction)
         * </ul>
         *
         * <p>Basically, all characters in a character class are interpreted literally. The only
         * exceptions are:
         *
         * <ul>
         *   <li>negation indicated by {@code !} (must be the first character)
         *   <li>{@code ]} matches ] only if it's the first or second (with negation) character in
         *       the class (e. g. {@code []a-d]} matches characters ], a, b, c and d, whereas {@code
         *       [!]a-d]} matches all characters except ], a, b, c and d)
         * </ul>
         *
         * <p>Outside of a character class, all characters except {@code *}, {@code ?} and {@code [}
         * are interpreted literally. The special meaning of these characters can be removed by
         * escaping them with a backslash.
         *
         * @param s the glob pattern to convert
         * @return the specified glob pattern as a Java Regular Expression Pattern instance
         * @throws PatternSyntaxException if the expression's syntax is invalid
         */
        protected static Pattern globToPattern(String s) {
            StringBuilder result = new StringBuilder("^");
            StringBuilder buf = new StringBuilder();

            boolean escaped = false;
            boolean inClass = false;
            int l1 = 1;

            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (inClass) {
                    switch (ch) {
                        case '!':
                            if (buf.length() == 1) {
                                buf.append('^');
                                l1 = 2;
                            } else {
                                buf.append('!');
                            }
                            break;

                        case '^':
                            buf.append("\\^");

                        case ']':
                            if (buf.length() == l1) {
                                buf.append("\\]");
                            } else {
                                if (buf.length() > l1) {
                                    result.append(buf).append(']');
                                }
                                buf.setLength(0);
                                inClass = false;
                            }
                            break;

                        case '\\':
                            buf.append("\\\\");
                            break;

                        case '[':
                            buf.append("\\[");
                            break;

                        default:
                            buf.append(ch);
                            break;
                    }

                    continue;
                }
                if (escaped) {
                    buf.append(ch);
                    escaped = false;
                    continue;
                }
                switch (ch) {
                    case '\\':
                        escaped = true;
                        break;

                    case '*':
                    case '?':
                        if (buf.length() > 0) {
                            result.append(Pattern.quote(buf.toString()));
                            buf.setLength(0);
                        }
                        result.append(ch == '*' ? ".*" : ".");
                        break;

                    case '[':
                        if (buf.length() > 0) {
                            result.append(Pattern.quote(buf.toString()));
                            buf.setLength(0);
                        }
                        buf.append('[');
                        inClass = true;
                        l1 = 1;
                        break;

                    default:
                        buf.append(ch);
                        break;
                }
            }

            // still in character class?
            if (inClass) {
                throw new PatternSyntaxException("Unclosed character class", s, s.length() - 1);
            }

            if (buf.length() > 0) {
                result.append(Pattern.quote(buf.toString()));
            }

            try {
                return Pattern.compile(result.append('$').toString());
            } catch (PatternSyntaxException e) {
                throw (PatternSyntaxException)
                        new PatternSyntaxException(e.getDescription(), s, -1).initCause(e);
            }
        }

        /**
         * Constructs a new linked Pattern hash set with Patterns defined by the specified
         * collection. The passed character sequences are converted according to the {@code mode}
         * parameter.
         *
         * @param c the character sequences to convert to Patterns and to populate the new set with
         * @param mode the conversion mode
         */
        public LinkedPatternHashSet(Collection<CharSequence> c, String mode) {
            super(getPatterns(c, mode != null ? mode : "re"));
        }

        /**
         * Returns {@code true} is this set contains a Pattern that matches the specified character
         * sequence. Returns {@code false} if the specified character sequence is {@code null}.
         *
         * @param s the character sequence to test this set's elements against
         * @return {@code true} is this set contains a Pattern that matches the specified character
         *     sequence, {@code false} otherwise or if the specified character sequence is {@code
         *     null}
         */
        public boolean containsMatched(CharSequence s) {
            if (s == null) {
                return false;
            }
            Boolean result = cache.get(s);
            if (result == null) {
                result = false;
                for (Pattern pattern : this) {
                    Matcher matcher = pattern.matcher(s);
                    if ((result = matcher.matches())) {
                        break;
                    }
                }
                cache.put(s, result);
            }
            return result;
        }

        @Override
        public boolean add(Pattern e) {
            if (cache != null) {
                cache.clear();
            }
            return super.add(e);
        }

        @Override
        public boolean remove(Object o) {
            cache.clear();
            return super.remove(o);
        }

        @Override
        public void clear() {
            cache.clear();
            super.clear();
        }
    }

    /** Helper class for building a Shared String Table */
    private class SharedStringTable {

        /**
         * The string table. This map's keys are the strings stored in the string table. Each
         * mapping's value is the index of the string in this string table, which corresponds to the
         * mapping's insertion order. Since a {@link LinkedHashMap} shall be used for this map, the
         * insertion order is preserved and the string table's are returned in correct order when
         * iterating over the map's key set.
         */
        private final Map<String, Integer> table;

        /**
         * Contains all zero-based indexes of properties that are of type String (technically) and
         * whose values (or at least some of them) are actually stored in this string table.
         */
        private final Set<Integer> indexes;

        /** Default constructor */
        public SharedStringTable() {
            table = new LinkedHashMap<>();
            indexes = new HashSet<>();
        }

        /**
         * Adds the specified value to this string table if it does not already contain that value,
         * updates the set of property indexes and returns the specified value's index in this
         * string table. Returns the specified string value if the value has not been added to the
         * string table. These are the reasons that prevent a string value from being added to the
         * string table:
         *
         * <ul>
         *   <li>the specified value is {@code null}
         *   <li>the specified value's byte length is smaller than {@link
         *       SpatialJSONGetFeatureResponse#SST_MINIMUM_BYTE_LENGTH SST_MINIMUM_BYTE_LENGTH}
         *   <li>the specified value's byte length is smaller than the number if digits of its table
         *       index
         *   <li>the string table is full (contains {@link Integer#MAX_VALUE} entries)
         * </ul>
         *
         * @param value string value to be added to this string table
         * @param index zero-based index of the specified value in the {@code properties} array
         * @return the value's index in this string table, or {@code value} if the specified value
         *     has not been added to this string table
         */
        public Object add(String value, int index) {
            if (value == null) {
                // do not add null values
                return value;
            }
            int byteLength = Utf8.encodedLength(value);
            if (byteLength < SST_MINIMUM_BYTE_LENGTH) {
                // do not add strings shorter than configured minimum length
                return value;
            }
            Integer tableIndex = table.get(value);
            if (tableIndex != null) {
                // specified string is already contained in the string table
                // update property indexes and return the value's table index
                indexes.add(index);
                return tableIndex;
            }
            tableIndex = table.size();
            if (tableIndex == Integer.MAX_VALUE) {
                // do not add more than Integer.MAX_VALUE strings
                return value;
            }
            if (numDigits(tableIndex) > byteLength + 2) {
                // do not add a string whose index takes more bytes than the string itself:
                // e. g. storing "abc" as number 12345 is useless and would increase output size
                return value;
            }
            // add to table, update property indexes and return the value's table index
            table.put(value, tableIndex);
            indexes.add(index);
            return tableIndex;
        }

        /**
         * Returns {@code true} if this string table contains no entries.
         *
         * @return {@code true} if this string table contains no entries
         */
        public boolean isEmpty() {
            return table.isEmpty();
        }

        /**
         * Returns this string table's string data as a List in proper order
         *
         * @return this string table's string data
         */
        public List<String> getStrings() {
            return new ArrayList<>(table.keySet());
        }

        /**
         * Returns this string table's property indexes as a List in natural (ascending) order
         *
         * @return this string table's property indexes
         */
        public List<Integer> getIndexes() {
            // use TreeSet for natural ordering
            return new ArrayList<>(new TreeSet<>(indexes));
        }

        /**
         * Returns the number of decimal digits required for the specified integer.
         *
         * @param n integer to determine number of decimal digits for
         * @return the number of decimal digits required for the specified integer
         */
        private int numDigits(int n) {
            if (n < 0) {
                n = (n == Integer.MIN_VALUE) ? Integer.MAX_VALUE : -n;
            }
            if (n == 0) {
                return 1;
            }
            return (int) (Math.floor(Math.log10(n)) + 1);
        }
    }

    /**
     * Defines the methods of the Context used for sharing data between methods {@link
     * #encodeSimpleFeatures} and {@link #writeExtraCollectionProperties}. Since this Context is
     * attached to the {@link GeoJSONBuilder} for simplicity (see class {@link
     * GeoJSONBuilderWithContext}), using a dedicated interface could provide more clarity to the
     * code.
     *
     * @see GeoJSONBuilderWithContext
     */
    private interface Context {

        /** Returns the property names. These are part of the Schema Information. */
        List<String> getPropertyNames();

        /**
         * Sets the property names. These are part of the Schema Information.
         *
         * @param propertyNames the property names to set
         */
        void setPropertyNames(List<String> propertyNames);

        /**
         * Returns the geometry name or {@code null} if none is available. This property is part of
         * the Schema Information.
         */
        String getGeometryName();

        /**
         * Sets the geometry name. This property is part of the Schema Information.
         *
         * @param geometryName the geometry name to set
         */
        void setGeometryName(String geometryName);

        /** Returns the Shared String Table or {@code null} if none is available. */
        SharedStringTable getSharedStringTable();

        /**
         * Sets the Shared String Table.
         *
         * @param sharedStringTable the Shared String Table to set
         */
        void setSharedStringTable(SharedStringTable sharedStringTable);
    }

    /**
     * Implementation of the GeoJSONBuilder with an additional {@link Context} to store/share data
     * between methods {@link #encodeSimpleFeatures} and {@link #writeExtraCollectionProperties}.
     *
     * @see #Context
     */
    private class GeoJSONBuilderWithContext extends GeoJSONBuilder implements Context {

        private List<String> propertyNames;

        private String geometryName;

        private SharedStringTable sharedStringTable;

        public GeoJSONBuilderWithContext(Writer w) {
            super(w);
        }

        @Override
        public List<String> getPropertyNames() {
            return propertyNames != null ? propertyNames : Collections.emptyList();
        }

        @Override
        public void setPropertyNames(List<String> propertyNames) {
            this.propertyNames = propertyNames;
        }

        @Override
        public String getGeometryName() {
            return geometryName;
        }

        @Override
        public void setGeometryName(String geometryName) {
            this.geometryName = geometryName;
        }

        @Override
        public SharedStringTable getSharedStringTable() {
            return sharedStringTable;
        }

        @Override
        public void setSharedStringTable(SharedStringTable sharedStringTable) {
            this.sharedStringTable = sharedStringTable;
        }
    }
}
