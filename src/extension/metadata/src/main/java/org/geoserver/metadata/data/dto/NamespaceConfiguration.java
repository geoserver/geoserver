/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.geoserver.metadata.data.dto.impl.NamespaceConfigurationImpl;

@JsonDeserialize(as = NamespaceConfigurationImpl.class)
public interface NamespaceConfiguration {

    String getPrefix();

    String getURI();
}
