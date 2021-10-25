package org.geoserver.featurestemplating.builders;

import java.util.HashMap;
import org.opengis.filter.expression.Expression;

/**
 * Class listing the supported options a user can specify from a template to customize the output.
 */
public class VendorOptions extends HashMap<String, Object> {

    // encoding hint to retrieve the json-ld context
    public static final String CONTEXT = "@context";

    // encoding hint to retrieve gml namespaces
    public static final String NAMESPACES = "NAMESPACES";

    // encoding hint to retrieve gml schema_location
    public static final String SCHEMA_LOCATION = "SCHEMA_LOCATION";

    // vendor option used to issue a GeoJSON flat output
    public static final String FLAT_OUTPUT = "flat_output";

    // vendor option used to customize the separator in GeoJSON flat output attributes
    public static final String SEPARATOR = "separator";

    // vendor option used to define Javascript for XHTML templates
    public static final String SCRIPT = "script";

    // vendor option used to define Style for XHTML templates
    public static final String STYLE = "style";

    public static final String LINK = "link";

    // encoding hint to encode all json-ld attributes as string
    public static final String JSON_LD_STRING_ENCODE = "encode_as_string";

    public <T> T get(String key, Class<T> cast) {
        Object value = get(key);
        T result = null;
        if (value instanceof Expression && !cast.isAssignableFrom(Expression.class)) {
            result = ((Expression) value).evaluate(null, cast);
        } else {
            result = cast.cast(value);
        }
        return result;
    }

    public <T> T get(String key, Class<T> cast, T defaultValue) {
        T result = get(key, cast);
        if (result == null) result = defaultValue;
        return result;
    }
}
