package org.geoserver.featurestemplating.builders;

import java.util.HashMap;
import org.geotools.api.filter.expression.Expression;

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

    // vendor option used to add a Link element for XHTML templates
    public static final String LINK = "link";

    // vendor option to encode all json-ld attributes as string
    public static final String JSON_LD_STRING_ENCODE = "encode_as_string";

    // vendor option to define the json-ld root type
    public static final String JSONLD_TYPE = "@type";

    // vendor option to define features collectionName
    public static final String COLLECTION_NAME = "collection_name";

    // vendor option used to inject a JSON-LD script element for XHTML templates
    public static final String JSON_LD_SCRIPT = "JSON_LD_SCRIPT";

    public <T> T get(String key, Class<T> cast) {
        Object value = get(key);
        T result = null;
        if (value instanceof Expression && !Expression.class.isAssignableFrom(cast)) {
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
