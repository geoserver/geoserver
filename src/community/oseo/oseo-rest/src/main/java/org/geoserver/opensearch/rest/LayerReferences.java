/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.util.Collections;
import java.util.List;

class LayerReferences {

    public static final LayerReferences EMPTY = new LayerReferences(Collections.emptyList());

    List<LayerReference> layers;

    public LayerReferences(List<LayerReference> collections) {
        super();
        this.layers = collections;
    }

    public List<LayerReference> getLayers() {
        return layers;
    }
}
