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
                        if (o1.getId() == o2.getId()) return 0;
                        return o1.getId() < o2.getId() ? -1 : 1;
                    }
                });
    }
}
