/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import org.geoserver.catalog.LayerInfo;

/**
 * Wrapper for {@link org.geoserver.catalog.LayerInfo} to be used when converting the importer JSON
 * representation to a Layer
 */
public class ImportLayer {
    LayerInfo layer;

    public ImportLayer(LayerInfo layer) {
        this.layer = layer;
    }

    public LayerInfo getLayer() {
        return layer;
    }
}
