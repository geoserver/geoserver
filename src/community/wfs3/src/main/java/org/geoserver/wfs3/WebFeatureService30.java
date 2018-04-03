/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import net.opengis.wfs20.GetFeatureType;
import org.geoserver.wfs.request.FeatureCollectionResponse;

public interface WebFeatureService30 {
    
    ContentsDocument contents(ContentRequest request);
    
    APIDocument api(APIRequest request);
    
    Object getFeature(GetFeatureType request);
}
