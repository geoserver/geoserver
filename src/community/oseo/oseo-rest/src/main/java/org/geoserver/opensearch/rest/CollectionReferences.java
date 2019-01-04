/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.util.List;

class CollectionReferences {

    List<CollectionReference> collections;

    public CollectionReferences(List<CollectionReference> collections) {
        super();
        this.collections = collections;
    }

    public List<CollectionReference> getCollections() {
        return collections;
    }
}
