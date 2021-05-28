package org.geoserver.featurestemplating.builders;

import java.util.HashMap;
import org.opengis.filter.expression.Expression;

public class VendorOptions extends HashMap<String, Object> {

    public static final String FLAT_OUTPUT = "flat_output";
    public static final String SEPARATOR = "separator";

    public boolean hasOption(String key) {
        return get(key) != null;
    }

    public <T> T get(String key, Class<T> cast) {
        Object value = get(key);
        T result = null;
        if (value instanceof Expression) {
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
