/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.geotools.data.Parameter;

/**
 * Simple helper to build {@link Parameter} objects
 *
 * @author Andrea Aime - GeoSolutions
 */
class ParameterBuilder {

    String key;

    Class type;

    boolean required;

    String prefix;

    Integer min;

    Integer max;

    String name;

    ParameterBuilder(String key, Class type) {
        this.key = key;
        this.type = type;
    }

    ParameterBuilder required(boolean required) {
        this.required = required;
        return this;
    }

    public ParameterBuilder prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public ParameterBuilder name(String name) {
        this.name = name;
        return this;
    }

    public Parameter build() {
        Map<String, Object> metadata = new HashMap<>(2);
        if (prefix != null) {
            metadata.put(OpenSearchParameters.PARAM_PREFIX, prefix);
        }
        if (name != null) {
            metadata.put(OpenSearchParameters.PARAM_NAME, name);
        }
        if (min != null) {
            metadata.put(OpenSearchParameters.MIN_INCLUSIVE, min);
        }
        if (max != null) {
            metadata.put(OpenSearchParameters.MAX_INCLUSIVE, max);
        }
        return new Parameter<>(
                key,
                type,
                null,
                null,
                required,
                required ? 1 : 0,
                1,
                null,
                Collections.unmodifiableMap(metadata));
    }

    public ParameterBuilder minimumInclusive(int min) {
        this.min = min;
        return this;
    }

    public ParameterBuilder maximumInclusive(int max) {
        this.max = max;
        return this;
    }
}
