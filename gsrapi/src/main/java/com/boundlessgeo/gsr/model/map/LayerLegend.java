/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.LayerInfo;

import com.boundlessgeo.gsr.model.renderer.ClassBreakInfo;
import com.boundlessgeo.gsr.model.renderer.ClassBreaksRenderer;
import com.boundlessgeo.gsr.model.renderer.Renderer;
import com.boundlessgeo.gsr.model.renderer.SimpleRenderer;
import com.boundlessgeo.gsr.translate.renderer.StyleEncoder;
import com.boundlessgeo.gsr.model.renderer.UniqueValueInfo;
import com.boundlessgeo.gsr.model.renderer.UniqueValueRenderer;

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

    public Integer getLayerId() {
        return layerId;
    }

    public void setLayerId(Integer layerId) {
        this.layerId = layerId;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getLayerType() {
        return layerType;
    }

    public void setLayerType(String layerType) {
        this.layerType = layerType;
    }

    public Integer getMinScale() {
        return minScale;
    }

    public void setMinScale(Integer minScale) {
        this.minScale = minScale;
    }

    public Integer getMaxScale() {
        return maxScale;
    }

    public void setMaxScale(Integer maxScale) {
        this.maxScale = maxScale;
    }

    public List<LegendEntry> getLegend() {
        return legend;
    }

    public void setLegend(List<LegendEntry> legend) {
        this.legend = legend;
    }
}
