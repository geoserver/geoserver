/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model;

import java.io.Serializable;

public interface ComplexMetadataAttribute<T extends Serializable> extends Serializable {

    T getValue();

    void setValue(T value);

    Integer getIndex();

    default void init() {
        // making sure that all sizes for nested attributes match up
        // (setting explicit null)
        // and fix the structure if the config has changed.
        setValue(getValue());
    }
}
