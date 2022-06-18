/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto.impl;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeConfiguration;

/**
 * Object that matches yaml structure.
 *
 * <p>The part describes a complex object. The complex object contains a list of mappings that make
 * the object.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class AttributeTypeConfigurationImpl implements AttributeTypeConfiguration {

    private static final long serialVersionUID = 7617959011871570119L;

    String typename;

    List<AttributeConfiguration> attributes = new ArrayList<>();

    List<String> csvImports = new ArrayList<>();

    @Override
    public List<AttributeConfiguration> getAttributes() {
        return attributes;
    }

    @Override
    public String getTypename() {
        return typename;
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
