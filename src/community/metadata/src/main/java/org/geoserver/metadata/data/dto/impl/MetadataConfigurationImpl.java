/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeConfiguration;
import org.geoserver.metadata.data.dto.GeonetworkConfiguration;
import org.geoserver.metadata.data.dto.MetadataConfiguration;

/**
 * Toplevel Object that matches yaml structure.
 *
 * <p>Contains the Gui description for the metadata and a list of geonetwork endpoints for importing
 * geonetwork metadata. The Gui is constructed from MetadataAttributeConfiguration and
 * MetadataAttributeComplexTypeConfiguration.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataConfigurationImpl implements MetadataConfiguration {

    List<AttributeConfiguration> attributes = new ArrayList<>();

    List<GeonetworkConfiguration> geonetworks = new ArrayList<>();

    List<AttributeTypeConfiguration> types = new ArrayList<>();

    List<String> csvImports = new ArrayList<>();

    @Override
    public List<AttributeConfiguration> getAttributes() {
        return attributes;
    }

    @Override
    public List<GeonetworkConfiguration> getGeonetworks() {
        return geonetworks;
    }

    @Override
    public List<AttributeTypeConfiguration> getTypes() {
        return types;
    }

    @Override
    public AttributeTypeConfiguration findType(String typename) {
        for (AttributeTypeConfiguration type : types) {
            if (typename.equals(type.getTypename())) {
                return type;
            }
        }
        return null;
    }

    @Override
    public AttributeConfiguration findAttribute(String attName) {
        for (AttributeConfiguration att : attributes) {
            if (attName.equals(att.getKey())) {
                return att;
            }
        }
        return null;
    }

    @Override
    public List<String> getCsvImports() {
        return csvImports;
    }
}
