/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.map;

import com.boundlessgeo.gsr.core.renderer.*;
import org.geoserver.catalog.LayerInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A layer legend entry, usually contained in a {@link Legends} object
 */
public class LayerLegend {

    Integer layerId;
    String layerName;
    String layerType = "Feature Layer";
    Integer minScale = 0;
    Integer maxScale = 0;
    List<LegendEntry> legend = new ArrayList<>();


    public LayerLegend(LayerInfo layer, int id) throws IOException {
        this.layerId = id;
        this.layerName = layer.getName();

        Renderer renderer = StyleEncoder.effectiveRenderer(layer);
        if (renderer instanceof SimpleRenderer) {
            SimpleRenderer simpleRenderer = (SimpleRenderer) renderer;
            legend.add(new LegendEntry(simpleRenderer.getLabel(), simpleRenderer.getSymbol()));
        } else if (renderer instanceof ClassBreaksRenderer) {
            ClassBreaksRenderer classBreaksRenderer = (ClassBreaksRenderer) renderer;
            for (ClassBreakInfo classBreakInfo : classBreaksRenderer.getClassBreakInfos()) {
                legend.add(new LegendEntry(classBreakInfo.getLabel(), classBreakInfo.getSymbol()));
            }
        } else if (renderer instanceof UniqueValueRenderer) {
            UniqueValueRenderer uniqueValueRenderer = (UniqueValueRenderer) renderer;
            for (UniqueValueInfo uniqueValueInfo : uniqueValueRenderer.getUniqueValueInfos()) {
                legend.add(new LegendEntry(uniqueValueInfo.getLabel(), uniqueValueInfo.getSymbol()));
            }
        }
    }
}
