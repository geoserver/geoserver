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
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gsr.model.renderer.ClassBreakInfo;
import org.geoserver.gsr.model.renderer.ClassBreaksRenderer;
import org.geoserver.gsr.model.renderer.Renderer;
import org.geoserver.gsr.model.renderer.SimpleRenderer;
import org.geoserver.gsr.model.renderer.UniqueValueInfo;
import org.geoserver.gsr.model.renderer.UniqueValueRenderer;
import org.geoserver.gsr.translate.renderer.StyleEncoder;

/** A layer legend entry, usually contained in a {@link Legends} object */
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
        if (renderer instanceof SimpleRenderer simpleRenderer) {
            legend.add(new LegendEntry(simpleRenderer.getLabel(), simpleRenderer.getSymbol()));
        } else if (renderer instanceof ClassBreaksRenderer classBreaksRenderer) {
            for (ClassBreakInfo classBreakInfo : classBreaksRenderer.getClassBreakInfos()) {
                legend.add(new LegendEntry(classBreakInfo.getLabel(), classBreakInfo.getSymbol()));
            }
        } else if (renderer instanceof UniqueValueRenderer uniqueValueRenderer) {
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
