/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

/**
 * Summary of a OpenSearch EO description, with indication of URL template parameters and the like
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class OSEODescription {

    String baseURL;

    String parentId;

    public OSEODescription(OSEODescriptionRequest request) {
        this.baseURL = request.getBaseUrl();
        this.parentId = request.getParentId();
    }

    public String getParentId() {
        return parentId;
    }

    public String getBaseURL() {
        return baseURL;
    }

}
