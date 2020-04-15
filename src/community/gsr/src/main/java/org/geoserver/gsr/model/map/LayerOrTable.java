/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.map;

import java.io.IOException;
import org.geoserver.catalog.LayerInfo;

/** Layer model for maps, used in the {@link LayersAndTables} listing. */
public class LayerOrTable extends AbstractLayerOrTable {
    public LayerOrTable(LayerInfo layer, int id) throws IOException {
        super(layer, id);
    }
}
