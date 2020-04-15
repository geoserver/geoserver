package org.geoserver.gsr.api.feature;

import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.gsr.model.feature.FeatureList;
import org.geoserver.gsr.model.map.LayerOrTable;
import org.geoserver.gsr.model.map.LayersAndTables;

/** Wrapper for feature query responses */
public class FeatureServiceQueryResult {
    private final List<FeatureLayer> layers;

    public FeatureServiceQueryResult(LayersAndTables layersAndTables) {
        this.layers =
                layersAndTables.layers.stream().map(FeatureLayer::new).collect(Collectors.toList());
    }

    public List<FeatureLayer> getLayers() {
        return layers;
    }

    public static class FeatureLayer {
        private final Integer id;

        private FeatureList features;

        public FeatureLayer(LayerOrTable layer) {
            this.id = layer.getId();
        }

        public Integer getId() {
            return id;
        }

        public void setFeatures(FeatureList features) {
            this.features = features;
        }

        public FeatureList getFeatures() {
            return features;
        }
    }
}
