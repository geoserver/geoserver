package com.boundlessgeo.gsr.model.feature;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureEdits {
    private FeatureArray adds;
    private FeatureArray updates;
    private List<Integer> deletes;


    public FeatureArray getAdds() {
        return adds;
    }

    public void setAdds(FeatureArray adds) {
        this.adds = adds;
    }

    public FeatureArray getUpdates() {
        return updates;
    }

    public void setUpdates(FeatureArray updates) {
        this.updates = updates;
    }

    public List<Integer> getDeletes() {
        return deletes;
    }

    public void setDeletes(List<Integer> deletes) {
        this.deletes = deletes;
    }
}
