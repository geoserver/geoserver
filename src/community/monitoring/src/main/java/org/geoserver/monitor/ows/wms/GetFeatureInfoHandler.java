/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wms;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;

public class GetFeatureInfoHandler extends RequestObjectHandler {

    public GetFeatureInfoHandler() {
        super("org.geoserver.wms.GetFeatureInfoRequest");
    }

    @Override
    public List<String> getLayers(Object request) {
        List queryLayers = (List) OwsUtils.get(request, "queryLayers");
        if (queryLayers == null) {
            return null;
        }
        
        List<String> layers = new ArrayList();
        for (int i = 0; i < queryLayers.size(); i++) {
            layers.add((String) OwsUtils.get(queryLayers.get(i), "name"));
        }
        
        return layers;
    }

}
