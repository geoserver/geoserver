/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractAPIExceptionHandler;
import org.springframework.stereotype.Component;

/**
 * Handles exception encoding for OGC API - Features (not the same as OGC API - Commons), which is
 * adopted as a base for STAC
 */
@Component
public class STACExceptionHandler extends AbstractAPIExceptionHandler {

    public STACExceptionHandler(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    public boolean canHandle(Throwable t, APIRequestInfo request) {
        return Optional.ofNullable(request.getService())
                .map(s -> s.getId().equals("STAC"))
                .orElse(request.getRequestPath().startsWith("/ogc/stac/"));
    }

    @Override
    protected void writeResponse(
            HttpServletResponse response, Throwable t, String type, String title) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("code", type);
        error.put("description", title);
        try (ServletOutputStream os = response.getOutputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(os, error);
            os.flush();
        } catch (Exception ex) {
            LOGGER.log(
                    Level.INFO,
                    "Problem writing exception information back to calling client:",
                    ex);
        }
    }
}
