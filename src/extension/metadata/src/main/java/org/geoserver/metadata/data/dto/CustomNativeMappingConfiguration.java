/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import org.geoserver.metadata.data.dto.impl.CustomNativeMappingConfigurationImpl;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = CustomNativeMappingConfigurationImpl.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface CustomNativeMappingConfiguration {

    String getType();

    Map<String, String> getMapping();
}
