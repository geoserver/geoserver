/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

import org.geoserver.metadata.data.dto.impl.NamespaceConfigurationImpl;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = NamespaceConfigurationImpl.class)
public interface NamespaceConfiguration {

    String getPrefix();

    String getURI();
}
