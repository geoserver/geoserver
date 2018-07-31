package com.boundlessgeo.gsr.model.feature;

import com.boundlessgeo.gsr.model.GSRModel;
import com.boundlessgeo.gsr.model.map.LayerEntry;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wfs.WFSInfo;

import java.util.ArrayList;
import java.util.List;

import static com.boundlessgeo.gsr.GSRConfig.CURRENT_VERSION;

/**
 * Detailed model of a FeatureService
 */
public class FeatureServiceRoot implements GSRModel {

    public final Double currentVersion = CURRENT_VERSION;
    public final String serviceDescription;
    public final String supportedQueryFormats = "JSON";
    public final String initialExtent = null;
    public final String fullExtent = null;

    public final List<LayerEntry> layers = new ArrayList<>();
    public final List<LayerEntry> tables = new ArrayList<>();

    public FeatureServiceRoot(WFSInfo service, List<LayerInfo> layers) {
        serviceDescription = service.getTitle() != null ? service.getTitle() : service.getName();
        for (int i = 0; i < layers.size(); i++) {
            LayerInfo l = layers.get(i);
            this.layers.add(new LayerEntry(i, l.getName()));
        }
    }
}
