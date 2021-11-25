/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders;

import java.util.HashMap;
import java.util.Optional;
import org.geotools.util.Converters;
import org.opengis.filter.expression.Expression;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * This class represent a Map of encoding hints. An encoding hint is a value giving additional
 * instructions to the writing process of a template output. The class also defines the constants
 * for the currently available EncodingHints.
 */
public class EncodingHints extends HashMap<String, Object> {

    // encoding hint to retrieve the json-ld context
    public static final String CONTEXT = "@context";

    // encoding hint to retrieve gml namespaces
    public static final String NAMESPACES = "NAMESPACES";

    // encoding hint to retrieve gml schema_location
    public static final String SCHEMA_LOCATION = "SCHEMA_LOCATION";

    // encoding hint to tell when a value builder has to be encoded as an xml attribute
    public static final String ENCODE_AS_ATTRIBUTE = "ENCODE_AS_ATTRIBUTE";

    // encoding hint to tell if the Iterating builder should iterate also
    // the key value (eg. for xml output)
    public static final String ITERATE_KEY = "INTERATE_KEY";

    // encoding hint to issue children evaluation in value builder (Dynamic and Static)
    // this is needed for xml output where a value builder can have children for xml attributes
    public static final String CHILDREN_EVALUATION = "CHILDREN_EVALUATION";

    // encoding hint to tell the writer to skip start and end object
    // when we are dealing with a single feature request
    public static final String SKIP_OBJECT_ENCODING = "SKIP_OBJECT_ENCODING";

    public EncodingHints() {
        super();
    }

    public EncodingHints(EncodingHints encodingHints) {
        super(encodingHints);
    }

    /**
     * Check if the hint is present.
     *
     * @param hint the name of the hint.
     * @return true if present false otherwise.
     */
    public boolean hasHint(String hint) {
        return get(hint) != null;
    }

    /**
     * Get the hint value with the requested type.
     *
     * @param key the hint name.
     * @param cast the type requested.
     * @return the value of the hint if found, otherwise null.
     */
    public <T> T get(String key, Class<T> cast) {
        return cast.cast(get(key));
    }

    /**
     * Get the hint value with the requested type.
     *
     * @param key the hint name.
     * @param cast the type requested.
     * @return the value of the hint if found, otherwise null.
     */
    public <T> T get(String key, Class<T> cast, T defaultValue) {
        Object result = get(key);
        if (result instanceof Expression && !Expression.class.isAssignableFrom(cast)) {
            result = ((Expression) result).evaluate(null);
        }
        T value = Converters.convert(result, cast);
        if (value == null) value = defaultValue;
        return value;
    }

    /**
     * Check if the current request is an OGCAPI request by feature id.
     *
     * @return true if is a single feature request, false otherwise.
     */
    public static boolean isSingleFeatureRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(
                        att ->
                                (String)
                                        att.getAttribute(
                                                "OGCFeatures:ItemId",
                                                RequestAttributes.SCOPE_REQUEST))
                .isPresent();
    }
}
