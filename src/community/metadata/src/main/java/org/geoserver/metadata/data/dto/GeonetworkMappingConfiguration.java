/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.geoserver.metadata.data.dto.impl.GeonetworkMappingConfigurationImpl;

/**
 * Toplevel Object that matches yaml structure.
 *
 * <p>This part or the yaml contains the configuration that matches fields in the xml (Xpath
 * expressions) to the field configuration of the geoserver metadata GUI.
 *
 * <p>example of the yaml file: metadata-mapping.yaml
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
@JsonDeserialize(as = GeonetworkMappingConfigurationImpl.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface GeonetworkMappingConfiguration {

    public List<AttributeMappingConfiguration> getGeonetworkmapping();

    public List<AttributeTypeMappingConfiguration> getObjectmapping();

    public List<NamespaceConfiguration> getNamespaces();

    public AttributeTypeMappingConfiguration findType(String typename);
}
