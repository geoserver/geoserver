/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.wms.GetFeatureInfoRequest;

/**
 * Response class that allows us to carry around the request, needed for the {@link
 * org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat}
 */
public class FeatureInfoResponse {

    FeatureCollectionType result;
    GetFeatureInfoRequest request;

    public FeatureInfoResponse(FeatureCollectionType result, GetFeatureInfoRequest request) {
        this.result = result;
        this.request = request;
    }

    public FeatureCollectionType getResult() {
        return result;
    }

    public GetFeatureInfoRequest getRequest() {
        return request;
    }
}
