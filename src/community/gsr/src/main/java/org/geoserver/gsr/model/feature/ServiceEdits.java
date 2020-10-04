/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.feature;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.geoserver.gsr.model.GSRModel;

public class ServiceEdits implements GSRModel {
    public final List<LayerEdits> layerEdits;

    public ServiceEdits(List<LayerEdits> layerEdits) {
        this.layerEdits = layerEdits;
    }

    public void sortByID() {
        Collections.sort(
                layerEdits,
                new Comparator<LayerEdits>() {
                    @Override
                    public int compare(LayerEdits o1, LayerEdits o2) {
                        if (o1.getId().intValue() == o2.getId().intValue()) return 0;
                        return o1.getId().intValue() < o2.getId().intValue() ? -1 : 1;
                    }
                });
    }
}
