package org.geoserver.gsr.model.map;

import java.io.IOException;
import org.geoserver.catalog.LayerInfo;

/** Layer model for maps, used in the {@link LayersAndTables} listing. */
public class LayerOrTable extends AbstractLayerOrTable {
    public LayerOrTable(LayerInfo layer, int id) throws IOException {
        super(layer, id);
    }
}
