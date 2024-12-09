/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ExtensionPriority;
import org.springframework.stereotype.Component;

/** Handles all exceptions encoding them as a JSON response as indicated by OGC API - Commons */
@Component
public class DefaultAPIExceptionHandler extends AbstractAPIExceptionHandler
        implements ExtensionPriority {

    public DefaultAPIExceptionHandler(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    protected void writeResponse(
            HttpServletResponse response, Throwable t, String type, String title) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("type", type);
        error.put("title", title);
        String trace = getStackTrace(t);
        if (trace != null) error.put("detail", trace);
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

    @Override
    protected String getDescription(Throwable t) {
        String description = super.getDescription(t);
        String trace = getStackTrace(t);
        if (trace != null) return description + "\nDetails:\n" + trace;
        else return description;
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }
}
