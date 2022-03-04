/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.List;
import org.opengis.feature.type.Name;

/** Event associated with OSEO data store changes */
public class OseoEvent {
    private OseoEventType type;
    private String collectionName;
    private List<Name> attributeNames;

    public OseoEventType getType() {
        return type;
    }

    public void setType(OseoEventType type) {
        this.type = type;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public List<Name> getAttributeNames() {
        return attributeNames;
    }

    public void setAttributeNames(List<Name> attributeNames) {
        this.attributeNames = attributeNames;
    }
}
