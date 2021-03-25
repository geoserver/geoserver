/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses.covjson;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Map;

/** Base class for CoverageJson object, declaring the type */
public class CoverageJson {

    public static final String EN_KEY = "en";

    @JsonProperty(required = true)
    private String type;

    public static Map<String, String> asI18nMap(String value) {
        return Collections.singletonMap(EN_KEY, value);
    }

    public String getType() {
        return type;
    }

    protected CoverageJson(String type) {
        this.type = type;
    }
}
