/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.metadata.data.dto.CustomNativeMappingConfiguration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomNativeMappingConfigurationImpl implements CustomNativeMappingConfiguration {

    private String type;

    private Map<String, String> mapping = new HashMap<>();

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Map<String, String> getMapping() {
        return mapping;
    }
}
