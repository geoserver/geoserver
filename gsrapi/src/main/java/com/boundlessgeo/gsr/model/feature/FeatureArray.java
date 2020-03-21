package com.boundlessgeo.gsr.model.feature;

import com.boundlessgeo.gsr.model.GSRModel;

import java.util.List;

/**
 * Model object for the list of features for an Add/Update/Delete Feature request
 */
public class FeatureArray implements GSRModel {
    public final List<Feature> features;

    public FeatureArray(List<Feature> features) {
        this.features = features;
    }
}
