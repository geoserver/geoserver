/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import net.opengis.wfs20.GetFeatureType;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WebFeatureService20;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.opengis.filter.FilterFactory2;

/**
 * WFS 3.0 implementation
 */
public class DefaultWebFeatureService30 implements WebFeatureService30 {

    private FilterFactory2 filterFactory;
    private final GeoServer geoServer;
    private WebFeatureService20 wfs20;

    public DefaultWebFeatureService30(GeoServer geoServer, WebFeatureService20 wfs20) {
        this.geoServer = geoServer;
        this.wfs20 = wfs20;
    }

    public FilterFactory2 getFilterFactory() {
        return filterFactory;
    }

    public void setFilterFactory(FilterFactory2 filterFactory) {
        this.filterFactory = filterFactory;
    }

    @Override
    public APIDocument api(APIRequest request) {
        return new APIDocument(geoServer.getService(WFSInfo.class), geoServer.getCatalog());
    }

    @Override
    public FeatureCollectionResponse getFeature(GetFeatureType request) {
        
        
        
        return wfs20.getFeature(request);
    }
}
