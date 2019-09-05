/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.geoserver.metadata.data.dto.impl.CustomNativeMappingsConfigurationImpl;

@JsonDeserialize(as = CustomNativeMappingsConfigurationImpl.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface CustomNativeMappingsConfiguration {

    List<CustomNativeMappingConfiguration> getCustomNativeMappings();
}
