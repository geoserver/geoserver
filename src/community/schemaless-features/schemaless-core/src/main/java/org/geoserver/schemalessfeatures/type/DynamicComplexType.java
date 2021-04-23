/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.type;

import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * Interface that represents a DynamicComplexType, a type with no fixed descriptor definition,
 * allowing the addition of new ones.
 */
public interface DynamicComplexType extends ComplexType {

    /**
     * add a PropertyDescriptor
     *
     * @param descriptor the descriptor to add
     */
    void addPropertyDescriptor(PropertyDescriptor descriptor);

    void removePropertyDescriptor(PropertyDescriptor descriptor);
}
