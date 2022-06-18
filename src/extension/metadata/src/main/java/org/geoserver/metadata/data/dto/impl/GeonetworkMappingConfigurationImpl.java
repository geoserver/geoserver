/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.metadata.data.dto.AttributeMappingConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeMappingConfiguration;
import org.geoserver.metadata.data.dto.GeonetworkMappingConfiguration;
import org.geoserver.metadata.data.dto.NamespaceConfiguration;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeonetworkMappingConfigurationImpl implements GeonetworkMappingConfiguration {

    List<AttributeMappingConfiguration> geonetworkmapping = new ArrayList<>();

    List<AttributeTypeMappingConfiguration> objectmapping = new ArrayList<>();

    List<NamespaceConfiguration> namespaces = new ArrayList<>();

    @Override
    public List<AttributeMappingConfiguration> getGeonetworkmapping() {
        return geonetworkmapping;
    }

    @Override
    public List<AttributeTypeMappingConfiguration> getObjectmapping() {
        return objectmapping;
    }

    @Override
    public List<NamespaceConfiguration> getNamespaces() {
        return namespaces;
    }

    @Override
    public AttributeTypeMappingConfiguration findType(String typename) {
        for (AttributeTypeMappingConfiguration type : objectmapping) {
            if (typename.equals(type.getTypename())) {
                return type;
            }
        }
        return null;
    }
}
