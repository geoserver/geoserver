/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.map;

import java.util.List;
import org.geoserver.gsr.model.GSRModel;

/** List of {@link Legends} */
public class Legends implements GSRModel {

    private List<LayerLegend> legends;

    public Legends(List<LayerLegend> legends) {
        this.legends = legends;
    }

    public List<LayerLegend> getLegends() {
        return legends;
    }

    public void setLegends(List<LayerLegend> legends) {
        this.legends = legends;
    }
}
