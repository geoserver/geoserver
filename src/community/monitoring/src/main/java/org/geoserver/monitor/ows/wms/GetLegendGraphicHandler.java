package org.geoserver.monitor.ows.wms;

import java.util.Arrays;
import java.util.List;

import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;

import org.opengis.feature.type.FeatureType;

public class GetLegendGraphicHandler extends RequestObjectHandler {

    public GetLegendGraphicHandler() {
        super("org.geoserver.wms.GetLegendGraphicRequest");
    }

    @Override
    protected List<String> getLayers(Object request) {
        FeatureType type = (FeatureType) OwsUtils.get(request, "layer");
        return Arrays.asList(type.getName().toString());
    }

}
