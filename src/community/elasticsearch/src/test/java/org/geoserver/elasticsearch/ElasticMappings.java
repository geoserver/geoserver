/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.elasticsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/** ElasticSearch mappings */
@SuppressWarnings("unused")
class ElasticMappings {

    private Map<String, Mapping> mappings;

    public Map<String, Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, Mapping> mappings) {
        this.mappings = mappings;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Mapping {

        private Map<String, Object> properties;

        public Map<String, Object> getProperties() {
            return properties;
        }
    }

    public static class Untyped {

        private Mapping mappings;

        public Mapping getMappings() {
            return mappings;
        }
    }
}
