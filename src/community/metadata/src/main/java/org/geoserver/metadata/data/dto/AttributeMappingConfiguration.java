/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.geoserver.metadata.data.dto.impl.AttributeMappingConfigurationImpl;

/**
 * Object that matches yaml structure.
 *
 * <p>The part describes one mapping between the geoserver fields en the xml metadata from
 * geonetwork. The geonetwork field is described as an xpath expression.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
@JsonDeserialize(as = AttributeMappingConfigurationImpl.class)
public interface AttributeMappingConfiguration extends Serializable {

    public String getGeoserver();

    public String getGeonetwork();

    public MappingTypeEnum getMappingType();
}
