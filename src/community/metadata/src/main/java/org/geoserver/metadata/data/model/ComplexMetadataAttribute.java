/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model;

import java.io.Serializable;

public interface ComplexMetadataAttribute<T extends Serializable> extends Serializable {

    public T getValue();

    public void setValue(T value);

    public Integer getIndex();

    public default void init() {
        if (getValue() == null) {
            // make sure that the null value is explicit
            // so that all sizes for nested attributes match up
            setValue(null);
        }
    }
}
