/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto.impl;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.metadata.data.dto.AttributeMappingConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeMappingConfiguration;

/**
 * Object that matches yaml structure.
 *
 * <p>The part describes one mapping for an object. The object mapping is made from a list of
 * mappings for each attribute.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class AttributeTypeMappingConfigurationImpl implements AttributeTypeMappingConfiguration {

    private static final long serialVersionUID = 8056316409852056776L;

    String typename;

    List<AttributeMappingConfiguration> mapping = new ArrayList<>();

    @Override
    public String getTypename() {
        return typename;
    }

    @Override
    public List<AttributeMappingConfiguration> getMapping() {
        return mapping;
    }
}
