/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import io.swagger.v3.oas.models.OpenAPI;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;

/** Returns a swagger-UI HTML wrapper for the JSON response */
public class OpenAPIHTMLResponse extends AbstractHTMLResponse {

    public OpenAPIHTMLResponse(GeoServerResourceLoader loader, GeoServer geoServer) {
        super(OpenAPI.class, loader, geoServer);
    }

    @Override
    protected String getTemplateName(Object value) {
        return "api.ftl";
    }

    @Override
    protected ResourceInfo getResource(Object value) {
        return null;
    }

    @Override
    protected String getFileName(Object value, Operation operation) {
        return "api";
    }
}
