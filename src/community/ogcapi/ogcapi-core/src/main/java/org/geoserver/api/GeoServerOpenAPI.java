/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Collections;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;

/** Extension to OpenAPI allowing to regsiter the link for the HTML representation */
public class GeoServerOpenAPI extends OpenAPI {

    private String serviceBase;

    @JsonIgnore
    public String getServiceBase() {
        return serviceBase;
    }

    public void setServiceBase(String serviceBase) {
        this.serviceBase = serviceBase;
    }

    /** Used by the HTML representation to locate the */
    @JsonIgnore
    public String getApiLocation() {
        String baseURL = APIRequestInfo.get().getBaseURL();
        return ResponseUtils.buildURL(
                baseURL,
                serviceBase + (serviceBase.endsWith("/") ? "api" : "/api"),
                Collections.singletonMap("f", OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE),
                URLMangler.URLType.SERVICE);
    }
}
