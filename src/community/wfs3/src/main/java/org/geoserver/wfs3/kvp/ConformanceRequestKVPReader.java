/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import org.geoserver.wfs3.ConformanceRequest;
import org.geoserver.wfs3.LandingPageRequest;

/**
 * Parses a "landingPage" request
 */
public class ConformanceRequestKVPReader extends BaseKvpRequestReader {

    public ConformanceRequestKVPReader() {
        super(ConformanceRequest.class);
    }
    
    
}
