/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.metadata.data.dto.CustomNativeMappingConfiguration;
import org.geoserver.metadata.data.dto.CustomNativeMappingsConfiguration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomNativeMappingsConfigurationImpl implements CustomNativeMappingsConfiguration {

    private List<CustomNativeMappingConfiguration> customNativeMappings = new ArrayList<>();

    @Override
    public List<CustomNativeMappingConfiguration> getCustomNativeMappings() {
        return customNativeMappings;
    }
}
