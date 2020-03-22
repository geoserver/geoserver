package org.geoserver.gsr.model.feature;

import java.util.List;
import org.geoserver.gsr.model.GSRModel;

/** Model object for the list of features for an Add/Update/Delete Feature request */
public class FeatureArray implements GSRModel {
    public final List<Feature> features;

    public FeatureArray(List<Feature> features) {
        this.features = features;
    }
}
