/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model;

import java.io.Serializable;

public interface ComplexMetadataMap extends Serializable {

    <T extends Serializable> ComplexMetadataAttribute<T> get(
            Class<T> clazz, String att, int... index);

    ComplexMetadataMap subMap(String name, int... index);

    void delete(String att, int... index);

    int size(String att, int... index);

    ComplexMetadataMap clone();

    int getIndex();
}
