/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.geoserver.config.GeoServerInfo;
import org.geotools.data.Parameter;

/**
 * Summary of a OpenSearch EO description, with indication of URL template parameters and the like
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEODescription {

    String baseURL;

    String parentId;

    OSEOInfo serviceInfo;

    GeoServerInfo geoserverInfo;

    List<Parameter<?>> searchParameters;

    public OSEODescription(
            OSEODescriptionRequest request,
            OSEOInfo serviceInfo,
            GeoServerInfo geoserverInfo,
            List<Parameter<?>> searchParameters) {
        this.baseURL = request.getBaseUrl();
        this.parentId = request.getParentId();
        this.serviceInfo = serviceInfo;
        this.geoserverInfo = geoserverInfo;
        this.searchParameters = searchParameters;
    }

    public OSEOInfo getServiceInfo() {
        return serviceInfo;
    }

    public String getParentId() {
        return parentId;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public GeoServerInfo getGeoserverInfo() {
        return geoserverInfo;
    }

    public List<Parameter<?>> getSearchParameters() {
        return searchParameters;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE, false);
    }
}
