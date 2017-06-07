package com.boundlessgeo.gsr.core.map;

import org.geoserver.catalog.LayerInfo;

import java.io.IOException;

/**
 * Layer model for maps, used in the {@link LayersAndTables} listing.
 */
public class LayerOrTable extends AbstractLayerOrTable {
    LayerOrTable(LayerInfo layer, int id) throws IOException {
        super(layer, id);
    }
}
