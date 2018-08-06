/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs3.BaseRequest;

/** JSON/YAML encoding for the API document */
public class OpenAPIResponse extends JacksonResponse {

    /** The JSON flavour indentifying OpenAPI documents */
    public static final String OPEN_API_MIME = "application/openapi+json;version=3.0";

    public OpenAPIResponse(GeoServer gs) {
        super(
                gs,
                OpenAPI.class,
                new LinkedHashSet<>(
                        Arrays.asList(
                                OPEN_API_MIME,
                                BaseRequest.JSON_MIME,
                                BaseRequest.YAML_MIME,
                                BaseRequest.XML_MIME)));
    }

    @Override
    protected boolean isJsonFormat(Operation operation) {
        String format = getFormat(operation);
        // accept json too, don't be a d**k, the openapi mime is hard to remember
        return OPEN_API_MIME.equalsIgnoreCase(format)
                || BaseRequest.JSON_MIME.equalsIgnoreCase(format);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if (OPEN_API_MIME.equalsIgnoreCase(getFormat(operation))) {
            return OPEN_API_MIME;
        }
        return super.getMimeType(value, operation);
    }

    protected String getFileName(Object value, Operation operation) {
        return "api";
    }
}
