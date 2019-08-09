package com.boundlessgeo.gsr.model.feature;

import static com.boundlessgeo.gsr.GSRConfig.CURRENT_VERSION;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wfs.WFSInfo;
import com.boundlessgeo.gsr.model.AbstractGSRModel;
import com.boundlessgeo.gsr.model.GSRModel;
import com.boundlessgeo.gsr.model.map.LayerEntry;

/**
 * Detailed model of a FeatureService
 */
public class FeatureServiceRoot extends AbstractGSRModel implements GSRModel {

    public final Double currentVersion = CURRENT_VERSION;
    public final String serviceDescription;
    public final String supportedQueryFormats = "JSON";
    public final String initialExtent = null;
    public final String fullExtent = null;
    
    private String workspace;

    public final List<LayerEntry> layers = new ArrayList<>();
    public final List<LayerEntry> tables = new ArrayList<>();

    public FeatureServiceRoot(WFSInfo service, String workspace, List<LayerInfo> layers) {
        this.workspace = workspace;
        serviceDescription = service.getTitle() != null ? service.getTitle() : service.getName();
        for (int i = 0; i < layers.size(); i++) {
            LayerInfo l = layers.get(i);
            this.layers.add(new LayerEntry(i, l.getName()));
        }
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public List<LayerEntry> getLayers() {
        return layers;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }
    
    
}
