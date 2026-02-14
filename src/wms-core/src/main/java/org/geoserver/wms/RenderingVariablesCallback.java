/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.geoserver.wms.RenderingVariables.setQueryHintsFromEnv;

import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;

/**
 * Callback that sets the WMS specific values from env variables to query hints. To be used further by SQL view layers.
 */
public class RenderingVariablesCallback implements GetMapCallback {

    @Override
    public GetMapRequest initRequest(GetMapRequest request) {
        return request;
    }

    @Override
    public void initMapContent(WMSMapContent mapContent) {}

    @Override
    public Layer beforeLayer(WMSMapContent mapContent, Layer layer) {
        return layer;
    }

    @Override
    public WMSMapContent beforeRender(WMSMapContent mapContent) {
        for (Layer layer : mapContent.layers()) {
            if (layer instanceof FeatureLayer featureLayer) {
                setQueryHintsFromEnv(featureLayer);
            }
        }
        return mapContent;
    }

    @Override
    public WebMap finished(WebMap map) {
        return map;
    }

    @Override
    public void failed(Throwable t) {}
}
