package com.boundlessgeo.gsr.core.map;

import com.boundlessgeo.gsr.core.GSRModel;

import java.util.List;

public class Legends implements GSRModel {

    List<LayerLegend> legends;

    public Legends(List<LayerLegend> legends) {
        this.legends = legends;
    }
}
