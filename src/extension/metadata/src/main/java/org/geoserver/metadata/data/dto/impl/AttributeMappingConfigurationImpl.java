/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto.impl;

import org.geoserver.metadata.data.dto.AttributeMappingConfiguration;
import org.geoserver.metadata.data.dto.MappingTypeEnum;

/**
 * Object that matches yaml structure.
 *
 * <p>The part describes one mapping between the geoserver fields en the xml metadata from
 * geonetwork. The geonetwork field is described as an xpath expression.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class AttributeMappingConfigurationImpl implements AttributeMappingConfiguration {

    private static final long serialVersionUID = -2528238667226248014L;

    String geoserver;

    String geonetwork;

    MappingTypeEnum mappingType = MappingTypeEnum.CUSTOM;

    public AttributeMappingConfigurationImpl() {}

    public AttributeMappingConfigurationImpl(AttributeMappingConfiguration other) {
        if (other != null) {
            geoserver = other.getGeoserver();
            geonetwork = other.getGeonetwork();
        }
    }

    @Override
    public String getGeoserver() {
        return geoserver;
    }

    @Override
    public String getGeonetwork() {
        return geonetwork;
    }

    @Override
    public MappingTypeEnum getMappingType() {
        return mappingType;
    }
}
