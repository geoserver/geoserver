/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.type;

import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * Interface that represents a SchemalessComplexType. By Schemaless is meant a type with no fixed
 * descriptor definition.
 */
public interface SchemalessComplexType extends ComplexType {

    /**
     * add a PropertyDescriptor
     *
     * @param descriptor the descriptor to add
     */
    void addPropertyDescriptor(PropertyDescriptor descriptor);
}
