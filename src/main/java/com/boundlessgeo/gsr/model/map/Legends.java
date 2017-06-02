package com.boundlessgeo.gsr.model.map;

import com.boundlessgeo.gsr.model.GSRModel;

import java.util.List;

public class Legends implements GSRModel {

    List<LayerLegend> legends;

    public Legends(List<LayerLegend> legends) {
        this.legends = legends;
    }
}
