package org.geoserver.monitor.ows.wms;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;

public class GetMapHandler extends RequestObjectHandler {

    public GetMapHandler() {
        super("org.geoserver.wms.GetMapRequest");
    }

    @Override
    public List<String> getLayers(Object request) {
        List mapLayers = (List) OwsUtils.get(request, "layers");
        
        List<String> layers = new ArrayList();
        for (int i = 0; i < mapLayers.size(); i++) {
            layers.add((String) OwsUtils.get(mapLayers.get(i), "name"));
        }
        
        return layers;
    }

}
