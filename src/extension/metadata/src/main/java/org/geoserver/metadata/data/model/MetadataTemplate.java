/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public interface MetadataTemplate extends Serializable {

    String getId();

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    Map<String, Serializable> getMetadata();

    Set<String> getLinkedLayers();

    MetadataTemplate clone();
}
