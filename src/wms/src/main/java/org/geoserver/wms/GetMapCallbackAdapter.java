/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geotools.map.Layer;

/**
 * Convenience base class for writing {@link GetMapCallback} that are only interested in a small
 * subset of the supported events.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GetMapCallbackAdapter implements GetMapCallback {

    @Override
    public GetMapRequest initRequest(GetMapRequest request) {
        return request;
    }

    @Override
    public void initMapContent(WMSMapContent content) {
        // nothing to do
    }

    @Override
    public Layer beforeLayer(WMSMapContent content, Layer layer) {
        return layer;
    }

    @Override
    public WMSMapContent beforeRender(WMSMapContent mapContent) {
        return mapContent;
    }

    @Override
    public WebMap finished(WebMap map) {
        return map;
    }

    @Override
    public void failed(Throwable t) {
        // nothing to do
    }
}
